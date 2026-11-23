# Referência da API do SISGES

Esta referência descreve os contratos da API em Go na revisão `6b37a18` do backend. Cada operação usa o caminho completo no servidor da API (localmente, `http://localhost:8080`). Turmas e aulas têm identificadores próprios. `/api/classes` identifica turmas e `/api/class` identifica aulas em datas específicas. [Roteador](../../internal/api/app.go)

| Tarefa de integração | Referência |
| --- | --- |
| Entrar, gerenciar contas e encontrar professores e alunos | [Identidade e diretório](identity.md) |
| Organizar turmas, disciplinas e vínculos docentes | [Turmas e disciplinas](classes.md) |
| Salvar a grade horária, registrar uma aula ou marcar a frequência | [Aulas e frequência](lessons.md) |
| Criar atividades, liberar notas, encerrar um trimestre ou consultar o boletim | [Avaliação e ciclo letivo](assessment.md) |
| Publicar avisos, curtir, comentar, distribuir materiais e divulgar eventos | [Comunicação escolar](communications.md) |
| Transferir arquivos ou assinar atualizações do mural | [Arquivos e notificações do mural](files-and-feed.md) |

## Transporte e autenticação

Nas operações de escrita em JSON, envie um único objeto com `Content-Type: application/json`. O servidor limita o corpo da requisição a 1 MiB. A maioria das consultas retorna diretamente um objeto ou uma lista em JSON. Os IDs são inteiros e, no caminho da URL, devem ser positivos. As datas seguem `YYYY-MM-DD`. Campos com data e hora incluem o deslocamento de fuso horário ou `Z`. Os caminhos HTTP não incluem um segmento de versão. [Tratamento de requisições](../../internal/api/app.go)

Envie `Authorization: Bearer <accessToken>` nas chamadas HTTP protegidas. O servidor verifica a assinatura e a validade do token e confere no banco se a conta está ativa e se o e-mail e o perfil ainda correspondem aos do token. Faça login novamente quando o token expirar. As tabelas usam os valores literais dos perfis da API. **Qualquer perfil** significa qualquer usuário autenticado. Em ambientes remotos, use TLS e trate a rejeição da sessão antes de repetir uma ação do usuário. [Autenticação](../../internal/api/auth.go)

| Operação | Acesso | Entrada e resposta |
| --- | --- | --- |
| `GET /health` | Público | Sem corpo. Retorna `200 {"status":"ok"}` após verificar a conexão com o banco, com tempo limite de um segundo. Uma falha no banco retorna `500`. |

### Busca e paginação

As consultas de pessoas, turmas, aulas, disciplinas, materiais e atividades retornam listas com limites específicos por operação. Avisos e eventos retornam até 500 registros. Envie os filtros de busca de pessoas, turmas e aulas no corpo do POST. Ao atingir o limite, a lista pode estar incompleta. Essas rotas não oferecem número de página, deslocamento, cursor ou contagem total. Boletins, curtidas, comentários e lançamentos de notas têm estruturas próprias, descritas nas respectivas operações. [Roteador e operações](../../internal/api/)

### Erros

| Status HTTP | Código no corpo | Tratamento pelo cliente |
| --- | --- | --- |
| 200, apenas no login | `AUTH_INVALID_CREDENTIALS` | O `status` no corpo vale 401. Verifique se a resposta inclui `accessToken` antes de iniciar a sessão. |
| 400 | `VALIDATION_ERROR` | Consulte `errors[].field` e `errors[].message` para identificar o campo a corrigir. |
| 400 | `BUSINESS_RULE_VIOLATION` | Consulte `message`. Por exemplo, pode ser necessário recarregar uma grade horária alterada ou resolver pendências de liberação de notas. |
| 401 | `AUTH_UNAUTHORIZED` | A rota protegida rejeitou a sessão. |
| 403 | `AUTH_FORBIDDEN` | O perfil ou o vínculo com o recurso não permite o acesso. Algumas verificações de domínio retornam erro de regra de negócio com status 400. |
| 404 | `RESOURCE_NOT_FOUND` | A operação não encontrou o recurso solicitado. |
| 409 | `DATA_CONFLICT` | Ocorreu um conflito de dados ao cadastrar um usuário ou criar uma turma. |
| 500 | `INTERNAL_ERROR` | Falha no servidor. Verifique o estado do recurso antes de repetir uma operação de escrita. |

