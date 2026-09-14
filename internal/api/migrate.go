package api

import (
	"context"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
)

type migrationFile struct {
	version    int
	name, path string
}

func (a *App) migrate(ctx context.Context) error {
	dir := a.cfg.MigrationDir
	entries, err := os.ReadDir(dir)
	if os.IsNotExist(err) && dir == "db/migration" {
		dir = "src/main/resources/db/migration"
		entries, err = os.ReadDir(dir)
	}
	if err != nil {
		return fmt.Errorf("read migrations: %w", err)
	}
	files := make([]migrationFile, 0, len(entries))
	for _, e := range entries {
		name := e.Name()
		if e.IsDir() || !strings.HasPrefix(name, "V") || !strings.HasSuffix(name, ".sql") {
			continue
		}
		cut := strings.Index(name, "__")
		if cut < 2 {
			continue
		}
		v, err := strconv.Atoi(name[1:cut])
		if err != nil {
			continue
		}
		files = append(files, migrationFile{v, name, filepath.Join(dir, name)})
	}
	sort.Slice(files, func(i, j int) bool { return files[i].version < files[j].version })
	if len(files) == 0 {
		return fmt.Errorf("no SQL migrations found in %s", dir)
	}

	if _, err := a.db.Exec(ctx, `CREATE SCHEMA IF NOT EXISTS sisges; CREATE TABLE IF NOT EXISTS sisges.go_schema_migrations (version INTEGER PRIMARY KEY, name TEXT NOT NULL, applied_at TIMESTAMPTZ NOT NULL DEFAULT now())`); err != nil {
		return err
	}
	var hasFlyway bool
	if err := a.db.QueryRow(ctx, `SELECT to_regclass('sisges.flyway_schema_history') IS NOT NULL`).Scan(&hasFlyway); err != nil {
		return err
	}
	for _, m := range files {
		var applied bool
		err := a.db.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM sisges.go_schema_migrations WHERE version=$1)`, m.version).Scan(&applied)
		if err != nil {
			return fmt.Errorf("check migration %d: %w", m.version, err)
		}
		if !applied && hasFlyway {
			err = a.db.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM sisges.flyway_schema_history WHERE success AND version=$1::text)`, m.version).Scan(&applied)
			if err != nil {
				return fmt.Errorf("check Flyway migration %d: %w", m.version, err)
			}
		}
		if applied {
			continue
		}
		sqlBytes, err := os.ReadFile(m.path)
		if err != nil {
			return err
		}
		tx, err := a.db.Begin(ctx)
		if err != nil {
			return err
		}
		if _, err = tx.Exec(ctx, string(sqlBytes)); err == nil {
			_, err = tx.Exec(ctx, `INSERT INTO sisges.go_schema_migrations(version,name) VALUES($1,$2)`, m.version, m.name)
		}
		if err != nil {
			_ = tx.Rollback(ctx)
			return fmt.Errorf("apply migration %s: %w", m.name, err)
		}
		if err := tx.Commit(ctx); err != nil {
			return err
		}
	}
	return nil
}
