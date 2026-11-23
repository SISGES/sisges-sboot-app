# Procedimento de publicação e implantação

Use este procedimento quando mudar o backend ou o frontend. Os fluxos versionados descrevem uma implantação no EC2 com imagens do Docker Hub. Obtenha com o responsável pela operação o manifesto Compose do servidor, os segredos pelos canais apropriados, os digests das imagens e a configuração real da infraestrutura. Para identificar a versão em execução, é preciso consultar o ambiente implantado. [Implantação do backend](../../.github/workflows/deploy.yml) · [Implantação do frontend](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/.github/workflows/deploy.yml)

## Acionamento e artefatos

| Push ou ação | Backend | Frontend |
| --- | --- | --- |
| Push em uma branch de funcionalidade | Os fluxos de push versionados não são acionados. | Os fluxos de push versionados não são acionados. |
| Push em `dev` | O fluxo de imagem executa testes com PostgreSQL e a análise com vet, depois publica a tag `dev` no Docker Hub. | Compila e publica o frontend com a tag `dev`. |
| Push em `main`, inclusive quando só a documentação muda | Tanto o fluxo de imagem quanto o de implantação executam testes e publicam `latest`. O fluxo de implantação se conecta ao EC2 e recria o serviço de backend. | Ambos os fluxos publicam `latest`. O de implantação se conecta ao EC2 e recria o serviço de frontend. |
| Execução manual do fluxo de imagem | Publica `latest` para `main` e `dev` para as demais referências selecionadas. | Segue a mesma regra de seleção da tag. |

As imagens são `<Docker Hub user>/sisges-backend` e `<Docker Hub user>/sisges-frontend`. Os dois fluxos de cada repositório que rodam na branch principal podem gravar a mesma tag mutável. Registre na publicação o digest efetivamente baixado para cada serviço. Os fluxos referenciam os segredos de repositório `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `EC2_USER` e `EC2_SSH_KEY`. [Fluxo da imagem do backend](../../.github/workflows/docker-publish.yml) · [Fluxo da imagem do frontend](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/.github/workflows/docker-publish.yml)

## Sequência de publicação

1. Identifique as revisões do backend e do frontend, as operações da API afetadas e as novas migrações necessárias. Registre os digests atuais e o ponto de recuperação. Para migrações que alteram referências a arquivos, verifique se os backups incluem o PostgreSQL e os objetos correspondentes do R2. [Procedimento de recuperação](recovery.md)
2. Execute as suítes de teste pertinentes e compare as alterações com o comportamento documentado. Se a mudança envolver os dois serviços, publique primeiro um backend compatível com o cliente anterior e valide esse cliente antes de atualizar o frontend. [Estratégia de testes](../engineering/test-strategy.md)
3. Confirme que o servidor dispõe de Docker Compose, do manifesto `~/sisges-deploy/docker-compose.hub.yml` e de `.env.prod`. Na configuração mantida pela operação, confira o serviço pretendido, o banco de destino, o alias de rede, a terminação TLS e a opção de carga de demonstração. Mantenha valores secretos fora do registro de publicação.
4. A ação de implantação baixa a imagem e recria o serviço selecionado. Ao iniciar, a API se conecta ao PostgreSQL, aplica os arquivos SQL pendentes, insere os dados de demonstração se habilitados e só então começa a aceitar conexões. Com várias réplicas, execute uma única migração por vez. Uma falha de migração impede o novo processo de atender requisições. [Inicialização](../../internal/api/app.go) · [Executor de migrações](../../internal/api/migrate.go)
5. Inspecione o estado do serviço e consulte `GET /health` na **origem da API**. O Nginx da imagem do frontend encaminha `/api/` e `/ws` e atende os demais caminhos com a SPA. Uma resposta HTML 200 na raiz do frontend ou em `/health` no host do frontend não verifica o banco.
6. A partir da origem que os usuários abrem no navegador, verifique o login, o perfil da sessão e a consulta de uma turma representativa. Para mudanças no armazenamento, envie e recupere um arquivo de teste permitido no ambiente aprovado. Para mudanças no mural, confirme a assinatura e a atualização em um segundo cliente. Compare o digest no servidor com o registro da publicação.

## Decisão de recuperação

Se o novo contêiner falhar antes de atender, preserve os logs, a versão da migração com erro, o digest anterior e as versões de esquema aplicadas. Antes de reiniciar a imagem anterior, confirme sua compatibilidade com o esquema real e com todos os prefixos de arquivos em uso. Se as alterações de dados impedirem a reversão apenas pelo código, ensaie uma restauração em destino separado ou prepare uma nova migração corretiva revisada. Os fluxos não implementam reversão automática, backup ou testes básicos de validação após a implantação. [Procedimento de incidentes e restauração](recovery.md)

## Imagens de avisos em instalações legadas

Uma instalação que está migrando do MinIO pode manter no banco caminhos iniciados por `/api/files/announcements/`. Em uma janela de migração controlada, copie os objetos de `announcements/` do bucket antigo para `post-img/` no R2, preservando os sufixos. Confira as quantidades e leia uma amostra dos arquivos antes de atualizar as linhas correspondentes em `sisges.announcement.image_path`. Substitua apenas o prefixo antigo por `/api/files/post-img/`. Confirme a leitura autenticada pela API antes de desativar o bucket legado ou descartar sua cópia. A rota atual de download reconhece `post-img/`, `profile-img/` e `lesson-files/`. [Implementação do armazenamento](../../internal/api/storage.go)
