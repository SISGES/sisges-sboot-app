# Estratégia de testes e evidências para a entrega

Use esta matriz ao alterar um fluxo escolar ou preparar uma entrega conjunta do cliente e do servidor. Os comandos correspondem às versões verificadas na branch `main` dos dois repositórios. Um teste de navegador com respostas simuladas verifica como o cliente trata aqueles dados. Execute também o fluxo integrado para verificar a compatibilidade com o servidor.

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

## Frontend e integração completa

Na raiz do frontend, com o backend no diretório irmão `backend`, execute os comandos abaixo.

```sh
npm ci
npm run test:all
```

A suíte integrada requer Node 22, Go 1.24 ou superior com compilador C para o detector de condições de corrida, PostgreSQL 16 com ferramentas de servidor e cliente, Bash, curl e as dependências de sistema do Cypress. Defina `PG_BINDIR` se o PostgreSQL não estiver no caminho padrão. Use `SISGES_BACKEND_DIR` para indicar outro checkout. O script cria um cluster temporário isolado, inicia a API Go com dados fictícios, compila e serve o cliente, executa a suíte e encerra os processos de teste.

Execute `npm test` para as regras de data e sessão, `npm run lint` para a análise estática e `npm run build` para a compilação. Com o cliente ativo, use `npm run cypress:run:mock` para o navegador com respostas simuladas ou `npm run cypress:run:integration` para os fluxos com a API real. Para estes últimos, use dados descartáveis e configure a API do cliente, `CYPRESS_API_BASE_URL`, a origem `CYPRESS_BASE_URL` e credenciais locais de administrador, professor e aluno. A suíte integrada configura esses valores automaticamente.

O fluxo de imagem do frontend executa a suíte integrada com o backend de `main` antes de publicar. A validação de pull requests para `main` executa testes unitários, análise estática, compilação e os casos Cypress com respostas simuladas. [Comandos do projeto](https://github.com/SISGES/sisges-react-app/blob/main/package.json) · [Script integrado](https://github.com/SISGES/sisges-react-app/blob/main/scripts/test-all.sh) · [Esteira do frontend](https://github.com/SISGES/sisges-react-app/blob/main/.github/workflows/tests.yml)

## Registrar o resultado

Registre as revisões do cliente e do servidor, a última migração aplicada, os perfis e o escopo dos dados de teste, as verificações executadas, as contagens de aprovações, falhas e testes pulados e o que ainda exige validação em produção. Para mudanças acadêmicas, compare os registros esperados e observados. Para implantações, inclua os identificadores das imagens por conteúdo, a resposta da verificação de saúde e os resultados dos testes básicos da aplicação. Repita as verificações afetadas por mudanças nas rotas, no esquema, nas regras de arquivos ou da escola ou nas ferramentas do cliente.
