# Files and live feed

## Binary files

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `POST /api/upload` | ADMIN, TEACHER | Multipart `file` part with filename, plus `subdir` | `200 {"path":"/api/files/…"}`. Use this API-relative path in the corresponding record. One nonempty file per call, up to 10 MiB. |
| `GET /api/files/{key...}` | Any, additional scope for lesson files | Fetch returned path with bearer header | `200` binary stream with stored Content-Type and `Cache-Control: private, max-age=300`. Missing object or unconfigured storage returns `404`. |

Specify the upload destination as a query parameter. If using a multipart field instead, put the `subdir` field **after** the `file` part. The handler scans to the first named file, then reads subsequent form fields. The whole multipart request is limited to 11 MiB. [Upload implementation](../../internal/api/storage.go)

| `subdir` | Accepted filename extensions | Resulting object scope |
| --- | --- | --- |
| `post-img`, `profile-img` | `.png .jpg .jpeg .gif .webp` | Random filename under that prefix. Authenticated file reads. |
| `lesson-files` | Listed image extensions plus `.pdf .txt .doc .docx .xls .xlsx .ppt .pptx` | Random filename under `lesson-files/<userId>/`. Downloadable by an admin or the uploader. For another user, the exact path must appear on an active meeting that they teach or whose class they currently attend. Served as an attachment with `nosniff`. |

A typical teacher upload uses the locally available file `worksheet.pdf` and a previously obtained token. A response such as `{"path":"/api/files/lesson-files/27/0123456789abcdef0123456789abcdef.pdf"}` can be referenced in a daily lesson as `{"name":"worksheet.pdf","path":"<returned path>"}`. [Lesson record](lessons.md)

```sh
curl -H "Authorization: Bearer $TOKEN" -F 'file=@worksheet.pdf' \
  'http://localhost:8080/api/upload?subdir=lesson-files'
```

Missing files, invalid destination, disallowed extension, or invalid file size typically return `400 {"error":"…"}`. Storage unavailable at upload returns `503 {"error":"Armazenamento não configurado"}`. Storage write failure can return `500 {"error":"Erro ao salvar arquivo"}`. Clients should also handle the common error envelope for authentication and internal errors. [Storage handler](../../internal/api/storage.go)

## Announcement notifications

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `GET /ws` | Any | Upgrade to WebSocket at `/ws?token=<accessToken>` on the API host | `101 Switching Protocols`. Invalid token returns `401`. A missing upgrade/key returns `400` with a valid session. |

Use `wss` with TLS and keep the query token out of logs. Send each frame below as a WebSocket text message, substituting LF for `\n` and a single NUL byte for `\0`. The endpoint supports a small STOMP-compatible feed subscription. [WebSocket implementation](../../internal/api/websocket.go)

1. Send `CONNECT\naccept-version:1.2\n\n\0`. The reply is `CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\0`.
2. Send `SUBSCRIBE\nid:feed\ndestination:/topic/feed\n\n\0`.
3. On a `MESSAGE` for `/topic/feed`, parse the JSON body, e.g. `{"type":"ANNOUNCEMENT_UPDATED","announcementId":42}`. Event types are `ANNOUNCEMENT_CREATED`, `ANNOUNCEMENT_UPDATED` and `ANNOUNCEMENT_DELETED`. Expiry can emit a deletion.
4. Refetch `GET /api/announcements/feed` with the session token to obtain the caller's current visible list. Likes and comments have their own REST reads.

Delivery is best-effort and process-local. The server admits up to 256 feed connections per process, queues eight outgoing notifications per client and drops notifications if the client queue is full. Incoming frames are limited to 64 KiB and the read deadline is two minutes. On reconnection, resubscribe and reload the feed to recover missed changes. [Feed hub](../../internal/api/websocket.go) · [Announcement REST contract](communications.md)
