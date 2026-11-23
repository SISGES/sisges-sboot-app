package api

import (
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"io"
	"mime/multipart"
	"net/http"
	"net/url"
	"os"
	"path"
	"path/filepath"
	"strings"
	"time"
)

const maxUploadSize int64 = 10 << 20

var allowedDocumentExtensions = map[string]bool{".pdf": true, ".txt": true, ".doc": true, ".docx": true, ".xls": true, ".xlsx": true, ".ppt": true, ".pptx": true}

var allowedImageExtensions = map[string]bool{".png": true, ".jpg": true, ".jpeg": true, ".gif": true, ".webp": true}

const postImagePrefix = "post-img"
const profileImagePrefix = "profile-img"

func allowedImagePrefix(value string) bool {
	return value == postImagePrefix || value == profileImagePrefix
}

func allowedImageKey(value string) bool {
	return strings.HasPrefix(value, postImagePrefix+"/") || strings.HasPrefix(value, profileImagePrefix+"/")
}

type R2Storage struct {
	endpoint                             *url.URL
	accessKey, secretKey, bucket, region string
	client                               *http.Client
}

func NewR2Storage(c Config) (*R2Storage, error) {
	u, e := url.Parse(c.R2Endpoint)
	if e != nil || u.Host == "" {
		return nil, fmt.Errorf("invalid SISGES_R2_ENDPOINT")
	}
	if strings.Trim(u.Path, "/") != "" {
		return nil, fmt.Errorf("SISGES_R2_ENDPOINT must be the account endpoint without a bucket path")
	}
	if c.R2AccessKeyID == "" || c.R2SecretAccessKey == "" {
		return nil, fmt.Errorf("R2 credentials are required when storage is enabled")
	}
	if c.R2Bucket != "sisges-prd" {
		return nil, fmt.Errorf("SISGES_R2_BUCKET must be sisges-prd")
	}
	return &R2Storage{u, c.R2AccessKeyID, c.R2SecretAccessKey, c.R2Bucket, c.R2Region, &http.Client{Timeout: 45 * time.Second}}, nil
}
func hmacSHA(key []byte, value string) []byte {
	m := hmac.New(sha256.New, key)
	_, _ = m.Write([]byte(value))
	return m.Sum(nil)
}
func escapeKey(key string) string {
	parts := strings.Split(key, "/")
	for i := range parts {
		parts[i] = url.PathEscape(parts[i])
	}
	return strings.Join(parts, "/")
}
func (s *R2Storage) request(ctx context.Context, method, key, payloadHash, contentType string, body io.Reader, length int64) (*http.Response, error) {
	now := time.Now().UTC()
	date := now.Format("20060102")
	stamp := now.Format("20060102T150405Z")
	uri := strings.TrimRight(s.endpoint.EscapedPath(), "/") + "/" + url.PathEscape(s.bucket) + "/" + escapeKey(key)
	target := *s.endpoint
	target.Path = strings.TrimRight(s.endpoint.Path, "/") + "/" + s.bucket + "/" + key
	canonicalHeaders := "host:" + target.Host + "\n" + "x-amz-content-sha256:" + payloadHash + "\n" + "x-amz-date:" + stamp + "\n"
	signedHeaders := "host;x-amz-content-sha256;x-amz-date"
	canonical := method + "\n" + uri + "\n\n" + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash
	sum := sha256.Sum256([]byte(canonical))
	scope := date + "/" + s.region + "/s3/aws4_request"
	stringToSign := "AWS4-HMAC-SHA256\n" + stamp + "\n" + scope + "\n" + hex.EncodeToString(sum[:])
	kDate := hmacSHA([]byte("AWS4"+s.secretKey), date)
	kRegion := hmacSHA(kDate, s.region)
	kService := hmacSHA(kRegion, "s3")
	signature := hex.EncodeToString(hmacSHA(hmacSHA(kService, "aws4_request"), stringToSign))
	req, e := http.NewRequestWithContext(ctx, method, target.String(), body)
	if e != nil {
		return nil, e
	}
	req.Header.Set("x-amz-date", stamp)
	req.Header.Set("x-amz-content-sha256", payloadHash)
	req.Header.Set("Authorization", "AWS4-HMAC-SHA256 Credential="+s.accessKey+"/"+scope+", SignedHeaders="+signedHeaders+", Signature="+signature)
	if contentType != "" {
		req.Header.Set("Content-Type", contentType)
	}
	if length >= 0 {
		req.ContentLength = length
	}
	return s.client.Do(req)
}
func randomID() (string, error) {
	var b [16]byte
	if _, e := rand.Read(b[:]); e != nil {
		return "", e
	}
	return hex.EncodeToString(b[:]), nil
}
func validSubdir(v string) string {
	v = strings.Trim(strings.TrimSpace(v), "/")
	if v == "" {
		return "general"
	}
	clean := path.Clean(v)
	if clean == "." || clean == ".." || strings.HasPrefix(clean, "../") {
		return "general"
	}
	for _, r := range clean {
		if !(r == '/' || r == '-' || r == '_' || r >= 'a' && r <= 'z' || r >= 'A' && r <= 'Z' || r >= '0' && r <= '9') {
			return "general"
		}
	}
	return clean
}
func findFilePart(reader *multipart.Reader) (*multipart.Part, error) {
	for {
		p, e := reader.NextPart()
		if e != nil {
			return nil, e
		}
		if p.FormName() == "file" && p.FileName() != "" {
			return p, nil
		}
		_ = p.Close()
	}
}
func (a *App) uploadFile(w http.ResponseWriter, r *http.Request) {
	if a.storage == nil {
		writeJSON(w, http.StatusServiceUnavailable, map[string]string{"error": "Armazenamento não configurado"})
		return
	}
	r.Body = http.MaxBytesReader(w, r.Body, maxUploadSize+(1<<20))
	mr, e := r.MultipartReader()
	if e != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "Upload multipart inválido"})
		return
	}
	part, e := findFilePart(mr)
	if e != nil {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "Arquivo não informado"})
		return
	}
	defer part.Close()
	ext := strings.ToLower(filepath.Ext(filepath.Base(part.FileName())))
	if !allowedImageExtensions[ext] && !allowedDocumentExtensions[ext] {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "Tipo de arquivo não permitido. Use imagens, PDF, TXT ou documentos Office"})
		return
	}
	tmp, e := os.CreateTemp("", "sisges-upload-*")
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	name := tmp.Name()
	defer os.Remove(name)
	defer tmp.Close()
	hasher := sha256.New()
	size, e := io.Copy(io.MultiWriter(tmp, hasher), io.LimitReader(part, maxUploadSize+1))
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	if size == 0 {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "Arquivo vazio"})
		return
	}
	if size > maxUploadSize {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "Arquivo muito grande (máx. 10MB)"})
		return
	}
	subdir := r.URL.Query().Get("subdir")
	_ = part.Close()
	for {
		field, nextErr := mr.NextPart()
		if nextErr != nil {
			break
		}
		if field.FormName() == "subdir" {
			value, _ := io.ReadAll(io.LimitReader(field, 256))
			subdir = string(value)
		}
		_ = field.Close()
	}
	subdir = validSubdir(subdir)
	isAttachment := subdir == "lesson-files" || subdir == "materials" || subdir == "activities"
	if !allowedImagePrefix(subdir) && !isAttachment {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "Destino de arquivo não permitido"})
		return
	}
	if !isAttachment && !allowedImageExtensions[ext] {
		writeJSON(w, http.StatusBadRequest, map[string]string{"error": "Este destino aceita apenas imagens"})
		return
	}
	if isAttachment || subdir == postImagePrefix {
		subdir = fmt.Sprintf("%s/%d", subdir, currentPrincipal(r).ID)
	}
	id, e := randomID()
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	key := subdir + "/" + id + ext
	if _, e = tmp.Seek(0, io.SeekStart); e != nil {
		writeError(w, internalError(e))
		return
	}
	contentType := part.Header.Get("Content-Type")
	if contentType == "" {
		contentType = "application/octet-stream"
	}
	resp, e := a.storage.request(r.Context(), http.MethodPut, key, hex.EncodeToString(hasher.Sum(nil)), contentType, tmp, size)
	if e != nil {
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "Erro ao salvar arquivo"})
		return
	}
	defer resp.Body.Close()
	if resp.StatusCode/100 != 2 {
		io.Copy(io.Discard, io.LimitReader(resp.Body, 4096))
		writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "Erro ao salvar arquivo"})
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"path": "/api/files/" + key})
}

