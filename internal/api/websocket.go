package api

import (
	"bufio"
	"context"
	"crypto/sha1"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"net/http"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

type feedHub struct {
	mu      sync.RWMutex
	clients map[*feedClient]struct{}
	max     int
}
type feedClient struct {
	conn       net.Conn
	send       chan []byte
	subscribed atomic.Bool
	writeMu    sync.Mutex
}

func (c *feedClient) write(opcode byte, payload []byte) error {
	c.writeMu.Lock()
	defer c.writeMu.Unlock()
	return writeWSFrame(c.conn, opcode, payload)
}

func newFeedHub(max int) *feedHub { return &feedHub{clients: make(map[*feedClient]struct{}), max: max} }
func (h *feedHub) add(c *feedClient) bool {
	h.mu.Lock()
	defer h.mu.Unlock()
	if len(h.clients) >= h.max {
		return false
	}
	h.clients[c] = struct{}{}
	return true
}
func (h *feedHub) remove(c *feedClient) {
	h.mu.Lock()
	if _, ok := h.clients[c]; ok {
		delete(h.clients, c)
		close(c.send)
	}
	h.mu.Unlock()
}
func (h *feedHub) broadcast(event string, id int) {
	body, _ := json.Marshal(struct {
		Type           string `json:"type"`
		AnnouncementID int    `json:"announcementId"`
	}{event, id})
	frame := []byte(fmt.Sprintf("MESSAGE\ndestination:/topic/feed\ncontent-type:application/json\ncontent-length:%d\n\n%s\x00", len(body), body))
	h.mu.RLock()
	defer h.mu.RUnlock()
	for c := range h.clients {
		if !c.subscribed.Load() {
			continue
		}
		select {
		case c.send <- frame:
		default:
		}
	}
}

func (a *App) webSocketFeed(w http.ResponseWriter, r *http.Request) {
	token := r.URL.Query().Get("token")
	r.Header.Set("Authorization", "Bearer "+token)
	if _, e := a.authenticate(r); e != nil {
		writeError(w, e)
		return
	}
	if !strings.EqualFold(r.Header.Get("Upgrade"), "websocket") || !strings.Contains(strings.ToLower(r.Header.Get("Connection")), "upgrade") {
		writeError(w, validationError("upgrade", "WebSocket upgrade obrigatório"))
		return
	}
	key := r.Header.Get("Sec-WebSocket-Key")
	if key == "" {
		writeError(w, validationError("Sec-WebSocket-Key", "Chave WebSocket ausente"))
		return
	}
	hj, ok := w.(http.Hijacker)
	if !ok {
		writeError(w, internalError(fmt.Errorf("websocket hijacking unavailable")))
		return
	}
	conn, rw, e := hj.Hijack()
	if e != nil {
		return
	}
	sum := sha1.Sum([]byte(key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"))
	accept := base64.StdEncoding.EncodeToString(sum[:])
	_, e = fmt.Fprintf(rw, "HTTP/1.1 101 Switching Protocols\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Accept: %s\r\n\r\n", accept)
	if e == nil {
		e = rw.Flush()
	}
	if e != nil {
		_ = conn.Close()
		return
	}
	client := &feedClient{conn: conn, send: make(chan []byte, 8)}
	if !a.feed.add(client) {
		_ = writeWSFrame(conn, 8, []byte("server busy"))
		_ = conn.Close()
		return
	}
	go a.runFeedClient(client)
}
func (a *App) runFeedClient(c *feedClient) {
	defer func() { a.feed.remove(c); _ = c.conn.Close() }()
	done := make(chan struct{})
	go func() {
		defer close(done)
		reader := bufio.NewReaderSize(c.conn, 4096)
		for {
			_ = c.conn.SetReadDeadline(time.Now().Add(2 * time.Minute))
			opcode, payload, e := readWSFrame(reader)
			if e != nil {
				return
			}
			switch opcode {
			case 8:
				return
			case 9:
				_ = c.write(10, payload)
			case 1:
				msg := string(payload)
				if strings.HasPrefix(msg, "CONNECT") || strings.HasPrefix(msg, "STOMP") {
					_ = c.write(1, []byte("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\x00"))
				}
				if strings.HasPrefix(msg, "SUBSCRIBE") && strings.Contains(msg, "destination:/topic/feed") {
					c.subscribed.Store(true)
				}
			}
		}
	}()
	for {
		select {
		case <-done:
			return
		case msg, ok := <-c.send:
			if !ok {
				return
			}
			if e := c.write(1, msg); e != nil {
				return
			}
		}
	}
}
func writeAll(w io.Writer, b []byte) error {
	for len(b) > 0 {
		n, e := w.Write(b)
		if e != nil {
			return e
		}
		b = b[n:]
	}
	return nil
}
func writeWSFrame(conn net.Conn, opcode byte, payload []byte) error {
	_ = conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
	var h [10]byte
	h[0] = 0x80 | opcode
	n := 2
	if len(payload) < 126 {
		h[1] = byte(len(payload))
	} else if len(payload) <= 65535 {
		h[1] = 126
		binary.BigEndian.PutUint16(h[2:4], uint16(len(payload)))
		n = 4
	} else {
		h[1] = 127
		binary.BigEndian.PutUint64(h[2:10], uint64(len(payload)))
		n = 10
	}
	if e := writeAll(conn, h[:n]); e != nil {
		return e
	}
	return writeAll(conn, payload)
}
func readWSFrame(r *bufio.Reader) (byte, []byte, error) {
	h := make([]byte, 2)
	if _, e := io.ReadFull(r, h); e != nil {
		return 0, nil, e
	}
	opcode := h[0] & 0xf
	masked := h[1]&0x80 != 0
	size := uint64(h[1] & 0x7f)
	if size == 126 {
		var b [2]byte
		if _, e := io.ReadFull(r, b[:]); e != nil {
			return 0, nil, e
		}
		size = uint64(binary.BigEndian.Uint16(b[:]))
	} else if size == 127 {
		var b [8]byte
		if _, e := io.ReadFull(r, b[:]); e != nil {
			return 0, nil, e
		}
		size = binary.BigEndian.Uint64(b[:])
	}
	if size > 64<<10 {
		return 0, nil, fmt.Errorf("websocket frame too large")
	}
	var mask [4]byte
	if masked {
		if _, e := io.ReadFull(r, mask[:]); e != nil {
			return 0, nil, e
		}
	}
	payload := make([]byte, size)
	if _, e := io.ReadFull(r, payload); e != nil {
		return 0, nil, e
	}
	if masked {
		for i := range payload {
			payload[i] ^= mask[i%4]
		}
	}
	return opcode, payload, nil
}

func (a *App) expiryLoop(ctx context.Context) {
	ticker := time.NewTicker(time.Minute)
	defer ticker.Stop()
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			rows, e := a.db.Query(ctx, `DELETE FROM sisges.announcement WHERE active_until<now() RETURNING id,image_path`)
			if e != nil {
				continue
			}
			for rows.Next() {
				var id int
				var imagePath *string
				if rows.Scan(&id, &imagePath) == nil {
					a.feed.broadcast("ANNOUNCEMENT_DELETED", id)
					a.deleteStoredPath(ctx, imagePath)
				}
			}
			rows.Close()
		}
	}
}
