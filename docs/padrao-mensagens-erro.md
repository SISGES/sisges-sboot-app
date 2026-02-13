# Padrão de Mensagens de Erro — API SISGES

Documento de referência para o **frontend** sobre como a API retorna erros e como tratá-los.

---

## 1. Estrutura da Resposta de Erro

Toda resposta de erro da API segue a mesma estrutura JSON:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Erro de validação",
  "timestamp": "2026-02-11T14:30:00.000000",
  "errors": [
    {
      "field": "email",
      "message": "E-mail inválido"
    }
  ]
}
```

### Campos

| Campo       | Tipo                | Obrigatório | Descrição                                                                 |
|-------------|---------------------|:-----------:|---------------------------------------------------------------------------|
| `status`    | `number`            | Sim         | Código HTTP do erro (ex: 400, 401, 404, 500)                             |
| `code`      | `string`            | Sim         | Código de erro padronizado (ver tabela abaixo). Usar para lógica no front |
| `message`   | `string`            | Sim         | Mensagem descritiva legível para humanos                                  |
| `timestamp` | `string` (ISO 8601) | Sim         | Data/hora em que o erro ocorreu                                           |
| `errors`    | `array` ou `null`   | Não         | Presente **somente** em erros de validação (`VALIDATION_ERROR`)           |

> **Dica:** O campo `code` é estável e deve ser usado para decisões programáticas (ex: exibir formulário com erros). O campo `message` pode mudar e é para exibição ao usuário.

---

## 2. Códigos de Erro

| Código                       | HTTP Status | Quando ocorre                                                      |
|------------------------------|:-----------:|--------------------------------------------------------------------|
| `AUTH_INVALID_CREDENTIALS`   | 401         | E-mail ou senha incorretos no login                                |
| `AUTH_UNAUTHORIZED`          | 401         | Token JWT ausente, inválido ou expirado                            |
| `AUTH_FORBIDDEN`             | 403         | Usuário autenticado mas sem permissão para o recurso               |
| `VALIDATION_ERROR`           | 400         | Campos do request body falharam na validação (campo `errors` virá) |
| `BUSINESS_RULE_VIOLATION`    | 400         | Regra de negócio violada (ex: e-mail já cadastrado)                |
| `RESOURCE_NOT_FOUND`         | 404         | Entidade buscada não existe (ex: turma, responsável)               |
| `DATA_CONFLICT`              | 409         | Conflito de integridade no banco (dados duplicados)                |
| `INTERNAL_ERROR`             | 500         | Erro inesperado no servidor                                       |

---

## 3. Exemplos por Cenário

### 3.1 Login com credenciais inválidas

**Request:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "usuario@teste.com",
  "password": "senhaErrada"
}
```

**Response (401):**
```json
{
  "status": 401,
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "E-mail ou senha inválidos",
  "timestamp": "2026-02-11T14:30:00.000000"
}
```

### 3.2 Erro de validação no registro

**Request:**
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "",
  "email": "email-invalido",
  "register": "",
  "password": "123",
  "role": "STUDENT"
}
```

**Response (400):**
```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Erro de validação",
  "timestamp": "2026-02-11T14:30:00.000000",
  "errors": [
    { "field": "name", "message": "Nome é obrigatório" },
    { "field": "email", "message": "E-mail inválido" },
    { "field": "register", "message": "Registro/matrícula é obrigatório" },
    { "field": "password", "message": "Senha deve ter no mínimo 6 caracteres" },
    { "field": "birthDate", "message": "Data de nascimento é obrigatória" },
    { "field": "gender", "message": "Gênero é obrigatório" }
  ]
}
```

### 3.3 E-mail já cadastrado (regra de negócio)

**Response (400):**
```json
{
  "status": 400,
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "E-mail já cadastrado: usuario@teste.com",
  "timestamp": "2026-02-11T14:30:00.000000"
}
```

### 3.4 Recurso não encontrado

**Response (404):**
```json
{
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Turma não encontrado(a): 999",
  "timestamp": "2026-02-11T14:30:00.000000"
}
```

### 3.5 Acesso negado (sem permissão)

**Response (403):**
```json
{
  "status": 403,
  "code": "AUTH_FORBIDDEN",
  "message": "Sem permissão para acessar este recurso",
  "timestamp": "2026-02-11T14:30:00.000000"
}
```

### 3.6 Token ausente ou inválido

**Response (401):**
```json
{
  "status": 401,
  "code": "AUTH_UNAUTHORIZED",
  "message": "Não autorizado. Token ausente ou inválido",
  "timestamp": "2026-02-11T14:30:00.000000"
}
```

### 3.7 Conflito de dados

**Response (409):**
```json
{
  "status": 409,
  "code": "DATA_CONFLICT",
  "message": "Conflito de dados. Verifique se os dados não estão duplicados ou inconsistentes",
  "timestamp": "2026-02-11T14:30:00.000000"
}
```

### 3.8 Erro interno do servidor

**Response (500):**
```json
{
  "status": 500,
  "code": "INTERNAL_ERROR",
  "message": "Erro interno do servidor",
  "timestamp": "2026-02-11T14:30:00.000000"
}
```

---

## 4. Guia de Tratamento no Frontend

### 4.1 Interceptor HTTP (exemplo com Axios)

```typescript
import axios from 'axios';

