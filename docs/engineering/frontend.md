# Frontend engineering guide

The school portal is a React 18 and TypeScript single-page application. The browser matches a route, checks the local session and role, mounts a page in the shared shell, then calls the Go API through a domain service. Use Brazilian Portuguese for existing screen labels and keep wire property names exactly as the handler defines them. [Router](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/App.tsx) · [Service directory](https://github.com/SISGES/sisges-react-app/tree/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services)

## Where a change belongs

| Concern | Entry point | Integration detail |
| --- | --- | --- |
| Screen and navigation | `src/App.tsx`, `src/pages`, `src/config/navConfig.ts` | Register the route and decide whether it belongs in each role's menu. Routes may exist without a navigation link. |
| Session and access | `AuthContext`, `authService`, `ProtectedRoute`, role wrappers | Restore waits for server validation. TeacherRoute admits teachers and admins. API middleware and handler checks determine what operations each may perform. |
| Request and response shape | `src/services/api.ts`, domain services, `src/types/auth.ts` | The shared client sets bearer auth and normalizes errors. Domain types also live beside services. Handle empty success bodies deliberately. |
| Uploads and protected images | `uploadService`, `ProtectedFile`, `UserAvatar` | Upload multipart form data, store the API-returned path, and fetch private files with a bearer token. Verify the destination against the server allowlist. |
| State that outlives a route | Providers in `src/main.tsx` | Timetable drafts live in memory per user and class. Route changes preserve them. A reload or an identity change clears them. |
| Styling, feedback and reusable controls | `src/index.css`, `src/themes/colors.ts`, `ThemeContext`, `src/components/ui` | ThemeProvider chooses the saved preference or system mode, sets `data-theme` and emits CSS variables. Use the shared dialog and toast providers for consistent interaction. |
| Dates and real-time updates | `src/utils/eventDateTime.ts`, `src/hooks/useStompFeed.ts` | Preserve the offset when sending event instants. A feed notification should trigger a fresh API read. |

[App providers](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/main.tsx) · [Theme](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/contexts/ThemeContext.tsx) · [Navigation](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/config/navConfig.ts) · [Brand assets and usage](https://github.com/SISGES/sisges-react-app/tree/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/brand/open-book)

## Trace a change from UI to storage

For a timetable change, begin at the administrator route `/admin/grade-horaria`. The page edits a per-class draft, uses `scheduleService` to save slots and a revision, and handles the server result. The API applies its class, discipline and revision checks before replacing the schedule in PostgreSQL. Exercise navigation away and back, reset confirmation, a successful save and a stale save from a second tab. A client-side draft and a persisted schedule have different lifetimes. [Timetable screen](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/pages/Timetable/Timetable.tsx) · [Draft provider](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/contexts/TimetableDraftProvider.tsx) · [Schedule handlers](../../internal/api/schedules.go)

For an API change, verify the method, role, body and HTTP status against the handler before changing the TypeScript type. At this baseline the Go token validation handler returns 200 with no body, while the shared GET helper always calls `response.json()`. Login can succeed and session restore can subsequently clear the browser session. Browser tests that stub validation with a JSON object can miss this mismatch. [Client](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/services/api.ts) · [Validation handler](../../internal/api/users.go)

## Verify the UI

Build from the frontend root with `npm ci` and `npm run build`. Check affected flows as each allowed role and one excluded role. Exercise keyboard navigation, focus restoration after dialogs, accessible field names, error text, both themes and mobile overflow. For an HTTP or file change, repeat the flow against the actual API and inspect the response body and content type. [Test strategy](test-strategy.md) · [Local setup](development.md)
