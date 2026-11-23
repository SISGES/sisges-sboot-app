# Publicação de imagens e implantação em produção

A branch `main` de cada repositório é a origem das imagens de produção. Ao receber commits, o GitHub Actions verifica a revisão e publica a imagem no Docker Hub com a tag `latest` e outra tag no formato `sha-<commit completo>`. A execução manual do fluxo de imagens também exige a referência `main`. Os fluxos que conectam ao servidor por SSH estão desativados. O operador do VPS é responsável por aplicar a publicação no servidor. [Fluxo da imagem do backend](../../.github/workflows/docker-publish.yml) · [Fluxo da imagem do frontend](https://github.com/SISGES/sisges-react-app/blob/main/.github/workflows/docker-publish.yml)

## Verificar a publicação no GitHub

1. Identifique os commits do backend e do frontend e confira o resultado do fluxo de imagem de cada revisão. O backend executa testes em PostgreSQL com detecção de condições de corrida e `go vet`. O frontend executa a suíte integrada usando o backend de `main` antes de publicar a imagem.
2. Registre as tags por commit e os digests exibidos pelo fluxo. As imagens são `<conta Docker Hub>/sisges-backend` e `<conta Docker Hub>/sisges-frontend`. Os segredos de publicação são `DOCKERHUB_USERNAME` e `DOCKERHUB_TOKEN`.
3. Confirme que o backend da nova versão mantém as operações usadas pelo frontend anterior. Identifique mudanças de contrato, migrações pendentes e requisitos do R2 antes de agendar a aplicação.

## Aplicar a publicação no servidor

Use o manifesto existente `~/sisges-deploy/docker-compose.hub.yml` e o arquivo `.env.prod`, sob responsabilidade do operador. Confira os nomes de serviço e o ambiente com `docker compose -f docker-compose.hub.yml --env-file .env.prod config --services`. Antes de atualizar, registre as imagens em execução, preserve os arquivos de configuração e tenha um ponto de recuperação verificado do PostgreSQL e dos objetos referenciados. [Incidentes e restauração](recovery.md)

Quando o manifesto usa tags por commit, selecione as revisões aprovadas. Quando usa `latest`, confira no registro os digests vigentes e verifique o digest baixado para cada serviço. Na janela aprovada, execute no servidor a partir do diretório do manifesto.

```sh
docker compose -f docker-compose.hub.yml --env-file .env.prod pull backend
docker compose -f docker-compose.hub.yml --env-file .env.prod up -d backend
docker compose -f docker-compose.hub.yml --env-file .env.prod logs --tail=100 backend
```

A API aplica as migrações antes de começar a escutar. Verifique `GET /health` no endereço do backend e confira a resposta JSON `{"status":"ok"}`. A rota do frontend pode devolver o HTML da aplicação e ocultar um erro no destino da verificação. Se a atualização do backend falhar, mantenha o frontend anterior e examine a versão da migração e os logs. [Inicialização](../../internal/api/app.go) · [Executor de migrações](../../internal/api/migrate.go)

Com a API saudável, aplique o frontend.

```sh
docker compose -f docker-compose.hub.yml --env-file .env.prod pull frontend
docker compose -f docker-compose.hub.yml --env-file .env.prod up -d frontend
docker compose -f docker-compose.hub.yml --env-file .env.prod ps
```

No endereço usado pela escola, confira login, recarregamento da página, a grade de uma turma de teste, o mural e um boletim de teste. Se o serviço usa anexos, valide um envio e uma leitura autorizada. Registre os digests em execução e as verificações concluídas.

## Recuperar uma publicação com falha

Uma imagem anterior pode ser reaplicada após verificar sua compatibilidade com as migrações e com os caminhos de arquivos gravados desde o início da atualização. Quando a recuperação exige restaurar dados, preserve a base afetada, ensaie em destino separado e combine com a escola o tratamento dos lançamentos feitos depois do ponto de recuperação. [Procedimento de restauração](recovery.md)

## Imagens de avisos em instalações legadas

Uma instalação que migra do MinIO pode manter caminhos iniciados por `/api/files/announcements/`. Na janela de migração, copie os objetos de `announcements/` do armazenamento antigo para `post-img/` no R2, preservando os sufixos. Compare as quantidades e verifique amostras antes de trocar o prefixo nas linhas correspondentes de `sisges.announcement.image_path`. Teste a leitura autenticada pela API antes de descartar a cópia original. [Armazenamento](../../internal/api/storage.go)
