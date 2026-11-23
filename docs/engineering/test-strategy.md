# Estratégia de testes e evidências para a entrega

Use esta matriz ao alterar um fluxo escolar ou preparar uma entrega conjunta do cliente e do servidor. Os comandos e a cobertura correspondem ao backend `6b37a18` e ao frontend `b58d2df`. Um teste de navegador com respostas simuladas verifica como o cliente trata aqueles dados. Execute também o fluxo integrado para verificar a compatibilidade com o servidor.

| Risco | Verificações existentes | Evidência adicional ao alterar esse fluxo |
| --- | --- | --- |
| Sessão e acesso a dados acadêmicos | Testes de token em Go, testes de login e mudança de senha com banco e testes de autenticação e de proteção por perfil no Cypress | Efetue login e recarregue a página usando a validação real da API. Compare as leituras permitidas e negadas para diferentes perfis e vínculos. |
| Grade semanal e aulas registradas | Validação em Go, fluxo de grade e aula com PostgreSQL, transferências simuladas de objetos e testes de grade no Cypress | Verifique a recuperação após tentar salvar uma versão desatualizada, a preservação da aula após mudar a grade e o acesso aos arquivos por contas autorizadas. |
| Avaliações e boletins | Testes Cypress de lançamento de notas, configuração da avaliação e boletim | Crie uma atividade real com pontuação, lance uma nota, libere-a e consulte o boletim do aluno. Teste um segundo ano letivo com os resultados esperados pela escola. |
| Avisos, eventos e materiais | Testes de navegador, um fluxo real de publicação e leitura de aviso e testes em Go e Node do intervalo permitido para eventos | Confira em um segundo navegador o público do evento e a atualização do mural. No envio de arquivos, verifique a operação HTTP e a gravação no armazenamento de objetos. |
| Persistência e migrações | Testes de integração com PostgreSQL cobrem a inicialização de um banco novo, a compatibilidade com o histórico do Flyway e o uso do esquema da grade | Aplique a migração candidata a uma cópia restaurada e representativa. Confira os registros e teste clientes antigos durante a implantação gradual. |

[Testes em Go](../../internal/api/) · [Testes de navegador](https://github.com/SISGES/sisges-react-app/tree/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/cypress/e2e) · [Requisitos funcionais](../product/prd.md)

## Backend

Na raiz do backend, com as ferramentas de Go instaladas, execute os comandos abaixo. Para cobrir a integração com o banco, defina antes `SISGES_TEST_DATABASE_URL` apontando para um **banco PostgreSQL dedicado e descartável**. Os testes criam dados escolares fictícios e alteram o histórico de migrações. Sem essa variável, os testes que dependem do banco são pulados.

```sh
go test -race -count=1 ./...
go vet ./...
```

Para executar apenas os dois fluxos com SQL, use `go test -v -run 'TestPostgresMigrationAndAuthFlow|TestScheduleAndDailyRecordsPostgres' ./internal/api` e confirme que ambos foram executados. As esteiras de publicação e implantação do backend executam os testes com detecção de condições de corrida e o `go vet`, com um serviço PostgreSQL 16 disponível. [Teste de integração](../../internal/api/integration_test.go) · [Casos da grade horária](../../internal/api/schedules_test.go) · [Integração contínua do backend](../../.github/workflows/docker-publish.yml)

## Frontend

Na raiz do frontend, instale as dependências nas versões fixadas e execute separadamente a compilação, os testes de datas e a análise estática.

```sh
npm ci
npm run build
node --test tests/eventDateTime.test.mjs
npm run lint
```

A compilação verifica os tipos e gera a aplicação de página única. A análise estática exige zero avisos. As esteiras de imagens Docker compilam o frontend. O Cypress e os testes isolados de datas são verificações separadas. [Comandos do projeto](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/package.json) · [Testes de datas](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/tests/eventDateTime.test.mjs) · [Compilação da imagem](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/Dockerfile)

Inicie o cliente antes do Cypress. No ambiente local deste manual, ele atende na porta 3001. A configuração padrão do Cypress usa a porta 3000. O Cypress precisa do binário compatível com a versão instalada e das dependências da plataforma.

```sh
CYPRESS_BASE_URL=http://localhost:3001 npm run cypress:run:mock
CYPRESS_BASE_URL=http://localhost:3001 npm run cypress:run:mock -- --spec cypress/e2e/lesson-schedule.cy.ts
```

Para o teste com a API real, use uma instância isolada com dados de demonstração. Configure a mesma URL base usada na compilação do cliente e forneça as credenciais locais de um professor. As credenciais opcionais de administrador permitem excluir o aviso criado pelo teste. Nesta revisão, os usuários antigos da V16 mencionados em um comentário do teste não são uma base confiável. Use contas ativas da carga de desenvolvimento e mantenha credenciais locais de teste fora do Git.

```sh
CYPRESS_BASE_URL=http://localhost:3001 \
CYPRESS_API_BASE_URL=http://localhost:8080/api \
CYPRESS_TEACHER_EMAIL=ana.clara.ferreira@sisges.local CYPRESS_TEACHER_PASSWORD=admin123 \
CYPRESS_ADMIN_EMAIL=adm0001@sisges.com CYPRESS_ADMIN_PASSWORD=admin123 \
npm run cypress:run:integration
```

O teste integrado cria um aviso de texto pela interface e o lê no mural real. Casos com dados simulados podem reproduzir comportamentos diferentes dos oferecidos pela API versionada. Quando o teste simulado passar e o integrado falhar, registre a requisição e a resposta, removendo campos sensíveis. [Configuração do Cypress](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/cypress.config.ts) · [Teste integrado de avisos](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/cypress/e2e/announcement-create-api.cy.ts)

## Registrar o resultado

Registre as revisões do cliente e do servidor, a última migração aplicada, os perfis e o escopo dos dados de teste, as verificações executadas, as contagens de aprovações, falhas e testes pulados e o que ainda exige validação em produção. Para mudanças acadêmicas, compare os registros esperados e observados. Para implantações, inclua os identificadores das imagens por conteúdo, a resposta da verificação de saúde e os resultados dos testes básicos da aplicação. Repita as verificações afetadas por mudanças nas rotas, no esquema, nas regras de arquivos ou da escola ou nas ferramentas do cliente.
