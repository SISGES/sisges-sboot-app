# School communications

## Materials

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `GET /api/materials` | Any | Query `classId` for teachers/admins, optional `disciplineId` | `200 Material[]`, newest first, up to 1,000. Uses the student's assigned class for student accounts. For teachers the list filters by the specified class and the teacher's disciplines. |
| `POST /api/materials` | TEACHER | `{disciplineId,classId,title,description?,materialType?,filePath?}` | `201 Material`. Requires a nonblank title, assignment to the class, and a discipline linked to both teacher and class. |
| `DELETE /api/materials/{id}` | TEACHER | Material ID | `204` on soft deletion by its creating teacher. Missing material or different creator returns `400` for a resolved teacher. |

**Material** includes `id,disciplineId,disciplineName,classId,className,title,description,materialType,filePath,createdAt`. The material API accepts `filePath` as a string. Supported binary upload destinations are documented separately. [Materials handler](../../internal/api/content.go) · [Upload contract](files-and-feed.md)

## Announcements, likes and comments

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `GET /api/announcements/feed` | Public, optional bearer | No filters | `200 Announcement[]`, newest first, capped at 500. Applies active window and `hiddenForRoles`. Anonymous callers use the student view. Supplying a valid token sets the caller's role and personal like flag. |
| `GET /api/announcements` | ADMIN | No body | `200 Announcement[]` of up to 500 nondeleted announcements, newest first, including entries outside the active window. |
| `GET /api/announcements/{id}` | ADMIN | Announcement ID | `200 Announcement` or `404`. |
| `POST /api/announcements` | ADMIN, TEACHER | `{title,type,content?,imagePath?,targetRoles?,hiddenForRoles?,activeFrom?,activeUntil?,ttlHours?}` | `201 Announcement`. Requires nonblank title and `type` of `TEXT` or `IMAGE`. |
| `PUT /api/announcements/{id}` | ADMIN | Optional announcement fields | `200 Announcement`. Applies supplied non-null fields. Empty `hiddenForRoles:[]` clears the exclusion list. To change expiry, send `activeUntil`. The update handler ignores `ttlHours`. |
| `DELETE /api/announcements/{id}` | ADMIN | Announcement ID | `204`. Deletes the row, broadcasts deletion and attempts stored image cleanup. |
| `POST /api/announcements/{id}/like` | Any | No body | `200 {liked}` with a boolean. Toggles the caller's like. Repeating the request reverses it. |
| `GET /api/announcements/{id}/likes` | Any | No body | `200 {count,likedBy:[{id,name}]}` in like creation order. |
| `GET /api/announcements/{id}/comments` | Any | No body | `200 Comment[]` in creation order. |
| `POST /api/announcements/{id}/comments` | Any | `{content}` | `201 Comment`. Content must be nonblank, at most 250 bytes before trimming. |
| `PUT /api/announcements/{id}/comments/{commentID}` | Any, comment author | `{content}` | `200 Comment` with trimmed text. Only the active matching comment owned by the caller can be edited. |
| `DELETE /api/announcements/{id}/comments/{commentID}` | Any, comment author | No body | `204` on soft deletion. A missing or other user's matching comment returns `403`. |

**Announcement** includes `id,title,content,type,imagePath,targetRoles,hiddenForRoles,activeFrom,activeUntil,createdAt,likeCount,likedByCurrentUser,commentCount,authorId,authorName,authorProfileImagePath`. On creation, `activeFrom` defaults to now, `targetRoles` defaults to all three roles, and an optional TTL of 1, 4, 10, 24, 48 or 168 hours overrides `activeUntil`. Feed visibility uses the hidden-role list and timestamps. Treat `targetRoles` as metadata in this version. Likes/comments operate by announcement ID and require a session. A **Comment** contains `id,content,createdAt,user:{id,name}`. [Announcement and social handlers](../../internal/api/content.go)

For example, `{"title":"School assembly","type":"TEXT","content":"Meet in the hall.","ttlHours":24}` creates an announcement lasting 24 hours. When an announcement expires, the feed excludes it and background cleanup periodically removes it. [Expiry and feed notifications](../../internal/api/websocket.go)

## Calendar

| Operation | Access | Request | Response and behavior |
| --- | --- | --- | --- |
| `GET /api/events` | Any | Optional `includePast=true` for ADMIN | `200 Event[]`. Returns upcoming events in chronological order. Admins may also see past events after upcoming ones, most recent past first. Maximum 500. |
| `POST /api/events` | ADMIN | `{title,description?,eventAt,audience,classId?}` | `201 Event`. Nonblank title and timestamp from now through the end of the current São Paulo calendar year. `audience` is `ALL`, `TEACHERS` or `CLASS`. Class audience requires a class ID. |
| `DELETE /api/events/{id}` | ADMIN | Event ID | `204` on deletion, `404` if missing. |

**Event** includes `id,title,description,eventAt,audience,classId,className,createdById,createdByName,createdAt`. Creator fields are populated for administrators. Users see all-school events, teachers also see teacher-wide events, and class events appear to students and teachers linked to that class. Admins see all audiences. Use RFC 3339 with an explicit offset for `eventAt`. [Event handlers](../../internal/api/content.go) · [Date boundary](../../internal/api/event_date.go)