// Novas referências devem usar a chave gerada para o usuário e o destino da publicação.
func validOwnedUpload(filePath, destination string, userID int) bool {
	prefix := fmt.Sprintf("/api/files/%s/%d/", destination, userID)
	name, ok := strings.CutPrefix(filePath, prefix)
	ext := path.Ext(name)
	id := strings.TrimSuffix(name, ext)
	if !ok || len(id) != 32 || !(allowedImageExtensions[ext] || (destination != postImagePrefix && allowedDocumentExtensions[ext])) {
		return false
	}
	_, err := hex.DecodeString(id)
	return err == nil
}

func validateRecordFile(filePath *string, destination string, userID int) error {
	if filePath == nil || *filePath == "" || validOwnedUpload(*filePath, destination, userID) {
		return nil
	}
	return validationError("filePath", "Envie o arquivo para este tipo de registro com a conta atual")
}

func (a *App) canReadPostImage(r *http.Request, name string) (bool, error) {
	// O MinIO gravava announcements/ e o armazenamento local usava /uploads/.
	paths := []string{
		"/api/files/post-img/" + name, "/uploads/post-img/" + name, "post-img/" + name,
		"/api/files/announcements/" + name, "/uploads/announcements/" + name, "announcements/" + name,
	}
	var visible bool
	err := a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.announcement a
		WHERE a.deleted_at IS NULL AND a.image_path=ANY($2::text[]) AND (`+visibleAnnouncement+`))`, currentPrincipal(r).Role, paths).Scan(&visible)
	return visible, err
}

// Reconhece os caminhos emitidos pelo armazenamento local legado e pela API.
func (a *App) canReadRecordFile(r *http.Request, destination, key string) (bool, error) {
	p := currentPrincipal(r)
	paths := []string{"/api/files/" + key, "/uploads/" + key, key}
	const material = `SELECT 1
		FROM sisges.discipline_material m
		JOIN sisges.school_class c ON c.id=m.class_id AND c.deleted_at IS NULL
		WHERE m.deleted_at IS NULL AND m.file_path=ANY($1::text[])
		  AND (EXISTS (SELECT 1 FROM sisges.student s WHERE s.user_id=$2 AND s.class_id=m.class_id AND s.deleted_at IS NULL)
		    OR EXISTS (SELECT 1 FROM sisges.teacher t
		        JOIN sisges.teacher_class tc ON tc.teacher_id=t.id AND tc.class_id=m.class_id AND tc.deleted_at IS NULL
		        JOIN sisges.discipline_teacher dt ON dt.teacher_id=t.id AND dt.discipline_id=m.discipline_id
		        WHERE t.user_id=$2 AND t.deleted_at IS NULL))`
	const activity = `SELECT 1
		FROM sisges.evaluative_activity ea
		JOIN sisges.class_meeting cm ON cm.id=ea.class_meeting_id AND cm.deleted_at IS NULL
		WHERE ea.deleted_at IS NULL AND ea.file_path=ANY($1::text[])
		  AND (EXISTS (SELECT 1 FROM sisges.activity_grade g JOIN sisges.student s ON s.id=g.student_id WHERE g.activity_id=ea.id AND s.user_id=$2 AND s.deleted_at IS NULL)
		    OR EXISTS (SELECT 1 FROM sisges.teacher t WHERE t.user_id=$2 AND t.id=cm.teacher_id AND t.deleted_at IS NULL))`
	var query string
	switch destination {
	case "materials":
		query = material
	case "activities":
		query = activity
	case "general":
		query = material + " UNION ALL " + activity
	default:
		return false, nil
	}
	var allowed bool
	err := a.db.QueryRow(r.Context(), "SELECT EXISTS("+query+")", paths, p.ID).Scan(&allowed)
	return allowed, err
}

func (a *App) downloadFile(w http.ResponseWriter, r *http.Request) {
	if a.storage == nil {
		writeError(w, resourceError("Arquivo"))
		return
	}
	key := r.PathValue("key")
	destination, name, _ := strings.Cut(key, "/")
	if destination == "announcements" {
		destination = postImagePrefix
		key = destination + "/" + name
	}
	recordFile := destination == "materials" || destination == "activities" || destination == "general"
	if name == "" || path.Clean(key) != key || strings.ContainsAny(key, "\\\x00") || (!allowedImageKey(key) && destination != "lesson-files" && !recordFile) {
		writeError(w, validationError("key", "Caminho de arquivo inválido"))
		return
	}
	p := currentPrincipal(r)
	if destination == postImagePrefix {
		w.Header().Set("Cache-Control", "private, no-store")
		if p.Role != "ADMIN" {
			visible, err := a.canReadPostImage(r, name)
			if err != nil {
				writeError(w, internalError(err))
				return
			}
			if !visible {
				writeError(w, resourceError("Arquivo"))
				return
			}
		}
	}
	if recordFile && p.Role != "ADMIN" {
		allowed, err := a.canReadRecordFile(r, destination, key)
		if err != nil {
			writeError(w, internalError(err))
			return
		}
		if !allowed {
			writeError(w, forbidden())
			return
		}
	}
	if destination == "lesson-files" {
		ownerPrefix := fmt.Sprintf("lesson-files/%d/", p.ID)
		if p.Role != "ADMIN" && !strings.HasPrefix(key, ownerPrefix) {
			var allowed bool
			err := a.db.QueryRow(r.Context(), `SELECT EXISTS(SELECT 1 FROM sisges.class_meeting m WHERE m.deleted_at IS NULL AND m.attachments @> $2::jsonb AND (EXISTS(SELECT 1 FROM sisges.student s WHERE s.user_id=$1 AND s.class_id=m.class_id AND s.deleted_at IS NULL) OR EXISTS(SELECT 1 FROM sisges.teacher t WHERE t.user_id=$1 AND t.id=m.teacher_id AND t.deleted_at IS NULL)))`, p.ID, fmt.Sprintf(`[{"path":%q}]`, "/api/files/"+key)).Scan(&allowed)
			if err != nil {
				writeError(w, internalError(err))
				return
			}
			if !allowed {
				writeError(w, forbidden())
				return
			}
		}
	}
	if recordFile || destination == "lesson-files" {
		w.Header().Set("Content-Disposition", "attachment")
		w.Header().Set("X-Content-Type-Options", "nosniff")
	}
	empty := sha256.Sum256(nil)
	resp, e := a.storage.request(r.Context(), http.MethodGet, key, hex.EncodeToString(empty[:]), "", nil, -1)
	if e != nil {
		writeError(w, internalError(e))
		return
	}
	defer resp.Body.Close()
	if resp.StatusCode == http.StatusNotFound {
		writeError(w, resourceError("Arquivo"))
		return
	}
	if resp.StatusCode/100 != 2 {
		writeError(w, internalError(fmt.Errorf("storage returned %s", resp.Status)))
		return
	}
	w.Header().Set("Content-Type", resp.Header.Get("Content-Type"))
	if destination != postImagePrefix {
		w.Header().Set("Cache-Control", "private, max-age=300")
	}
	if v := resp.Header.Get("Content-Length"); v != "" {
		w.Header().Set("Content-Length", v)
	}
	w.WriteHeader(http.StatusOK)
	_, _ = io.Copy(w, resp.Body)
}

func (s *R2Storage) delete(ctx context.Context, key string) error {
	empty := sha256.Sum256(nil)
	resp, err := s.request(ctx, http.MethodDelete, key, hex.EncodeToString(empty[:]), "", nil, 0)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	_, _ = io.Copy(io.Discard, io.LimitReader(resp.Body, 4096))
	if resp.StatusCode/100 != 2 && resp.StatusCode != http.StatusNotFound {
		return fmt.Errorf("storage delete returned %s", resp.Status)
	}
	return nil
}

func (a *App) deleteStoredPath(ctx context.Context, storedPath *string) {
	if a.storage == nil || storedPath == nil {
		return
	}
	const prefix = "/api/files/"
	if !strings.HasPrefix(*storedPath, prefix) {
		return
	}
	key := path.Clean(strings.TrimPrefix(*storedPath, prefix))
	if key == "." || key == ".." || strings.HasPrefix(key, "../") || !allowedImageKey(key) {
		return
	}
	_ = a.storage.delete(ctx, key)
}
