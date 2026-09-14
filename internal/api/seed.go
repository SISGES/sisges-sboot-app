package api

import (
	"context"
	"fmt"
	"time"

	"golang.org/x/crypto/bcrypt"
)

// seed deliberately stays small. Large demonstration datasets waste memory and
// startup time; callers can load realistic fixtures separately.
func (a *App) seed(ctx context.Context) error {
	var exists bool
	if err := a.db.QueryRow(ctx, `SELECT EXISTS(SELECT 1 FROM sisges.users WHERE register='ADM0001' AND deleted_at IS NULL)`).Scan(&exists); err != nil || exists {
		return err
	}
	tx, err := a.db.Begin(ctx)
	if err != nil {
		return err
	}
	defer tx.Rollback(ctx)
	hash, err := bcrypt.GenerateFromPassword([]byte("admin123"), bcrypt.DefaultCost)
	if err != nil {
		return err
	}
	var adminID int
	if err := tx.QueryRow(ctx, `INSERT INTO sisges.users(name,email,register,password,birth_date,gender,user_role) VALUES($1,$2,$3,$4,$5,$6,'ADMIN') RETURNING id`, "Administrador SISGES", "adm0001@sisges.com", "ADM0001", string(hash), time.Date(1980, 1, 1, 0, 0, 0, 0, time.UTC), "OTHER").Scan(&adminID); err != nil {
		return err
	}
	classes := []struct{ name, year string }{{"1º Ano A", "1º ano - Fundamental"}, {"6º Ano A", "6º ano"}}
	for _, c := range classes {
		if _, err := tx.Exec(ctx, `INSERT INTO sisges.school_class(name,academic_year) VALUES($1,$2) ON CONFLICT(name,academic_year) DO NOTHING`, c.name, c.year); err != nil {
			return err
		}
	}
	for _, name := range []string{"Português", "Matemática", "Ciências", "História", "Geografia"} {
		if _, err := tx.Exec(ctx, `INSERT INTO sisges.discipline(name,description) VALUES($1,'Componente curricular') ON CONFLICT(name) DO NOTHING`, name); err != nil {
			return err
		}
	}
	if _, err := tx.Exec(ctx, `INSERT INTO sisges.announcement(title,content,type,target_roles,active_from,active_until,created_by) VALUES('Bem-vindo ao SISGES','Ambiente de demonstração pronto para uso.','TEXT','ADMIN,TEACHER,STUDENT',now(),now()+interval '168 hours',$1)`, adminID); err != nil {
		return fmt.Errorf("seed announcement: %w", err)
	}
	return tx.Commit(ctx)
}
