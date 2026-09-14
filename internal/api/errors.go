package api

import (
	"errors"
	"log/slog"
	"net/http"
	"time"

	"github.com/jackc/pgx/v5"
)

type apiError struct {
	Status    int          `json:"status"`
	Code      string       `json:"code"`
	Message   string       `json:"message"`
	Timestamp time.Time    `json:"timestamp"`
	Errors    []fieldError `json:"errors,omitempty"`
	cause     error
}
type fieldError struct {
	Field   string `json:"field"`
	Message string `json:"message"`
}

func (e *apiError) Error() string { return e.Message }
func apiErr(status int, code, message string) *apiError {
	return &apiError{Status: status, Code: code, Message: message, Timestamp: time.Now()}
}
func validationError(field, message string) *apiError {
	e := apiErr(400, "VALIDATION_ERROR", "Erro de validação")
	e.Errors = []fieldError{{field, message}}
	return e
}
func businessError(message string) *apiError { return apiErr(400, "BUSINESS_RULE_VIOLATION", message) }
func resourceError(resource string) *apiError {
	return apiErr(404, "RESOURCE_NOT_FOUND", resource+" não encontrado")
}
func forbidden() *apiError {
	return apiErr(403, "AUTH_FORBIDDEN", "Sem permissão para acessar este recurso")
}
func unauthorized() *apiError {
	return apiErr(401, "AUTH_UNAUTHORIZED", "Não autorizado. Token ausente ou inválido")
}
func internalError(err error) *apiError {
	e := apiErr(500, "INTERNAL_ERROR", "Erro interno do servidor")
	e.cause = err
	return e
}
func dbError(err error) *apiError {
	if errors.Is(err, pgx.ErrNoRows) {
		return resourceError("Recurso")
	}
	return internalError(err)
}
func writeError(w http.ResponseWriter, err error) {
	var ae *apiError
	if !errors.As(err, &ae) {
		ae = internalError(err)
	}
	if ae.Status >= 500 {
		slog.Error("request failed", "error", ae.cause)
	}
	writeJSON(w, ae.Status, ae)
}
