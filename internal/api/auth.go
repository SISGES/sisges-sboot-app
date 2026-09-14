package api

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
	"time"
)

type principal struct {
	ID        int    `json:"userId"`
	Email     string `json:"sub"`
	Role      string `json:"role"`
	IssuedAt  int64  `json:"iat"`
	ExpiresAt int64  `json:"exp"`
}
type principalKey struct{}

func (a *App) signToken(p principal) (string, error) {
	header := base64.RawURLEncoding.EncodeToString([]byte(`{"alg":"HS256","typ":"JWT"}`))
	p.IssuedAt = time.Now().Unix()
	p.ExpiresAt = time.Now().Add(a.cfg.JWTExpiry).Unix()
	payload, err := json.Marshal(p)
	if err != nil {
		return "", err
	}
	unsigned := header + "." + base64.RawURLEncoding.EncodeToString(payload)
	mac := hmac.New(sha256.New, a.cfg.JWTSecret)
	_, _ = mac.Write([]byte(unsigned))
	return unsigned + "." + base64.RawURLEncoding.EncodeToString(mac.Sum(nil)), nil
}

func (a *App) parseToken(token string) (*principal, error) {
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		return nil, unauthorized()
	}
	unsigned := parts[0] + "." + parts[1]
	sig, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil {
		return nil, unauthorized()
	}
	mac := hmac.New(sha256.New, a.cfg.JWTSecret)
	_, _ = mac.Write([]byte(unsigned))
	if !hmac.Equal(sig, mac.Sum(nil)) {
		return nil, unauthorized()
	}
	payload, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return nil, unauthorized()
	}
	var p principal
	if json.Unmarshal(payload, &p) != nil || p.ID < 1 || p.Email == "" || p.ExpiresAt <= time.Now().Unix() {
		return nil, unauthorized()
	}
	return &p, nil
}

func (a *App) authenticate(r *http.Request) (*principal, error) {
	h := r.Header.Get("Authorization")
	if !strings.HasPrefix(h, "Bearer ") {
		return nil, unauthorized()
	}
	p, err := a.parseToken(strings.TrimSpace(strings.TrimPrefix(h, "Bearer ")))
	if err != nil {
		return nil, err
	}
	var role, email string
	if err := a.db.QueryRow(r.Context(), `SELECT user_role,email FROM sisges.users WHERE id=$1 AND deleted_at IS NULL`, p.ID).Scan(&role, &email); err != nil || role != p.Role || email != p.Email {
		return nil, unauthorized()
	}
	return p, nil
}

func (a *App) optionalPrincipal(r *http.Request) *principal {
	if r.Header.Get("Authorization") == "" {
		return nil
	}
	p, _ := a.authenticate(r)
	return p
}
func currentPrincipal(r *http.Request) *principal {
	p, _ := r.Context().Value(principalKey{}).(*principal)
	return p
}

func (a *App) require(role string, next http.HandlerFunc) http.HandlerFunc {
	return a.requireAny(func() []string {
		if role == "" {
			return nil
		}
		return []string{role}
	}(), next)
}
func (a *App) requireAny(roles []string, next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		p, err := a.authenticate(r)
		if err != nil {
			writeError(w, err)
			return
		}
		if len(roles) > 0 {
			ok := false
			for _, role := range roles {
				if p.Role == role {
					ok = true
					break
				}
			}
			if !ok {
				writeError(w, forbidden())
				return
			}
		}
		next(w, r.WithContext(context.WithValue(r.Context(), principalKey{}, p)))
	}
}

func pathInt(r *http.Request, name string) (int, error) {
	n, err := strconv.Atoi(r.PathValue(name))
	if err != nil || n < 1 {
		return 0, validationError(name, "ID inválido")
	}
	return n, nil
}
