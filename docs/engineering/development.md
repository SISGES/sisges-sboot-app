# Como executar o SISGES localmente

Os dois repositórios têm compilações independentes. Para desenvolver com dados escolares descartáveis, inicie o PostgreSQL e a API Go com o arquivo Compose do backend e execute o cliente React com o Vite. É preciso ter Git, Docker com Compose e Node com npm. O Dockerfile do frontend usa Node 20. Para executar o cliente diretamente na máquina, use uma versão do Node compatível com as dependências fixadas no projeto. [Compose do backend](../../docker-compose.yml) · [Compilação do frontend](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/Dockerfile)

## Iniciar uma sessão de desenvolvimento

1. Em um diretório vazio, clone os repositórios lado a lado com os nomes esperados pelo Compose.

   ```sh
   git clone https://github.com/SISGES/sisges-sboot-app.git backend
   git clone https://github.com/SISGES/sisges-react-app.git frontend
   ```

2. No diretório que contém os dois repositórios, inicie a API e o banco. O Compose do backend carrega os dados de demonstração, publica a API na porta 8080 e mantém o PostgreSQL na rede dos contêineres.

   ```sh
   docker compose -f backend/docker-compose.yml up -d --build backend
   docker compose -f backend/docker-compose.yml logs --tail=50 backend
   curl --fail http://localhost:8080/health
   ```

   Na inicialização, a API aplica as migrações. Quando estiver pronta, a verificação de saúde retornará `{"status":"ok"}`. Se ela não iniciar, consulte os logs do contêiner da API para identificar erros de configuração, banco ou migração. Com o endereço do R2 vazio, os fluxos que dispensam anexos continuam disponíveis.

3. Em outro terminal, abra o diretório do frontend e execute o cliente na origem permitida pelo Compose do backend.

   ```sh
   npm ci
   VITE_API_BASE_URL=http://localhost:8080/api npm run dev -- --port 3001 --strictPort
   ```

   Acesse `http://localhost:3001`. A conta de administração da demonstração é `adm0001@sisges.com`, com a senha `admin123`. A carga inicial também cria endereços de exemplo para professores e alunos. Essas contas e a chave de assinatura do Compose destinam-se ao ambiente local. A porta padrão do Vite é 3000. Para usá-la em chamadas diretas entre origens, ajuste a configuração de CORS da API. [Carga inicial](../../internal/api/seed.go) · [Configuração do Vite](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/vite.config.ts)

4. Interrompa o Vite com Ctrl+C. No diretório que contém os repositórios, encerre os contêineres com `docker compose -f backend/docker-compose.yml down`. O volume nomeado preserva os dados entre sessões. Para começar com dados novos, libere as portas e inicie outro projeto Compose com um nome diferente e um volume vazio.

## Executar o serviço Go diretamente na máquina

Use Go 1.24 ou superior e um banco PostgreSQL dedicado e acessível pela máquina. O usuário do banco precisa de permissão para criar o esquema `sisges`, as tabelas e os índices. Gere uma chave de assinatura local e execute os comandos na raiz do backend para que o serviço encontre o diretório de migrações.

```sh
export DATABASE_URL='postgresql://localhost/sisges_dev?sslmode=disable'
export SECURITY_JWT_SECRET_KEY="$(openssl rand -hex 32)"
export SISGES_SEED_ENABLED=true
go run ./cmd/sisges
```

Forneça as credenciais pela configuração local do PostgreSQL ou por variáveis de ambiente. O Compose do backend mantém a porta do banco restrita à rede dos contêineres. O ambiente original com os dois repositórios também contém um `compose.yaml` no diretório pai que publica a porta 5432. Para clones independentes, use o arquivo Compose versionado no backend conforme o primeiro procedimento. [Referência de configuração](../operations/configuration.md)

## Diagnóstico de problemas de integração

| Sintoma | O que verificar |
| --- | --- |
| O navegador bloqueia chamadas à API entre origens | Confira se protocolo, nome do host e porta correspondem exatamente a uma origem permitida. Se alterar a URL usada na compilação, gere uma nova imagem do cliente. |
| A verificação de saúde passa, mas o envio de arquivo falha | A verificação consulta o banco. O envio também exige um endereço de conta R2 configurado, credenciais de assinatura, espaço temporário com permissão de escrita, destino permitido e tipo de arquivo aceito. |

[Guia do cliente da API](frontend.md) · [Diagnóstico e recuperação](../operations/recovery.md)
