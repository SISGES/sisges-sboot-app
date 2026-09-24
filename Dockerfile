FROM golang:1.24-alpine AS build
WORKDIR /src

COPY go.mod go.sum ./
RUN go mod download
COPY cmd ./cmd
COPY internal ./internal
RUN CGO_ENABLED=0 GOOS=linux go build \
    -trimpath -ldflags="-s -w -buildid=" \
    -o /out/sisges ./cmd/sisges
RUN mkdir -p /out/tmp && chmod 1777 /out/tmp

FROM scratch
COPY --from=build /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=build /out/sisges /sisges
COPY --from=build /out/tmp /tmp
COPY db/migration /db/migration

USER 65532:65532
ENV PORT=8080 \
    SISGES_MIGRATION_DIR=/db/migration \
    SISGES_DB_MAX_CONNECTIONS=6 \
    GOMEMLIMIT_BYTES=167772160 \
    GOGC_PERCENT=75
EXPOSE 8080
ENTRYPOINT ["/sisges"]