Os erros da aplicação incluem `status` numérico, `code` para tratamento programático, `message` para exibição e `timestamp` no formato RFC 3339. A validação em Go informa um campo por vez. O envio de arquivos também pode retornar `{"error":"…"}`. Para caminhos ou métodos sem correspondência, o roteador pode responder fora do formato JSON da aplicação. [Definições de erro](../../internal/api/errors.go) · [Login](../../internal/api/users.go) · [Arquivos](../../internal/api/storage.go)

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Erro de validação",
  "timestamp": "2026-01-15T10:00:00Z",
  "errors": [{"field": "body", "message": "JSON inválido"}]
}
```

### Testar uma consulta e uma falha de validação

No ambiente local, o primeiro comando retorna `200` e o objeto de saúde se o PostgreSQL estiver acessível. O segundo envia um JSON incompleto e retorna `400` com o erro do exemplo acima e o horário real da requisição. Os comandos não alteram os dados.

```sh
curl -i http://localhost:8080/health
curl -i -H 'Content-Type: application/json' \
  --data-binary '{' http://localhost:8080/api/auth/login
```

### Entrar e buscar uma turma

Inicie a API local e o banco com os dados de demonstração seguindo o [guia de desenvolvimento](../engineering/development.md). Execute o bloco em Bash com `curl` e `jq`. Ele usa a conta de administrador de demonstração, consulta o perfil e busca uma turma pelo nome. A busca usa POST apenas para consulta.

```bash
(
  set -euo pipefail
  api_url=http://localhost:8080/api
  export SISGES_DEMO_EMAIL=adm0001@sisges.com
  export SISGES_DEMO_PASSWORD=admin123

  token=$(
    jq -n '{email: env.SISGES_DEMO_EMAIL, password: env.SISGES_DEMO_PASSWORD}' |
      curl -fsS -H 'Content-Type: application/json' \
        --data-binary @- "$api_url/auth/login" |
      jq -er '.accessToken // error(.code // "accessToken ausente")'
  )

  curl -fsS -H "Authorization: Bearer $token" "$api_url/users/me" |
    jq '{register, role, email}'

  curl -fsS -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    --data-binary '{"name":"Turma 01"}' "$api_url/classes/search" |
    jq 'map({id, name, academicYear, studentCount, teacherCount})'
)
```

Em um banco recém-populado com os dados de demonstração, o perfil retorna a matrícula `ADM0001` e o papel `ADMIN`. A busca retorna uma lista com `Turma 01`, a série `6º ano`, o ID numérico da turma e as quantidades de alunos e professores. Uma lista vazia indica que nenhuma turma visível corresponde ao nome. Se o login retornar `AUTH_INVALID_CREDENTIALS`, verifique se os dados de demonstração foram carregados e se a conta mantém a senha inicial. Para consultar a grade horária ou registrar uma aula, use o ID da turma retornado pela busca e o perfil exigido pela operação. [Dados de demonstração](../../internal/api/seed.go) · [Exemplos de grade horária e aulas](lessons.md)

## Manutenção do contrato

Ao alterar uma rota, uma regra de acesso, um DTO, um status HTTP ou um filtro no banco, atualize a tabela do domínio e os exemplos afetados. Confira método e caminho em `App.routes`, incluindo a rota de notas atendida pelo curinga e a negociação de WebSocket. Verifique tanto o perfil exigido pelo roteador quanto os vínculos que a operação exige para acessar o recurso. [Roteador](../../internal/api/app.go) · [Modelo de referência da API](../templates/api-reference.md)
