package api

import (
	"errors"
	"fmt"
	"net/url"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	ListenAddr       string
	DatabaseURL      string
	JWTSecret        []byte
	JWTExpiry        time.Duration
	AllowedOrigins   []string
	DBMaxConnections int32
	MemoryLimitBytes int64
	GCPercent        int
	MigrationDir     string
	SeedEnabled      bool
	MinioEndpoint    string
	MinioAccessKey   string
	MinioSecretKey   string
	MinioBucket      string
	MinioRegion      string
}

func LoadConfig() (Config, error) {
	port := env("PORT", "8080")
	c := Config{
		ListenAddr:       ":" + port,
		DatabaseURL:      databaseURL(),
		JWTSecret:        []byte(os.Getenv("SECURITY_JWT_SECRET_KEY")),
		JWTExpiry:        time.Duration(envInt64("SECURITY_JWT_EXPIRATION_TIME", 3_600_000)) * time.Millisecond,
		AllowedOrigins:   splitCSV(env("SISGES_CORS_ALLOWED_ORIGIN_PATTERNS", "http://localhost:*,http://127.0.0.1:*,http://[::1]:*,https://*.vercel.app")),
		DBMaxConnections: int32(envInt64("SISGES_DB_MAX_CONNECTIONS", 6)),
		MemoryLimitBytes: envInt64("GOMEMLIMIT_BYTES", 160<<20),
		GCPercent:        int(envInt64("GOGC_PERCENT", 75)),
		MigrationDir:     env("SISGES_MIGRATION_DIR", "db/migration"),
		SeedEnabled:      strings.EqualFold(env("SISGES_SEED_ENABLED", "false"), "true"),
		MinioEndpoint:    strings.TrimRight(os.Getenv("SISGES_MINIO_ENDPOINT"), "/"),
		MinioAccessKey:   os.Getenv("SISGES_MINIO_ACCESS_KEY"),
		MinioSecretKey:   os.Getenv("SISGES_MINIO_SECRET_KEY"),
		MinioBucket:      os.Getenv("SISGES_MINIO_BUCKET"),
		MinioRegion:      env("SISGES_MINIO_REGION", "us-east-1"),
	}
	if len(c.JWTSecret) < 32 {
		return Config{}, errors.New("SECURITY_JWT_SECRET_KEY must have at least 32 bytes")
	}
	if c.DatabaseURL == "" {
		return Config{}, errors.New("DATABASE_URL or SPRING_DATASOURCE_URL is required")
	}
	if c.DBMaxConnections < 1 || c.DBMaxConnections > 32 {
		return Config{}, fmt.Errorf("SISGES_DB_MAX_CONNECTIONS must be between 1 and 32")
	}
	return c, nil
}

func databaseURL() string {
	if v := os.Getenv("DATABASE_URL"); v != "" {
		return v
	}
	jdbc := strings.TrimPrefix(os.Getenv("SPRING_DATASOURCE_URL"), "jdbc:")
	if jdbc == "" {
		return ""
	}
	u := os.Getenv("SPRING_DATASOURCE_USERNAME")
	p := os.Getenv("SPRING_DATASOURCE_PASSWORD")
	if u != "" && strings.HasPrefix(jdbc, "postgresql://") && !strings.Contains(strings.TrimPrefix(jdbc, "postgresql://"), "@") {
		parsed, err := url.Parse(jdbc)
		if err == nil {
			parsed.User = url.UserPassword(u, p)
			jdbc = parsed.String()
		}
	}
	return jdbc
}
func env(k, fallback string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return fallback
}
func envInt64(k string, fallback int64) int64 {
	v, err := strconv.ParseInt(os.Getenv(k), 10, 64)
	if err != nil {
		return fallback
	}
	return v
}
func splitCSV(v string) []string {
	out := strings.Split(v, ",")
	for i := range out {
		out[i] = strings.TrimSpace(out[i])
	}
	return out
}
