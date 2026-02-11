# Issues – melhorias do code review

Crie as issues abaixo no GitHub (**Issues** → **New issue**). Assigne-as a você mesmo.  
Depois de abrir o PR (branch `feature/13` → `developer`), use no **corpo do PR** a linha `Closes #N` para cada issue (ex.: `Closes #13`, `Closes #14`, …) para fechá-las ao fazer o merge.

---

## Issue 1 – GlobalExceptionHandler: BadCredentialsException e outros casos

**Título:** GlobalExceptionHandler: tratar BadCredentialsException e expandir handlers

**Descrição:**
- Tratar `BadCredentialsException` → 401 com mensagem "E-mail ou senha inválidos".
- Tratar `AuthenticationException` → 401.
- Tratar `MethodArgumentNotValidException` → 400 com lista de erros de validação (field + message).
- Tratar `DataIntegrityViolationException` → 409 com mensagem genérica.
- Tratar `Exception` → 500 com mensagem genérica.
- Manter handler de `IllegalArgumentException` → 400.

**Labels (sugestão):** enhancement, code-review

---

## Issue 2 – JwtAuthenticationFilter: log com SLF4J

**Título:** JwtAuthenticationFilter: log em falha de validação do JWT com SLF4J

**Descrição:**
- Adicionar logger SLF4J ao `JwtAuthenticationFilter`.
- No `catch` de exceção na validação do JWT, registrar em nível debug a mensagem da exceção (evitar `catch` vazio).

**Labels (sugestão):** enhancement, code-review

---

## Issue 3 – JWT: variável de ambiente para secret

**Título:** JWT: usar variável de ambiente para chave secreta

**Descrição:**
- Em produção, usar variável de ambiente `SECURITY_JWT_SECRET_KEY` (e opcionalmente `SECURITY_JWT_EXPIRATION_TIME`).
- Em `application.properties`: `security.jwt.secret-key=${SECURITY_JWT_SECRET_KEY}`.
- Em `application-local.properties`: fallback com valor padrão para desenvolvimento local.

**Labels (sugestão):** enhancement, security, code-review

---

## Issue 4 – Aluno: responsável obrigatório

**Título:** Registro de aluno: validar responsável obrigatório (regra de negócio)

**Descrição:**
- No registro de usuário com role STUDENT, exigir que seja informado `responsibleId` ou `responsibleData`.
- Caso contrário, lançar `IllegalArgumentException` com mensagem clara.

**Labels (sugestão):** enhancement, business-rule, code-review

---

## Issue 5 – ResponsibleData: alternativePhone e alternativeEmail opcionais

**Título:** ResponsibleData: tornar alternativePhone e alternativeEmail opcionais

**Descrição:**
- Remover `@NotBlank` de `alternativePhone` e `alternativeEmail` em `RegisterUserRequest.ResponsibleData`.
- Ajustar entidade `StudentResponsible` para permitir nulo nessas colunas.
- Criar migração Flyway (V4) para alterar as colunas para nullable.

**Labels (sugestão):** enhancement, code-review

---

## Issue 6 – pom.xml: scope runtime para jjwt-impl e jjwt-jackson

**Título:** pom.xml: adicionar scope runtime em jjwt-impl e jjwt-jackson

**Descrição:**
- Adicionar `<scope>runtime</scope>` nas dependências `jjwt-impl` e `jjwt-jackson`.

**Labels (sugestão):** enhancement, code-review

---

## Após criar as issues

1. Anote os números das issues (ex.: 13, 14, 15, 16, 17, 18).
2. Abra um **Pull Request** de `feature/13` para `developer`.
3. No corpo do PR, adicione: `Closes #13`, `Closes #14`, … (com os números reais).
4. Faça o merge do PR para fechar as issues automaticamente.
