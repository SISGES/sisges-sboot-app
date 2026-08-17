# Análise: CAPTCHA e 2FA no Login — SISGES

Documento de análise técnica sobre a implementação de **CAPTCHA** e **Autenticação em Dois Fatores (2FA)** no fluxo de login do SISGES.

---

## 1. Contexto Atual

- **Autenticação:** Login via e-mail + senha, retornando JWT (stateless)
- **Segurança:** Spring Security + BCrypt + JWT com HS256
- **Perfis:** ADMIN, TEACHER, STUDENT
- **Vulnerabilidades atuais:**
  - Sem proteção contra ataques de brute force no endpoint `/api/auth/login`
  - Sem rate limiting
  - Sem verificação de identidade além de senha

---

## 2. CAPTCHA

### 2.1 O que é e para que serve

CAPTCHA (Completely Automated Public Turing test to tell Computers and Humans Apart) serve para **impedir bots** de acessarem endpoints públicos como login e registro. Não verifica a identidade do usuário, apenas se ele é humano.

### 2.2 Opções Disponíveis

| Solução                    | Prós                                               | Contras                        | Custo      |
| -------------------------- | -------------------------------------------------- | ------------------------------ | ---------- |
| **Google reCAPTCHA v3**    | Invisível, sem fricção para o usuário, score-based | Depende do Google, privacidade | Gratuito\* |
| **Google reCAPTCHA v2**    | Amplamente conhecido ("Não sou um robô")           | Fricção para o usuário         | Gratuito\* |
| **hCaptcha**               | Alternativa ao Google, mais privacidade            | Menos popular no Brasil        | Gratuito\* |
| **Turnstile (Cloudflare)** | Invisível, boa privacidade, rápido                 | Menor ecossistema              | Gratuito   |

> \*Gratuito até determinado volume de requisições.

### 2.3 Recomendação: Google reCAPTCHA v3

Para o SISGES, o reCAPTCHA v3 é a melhor opção porque:

- **Invisível:** Não adiciona fricção para professores e responsáveis
- **Score-based:** Retorna um score de 0.0 a 1.0 (ex: acima de 0.5 é humano)
- **Amplamente suportado:** Boa documentação e SDKs

### 2.4 Como Funcionaria

```
┌──────────┐       ┌──────────┐       ┌──────────────┐
│ Frontend │──(1)──│ Google   │       │              │
│          │◄─(2)──│ reCAPTCHA│       │   Backend    │
│          │──(3)──│          │       │   SISGES     │
│          │       └──────────┘       │              │
│          │───────────(4)───────────►│              │
│          │                          │──(5)──►Google│
│          │◄──────────(6)────────────│              │
└──────────┘                          └──────────────┘
```

1. Frontend carrega o script do reCAPTCHA e executa no submit do formulário
2. Google retorna um **token** para o frontend
3. Frontend envia o token junto com email/senha no request de login
4. Backend recebe o token e as credenciais
5. Backend valida o token chamando a API do Google (`https://www.google.com/recaptcha/api/siteverify`)
6. Se o score for aceitável (>= 0.5), processa o login normalmente

### 2.5 O que Precisaria Ser Feito (CAPTCHA)

#### Backend (Spring Boot)

1. **Adicionar dependência HTTP Client** (já disponível via Spring Web):
   - Usar `RestTemplate` ou `WebClient` para chamar a API do Google

2. **Criar propriedades de configuração:**

   ```properties
   # application.properties
   recaptcha.secret-key=${RECAPTCHA_SECRET_KEY}
   recaptcha.verify-url=https://www.google.com/recaptcha/api/siteverify
   recaptcha.threshold=0.5
   ```

3. **Criar `RecaptchaService`** para validar o token:
   - Recebe o token do frontend
   - Chama a API do Google para verificar
   - Retorna `true/false` com base no score

4. **Alterar `LoginRequest`** para incluir o campo `captchaToken`:

   ```java
   private String captchaToken;
   ```

5. **Alterar `AuthService.login()`** para validar o captcha antes de autenticar

