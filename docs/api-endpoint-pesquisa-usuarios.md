# API - Pesquisa de Usuários

Documentação dos endpoints de usuários para consumo pelo frontend.

---

## 1. Pesquisa de Usuários

Endpoint de pesquisa com filtros dinâmicos. Todos os filtros são opcionais — enviar o body vazio (ou sem body) retorna todos os usuários ativos.

### Request

```
POST /api/users/search
```

**Headers:**

| Header          | Valor             | Obrigatório |
|-----------------|-------------------|-------------|
| Authorization   | Bearer {token}    | Sim         |
| Content-Type    | application/json  | Não*        |

> *O Content-Type é necessário apenas quando houver body na requisição.

**Permissão necessária:** `ROLE_ADMIN`

**Body (opcional):**

```json
{
  "name": "string",
  "email": "string",
  "register": "string",
  "gender": "string",
  "initialDate": "yyyy-MM-dd",
  "finalDate": "yyyy-MM-dd"
}
```

| Campo         | Tipo       | Descrição                                                        |
|---------------|------------|------------------------------------------------------------------|
| name          | String     | Filtro parcial (LIKE) pelo nome do usuário (case-insensitive)    |
| email         | String     | Filtro parcial (LIKE) pelo e-mail do usuário (case-insensitive)  |
| register      | String     | Filtro parcial (LIKE) pela matrícula do usuário (case-insensitive)|
| gender        | String     | Filtro exato pelo gênero (case-insensitive)                      |
| initialDate   | LocalDate  | Data de nascimento mínima (inclusiva) — formato `yyyy-MM-dd`     |
| finalDate     | LocalDate  | Data de nascimento máxima (inclusiva) — formato `yyyy-MM-dd`     |

> Todos os campos são opcionais. Campos `null` ou em branco são ignorados no filtro.

### Response — 200 OK

```json
[
  {
    "id": 1,
    "name": "João Silva",
    "email": "joao.silva@sisges.com",
    "register": "2024001",
    "role": "ADMIN",
    "birthDate": "1990-05-15",
    "gender": "MASCULINO"
  }
]
```

| Campo     | Tipo       | Descrição                          |
|-----------|------------|------------------------------------|
| id        | Integer    | ID do usuário                      |
| name      | String     | Nome completo                      |
| email     | String     | E-mail institucional               |
| register  | String     | Matrícula / registro               |
| role      | String     | Papel (`ADMIN`, `TEACHER`, `STUDENT`) |
| birthDate | LocalDate  | Data de nascimento (`yyyy-MM-dd`)  |
| gender    | String     | Gênero                             |

### Exemplos de Uso

**Buscar todos os usuários (sem filtro):**

```bash
curl -X POST http://localhost:8080/api/users/search \
  -H "Authorization: Bearer {token}"
```

**Filtrar por nome:**

```bash
curl -X POST http://localhost:8080/api/users/search \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"name": "João"}'
```

**Filtrar por intervalo de data de nascimento:**

```bash
curl -X POST http://localhost:8080/api/users/search \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"initialDate": "1990-01-01", "finalDate": "2000-12-31"}'
```

**Filtrar por gênero e e-mail:**

```bash
curl -X POST http://localhost:8080/api/users/search \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"gender": "FEMININO", "email": "maria"}'
```

---

## 2. Buscar Usuário por ID

Retorna os dados de um único usuário pelo seu ID.

### Request

```
GET /api/users/{id}
```

**Headers:**

| Header          | Valor             | Obrigatório |
|-----------------|-------------------|-------------|
| Authorization   | Bearer {token}    | Sim         |

**Permissão necessária:** `ROLE_ADMIN`

**Path Parameters:**

| Parâmetro | Tipo    | Descrição      |
|-----------|---------|----------------|
| id        | Integer | ID do usuário  |

### Response — 200 OK

```json
{
  "id": 1,
  "name": "João Silva",
  "email": "joao.silva@sisges.com",
  "register": "2024001",
  "role": "ADMIN",
  "birthDate": "1990-05-15",
  "gender": "MASCULINO"
}
```

### Response — 404 Not Found

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "status": 404,
  "message": "Usuário não encontrado(a): 999"
}
```

---

## Observações

- Todos os endpoints requerem autenticação via JWT (Bearer token).
- Apenas usuários com papel `ADMIN` podem acessar estes endpoints.
- Usuários com soft delete (campo `deletedAt` preenchido) são automaticamente excluídos dos resultados.
- Todos os e-mails cadastrados devem pertencer ao domínio `@sisges.com`.