const api = axios.create({ baseURL: '/api' });

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const err = error.response?.data;

    if (!err?.code) {
      // Sem resposta do servidor (rede, timeout, etc.)
      showToast('Erro de conexão. Tente novamente.');
      return Promise.reject(error);
    }

    switch (err.code) {
      case 'AUTH_INVALID_CREDENTIALS':
        showToast(err.message);
        break;

      case 'AUTH_UNAUTHORIZED':
        // Token expirado/inválido → redirecionar para login
        clearAuthToken();
        redirectToLogin();
        break;

      case 'AUTH_FORBIDDEN':
        showToast('Você não tem permissão para esta ação.');
        break;

      case 'VALIDATION_ERROR':
        // Tratar erros de campo individualmente no formulário
        setFormErrors(err.errors);
        break;

      case 'BUSINESS_RULE_VIOLATION':
        showToast(err.message);
        break;

      case 'RESOURCE_NOT_FOUND':
        showToast(err.message);
        break;

      case 'DATA_CONFLICT':
        showToast(err.message);
        break;

      case 'INTERNAL_ERROR':
      default:
        showToast('Ocorreu um erro inesperado. Tente novamente mais tarde.');
        break;
    }

    return Promise.reject(error);
  }
);
```

### 4.2 Tratamento de erros de validação em formulários

```typescript
interface FieldError {
  field: string;
  message: string;
}

interface ApiError {
  status: number;
  code: string;
  message: string;
  timestamp: string;
  errors?: FieldError[];
}

function setFormErrors(errors: FieldError[]) {
  // Mapear erros para os campos do formulário
  const fieldErrors: Record<string, string> = {};
  errors.forEach((err) => {
    fieldErrors[err.field] = err.message;
  });
  // Usar com o estado do formulário (React Hook Form, Formik, etc.)
  return fieldErrors;
}
```

### 4.3 Resumo de ações por código

| Código                     | Ação recomendada no frontend                                      |
|----------------------------|-------------------------------------------------------------------|
| `AUTH_INVALID_CREDENTIALS` | Exibir mensagem no formulário de login                            |
| `AUTH_UNAUTHORIZED`        | Limpar token local e redirecionar para `/login`                   |
| `AUTH_FORBIDDEN`           | Exibir toast/alert de permissão negada                            |
| `VALIDATION_ERROR`         | Mapear `errors[]` aos campos do formulário                        |
| `BUSINESS_RULE_VIOLATION`  | Exibir `message` em toast ou alert                                |
| `RESOURCE_NOT_FOUND`       | Exibir mensagem ou redirecionar para página 404                   |
| `DATA_CONFLICT`            | Exibir `message` e sugerir que o usuário verifique dados          |
| `INTERNAL_ERROR`           | Exibir mensagem genérica e orientar para tentar novamente depois  |

---

## 5. Tipagem TypeScript Completa

```typescript
/** Resposta de erro padrão da API SISGES */
export interface SisgesErrorResponse {
  status: number;
  code:
    | 'AUTH_INVALID_CREDENTIALS'
    | 'AUTH_UNAUTHORIZED'
    | 'AUTH_FORBIDDEN'
    | 'VALIDATION_ERROR'
    | 'BUSINESS_RULE_VIOLATION'
    | 'RESOURCE_NOT_FOUND'
    | 'DATA_CONFLICT'
    | 'INTERNAL_ERROR';
  message: string;
  timestamp: string;
  errors?: FieldValidationError[];
}

export interface FieldValidationError {
  field: string;
  message: string;
}
```