6. **Criar nova exceção ou usar `BusinessRuleException`** para captcha inválido

#### Frontend

1. **Registrar o site no [Google reCAPTCHA Admin](https://www.google.com/recaptcha/admin)**
2. **Instalar o SDK** (`react-google-recaptcha-v3` ou script direto)
3. **Executar o reCAPTCHA** no submit do formulário de login/registro
4. **Enviar o token** junto com as credenciais

#### Infraestrutura

- Configurar variável de ambiente `RECAPTCHA_SECRET_KEY` no servidor
- Nenhuma alteração em banco de dados necessária

#### Estimativa de esforço: **2-4 horas** (backend + frontend)

---

## 3. Autenticação em Dois Fatores (2FA)

### 3.1 O que é e para que serve

2FA adiciona uma **segunda camada de verificação de identidade** após a senha. O usuário precisa provar que possui um segundo fator (dispositivo, e-mail, etc.) além da senha.

### 3.2 Opções Disponíveis

| Método                 | Prós                                  | Contras                                     | Custo         |
| ---------------------- | ------------------------------------- | ------------------------------------------- | ------------- |
| **TOTP (Google Auth)** | Offline, seguro, sem custo recorrente | Requer app no celular, setup mais complexo  | Gratuito      |
| **OTP por E-mail**     | Simples, não requer app extra         | Depende do servidor de e-mail, menos seguro | Custo de SMTP |
| **OTP por SMS**        | Familiar para o usuário               | Custo por SMS, menos seguro (SIM swap)      | Caro          |
| **Push Notification**  | UX excelente                          | Complexo, requer app mobile                 | Complexo      |

### 3.3 Recomendação: TOTP (Time-based One-Time Password)

Para o SISGES, o **TOTP** é a melhor opção porque:

- **Gratuito:** Sem custos recorrentes (sem SMS, sem serviço de e-mail)
- **Seguro:** Códigos gerados offline, baseados em tempo
- **Padrão aberto:** Funciona com Google Authenticator, Authy, Microsoft Authenticator
- **Adequado ao público:** ADMIN e TEACHER são perfis que se beneficiam de 2FA; STUDENT pode ser opcional

> **Alternativa viável:** OTP por e-mail como fallback ou opção para quem não quer instalar app.

### 3.4 Como Funcionaria (TOTP)

#### Fluxo de Ativação (uma única vez)

```
┌──────────┐                          ┌──────────────┐
│ Frontend │───(1) GET /2fa/setup────►│   Backend    │
│          │◄──(2) QR Code + Secret───│   SISGES     │
│          │                          │              │
│  Usuário │──scanneia QR no App──►📱 │              │
│          │                          │              │
│          │───(3) POST /2fa/verify───│              │
│          │   { code: "123456" }     │              │
│          │◄──(4) 2FA ativado────────│              │
└──────────┘                          └──────────────┘
```

1. Usuário solicita ativação do 2FA
2. Backend gera um **secret** TOTP e retorna como QR Code
3. Usuário escaneia o QR no app autenticador e envia o código gerado
4. Backend valida o código e marca 2FA como ativo para o usuário

#### Fluxo de Login (com 2FA ativo)

```
┌──────────┐                          ┌──────────────┐
│ Frontend │───(1) POST /auth/login──►│   Backend    │
│          │   { email, password }    │   SISGES     │
│          │◄──(2) { requires2fa }────│              │
│          │                          │              │
│          │───(3) POST /auth/2fa ───►│              │
│          │   { tempToken, code }    │              │
│          │◄──(4) { accessToken }────│              │
└──────────┘                          └──────────────┘
```

1. Usuário faz login normalmente com email/senha
2. Backend detecta que 2FA está ativo e retorna um **token temporário** (não o JWT final) + flag `requires2fa: true`
3. Frontend exibe tela para digitar o código de 6 dígitos e envia com o token temporário
4. Backend valida o código TOTP e retorna o JWT definitivo

### 3.5 O que Precisaria Ser Feito (2FA)

#### Banco de Dados

Nova migration Flyway com alterações na tabela `users`:

```sql
ALTER TABLE sisges.users ADD COLUMN two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE sisges.users ADD COLUMN two_factor_secret VARCHAR(64);
```

#### Backend (Spring Boot)

1. **Adicionar dependência TOTP** no `pom.xml`:

   ```xml
   <dependency>
       <groupId>dev.samstevens.totp</groupId>
       <artifactId>totp</artifactId>
       <version>1.7.1</version>
   </dependency>
   ```

2. **Alterar entidade `User`:**
   - Adicionar campos `twoFactorEnabled` (boolean) e `twoFactorSecret` (String)

3. **Criar `TwoFactorService`:**
   - `generateSecret()` — gera o secret TOTP
   - `generateQrCodeUri(secret, email)` — gera URI para QR Code
   - `verifyCode(secret, code)` — valida o código de 6 dígitos

4. **Criar novos endpoints:**
   - `POST /api/auth/2fa/setup` — Gera secret e retorna QR code (usuário autenticado)
   - `POST /api/auth/2fa/verify-setup` — Valida código e ativa 2FA
   - `POST /api/auth/2fa/validate` — Valida código no fluxo de login
   - `DELETE /api/auth/2fa` — Desativa 2FA (opcional, apenas ADMIN)

5. **Alterar `AuthService.login()`:**
   - Se `twoFactorEnabled == true`, retornar response diferente com `requires2fa: true` e um token temporário curto (ex: 5 min de expiração, sem role claims)
   - Criar novo endpoint que recebe o token temporário + código TOTP e retorna o JWT final

6. **Criar DTOs:**
   - `TwoFactorSetupResponse` (qrCodeUri, secret)
   - `TwoFactorVerifyRequest` (code)
   - `TwoFactorLoginRequest` (tempToken, code)

7. **Alterar `LoginResponse`** para incluir campo opcional `requires2fa`:
   ```java
   private Boolean requires2fa; // true quando 2FA está ativo
   private String tempToken;    // token temporário para completar 2FA
   ```

#### Frontend

1. **Tela de configuração 2FA:**
   - Exibir QR Code para escanear
   - Input para digitar código de verificação
   - Botão para ativar/desativar

2. **Fluxo de login ajustado:**
   - Se response tiver `requires2fa: true`, exibir tela de código
   - Enviar código + tempToken para `/api/auth/2fa/validate`
   - Salvar JWT final normalmente

3. **Tela de perfil/configurações:**
   - Opção para ativar/desativar 2FA

#### Infraestrutura

- Nenhum serviço externo necessário (TOTP é offline)
- Migration Flyway para alteração no banco

#### Estimativa de esforço: **8-16 horas** (backend + frontend + testes)

---

## 4. Comparação e Recomendação

| Aspecto                 | CAPTCHA (reCAPTCHA v3)         | 2FA (TOTP)                     |
| ----------------------- | ------------------------------ | ------------------------------ |
| **Protege contra**      | Bots, brute force automatizado | Roubo de credenciais, phishing |
| **Impacto no UX**       | Nenhum (invisível)             | Médio (passo extra no login)   |
| **Complexidade**        | Baixa                          | Média-alta                     |
| **Alteração no banco**  | Nenhuma                        | 2 colunas na tabela `users`    |
| **Dependência externa** | Google reCAPTCHA API           | Nenhuma (lib local)            |
| **Estimativa**          | 2-4 horas                      | 8-16 horas                     |
| **Prioridade**          | **Alta** (risco imediato)      | Média (melhoria de segurança)  |

### Recomendação de implementação

1. **Curto prazo (imediato):** Implementar **reCAPTCHA v3** no login e registro — impacto baixo, proteção contra bots
2. **Médio prazo:** Implementar **2FA via TOTP** — obrigatório para ADMIN, opcional para TEACHER e STUDENT
3. **Complementar:** Adicionar **rate limiting** (ex: Spring Boot Bucket4j ou filtro customizado) para limitar tentativas de login por IP
