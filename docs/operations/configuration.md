# Configuração de execução e compilação

A operação configura o processo da API antes de iniciá-lo e o frontend antes de gerar os arquivos estáticos. Os padrões abaixo vêm do código. Arquivos do Compose e argumentos de compilação da imagem podem sobrescrevê-los. [Leitura da configuração em Go](../../internal/api/config.go) · [Entrada da API](../../cmd/sisges/main.go) · [Exemplo de ambiente](../../.env.example)

| Variável da API | Padrão ou requisito | Efeito operacional |
| --- | --- | --- |
| `PORT` | 8080 | O processo Go escuta nessa porta em todas as interfaces. |
| `DATABASE_URL` | Obrigatória quando a URL legada não é fornecida | String de conexão do pgx. Configure as credenciais do banco e a segurança do transporte para o ambiente. |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Alternativa legada | A aplicação remove o prefixo `jdbc:` e pode inserir as credenciais na URI do PostgreSQL. Uma `DATABASE_URL` explícita tem precedência. |
| `SECURITY_JWT_SECRET_KEY` | Pelo menos 32 bytes | Chave HMAC de assinatura. Todas as instâncias que aceitam as mesmas sessões precisam usar o mesmo valor. Após a rotação, tokens assinados com o valor anterior falham na próxima verificação. |
| `SECURITY_JWT_EXPIRATION_TIME` | 3600000 milissegundos | Validade do token de login. Mantenha a unidade explícita ao mudar o valor. |
| `SISGES_CORS_ALLOWED_ORIGIN_PATTERNS` | Localhost, loopback e subdomínios HTTPS da Vercel | Origens exatas ou padrões com curinga, separados por vírgula. O serviço devolve a origem permitida, autoriza credenciais e aceita Authorization e Content-Type. Defina explicitamente as origens da implantação. |
| `SISGES_MIGRATION_DIR` | `db/migration` | Diretório dos arquivos SQL relativo ao diretório de trabalho. Na imagem, o caminho é `/db/migration`. |
| `SISGES_SEED_ENABLED` | false | O valor `true`, independentemente de maiúsculas e minúsculas, habilita os dados de demonstração. Os ambientes de desenvolvimento versionados habilitam essa opção. |
| `SISGES_DB_MAX_CONNECTIONS` | 6, intervalo válido de 1 a 32 | Limite do pool por processo. Reserve conexões para todas as réplicas, manutenção do banco e recuperação. |
| `GOMEMLIMIT_BYTES` | 167772160 | Meta flexível de memória do Go em bytes. Reserve memória adicional para o contêiner. |
| `GOGC_PERCENT` | 75 | Percentual aplicado ao coletor de lixo do Go na inicialização. |
| `SISGES_R2_ENDPOINT` | Vazio | Endereço da conta, sem o caminho do bucket. O valor vazio desabilita o armazenamento de objetos. Para o serviço hospedado, use HTTPS. |
| `SISGES_R2_ACCESS_KEY_ID`, `SISGES_R2_SECRET_ACCESS_KEY` | Obrigatórias quando o endpoint está configurado | Credenciais de assinatura de requisições no servidor. Mantenha-as fora dos argumentos de compilação do frontend e do controle de versão. |
| `SISGES_R2_BUCKET` | `sisges-prd` | Único nome de bucket aceito com o armazenamento habilitado. Use contas isoladas ou dados controlados nos testes de armazenamento. |
| `SISGES_R2_REGION` | `auto` | Região usada para assinar as requisições. |

O armazenamento de objetos atende imagens de avisos, imagens de perfil e arquivos de aula. O arquivo de exemplo contém valores ilustrativos para o R2. Para trabalhar localmente sem esse serviço, deixe o endpoint vazio. O processo Go lê diretamente as variáveis do ambiente. O Compose interpola as variáveis selecionadas a partir do ambiente ou do arquivo correspondente. [Adaptador de armazenamento](../../internal/api/storage.go) · [Compose versionado](../../docker-compose.yml)

| Configuração do frontend | Onde se aplica | Efeito |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Processo de desenvolvimento do Vite ou argumento de compilação do Docker | Inclui o sufixo `/api`. O padrão no código do cliente é `http://localhost:8080/api`. A compilação de produção usa `/api` para chamadas HTTP de mesma origem. Alterar o ambiente de execução do contêiner depois da compilação não altera a URL incorporada. |
| Proxy e porta do Vite | Servidor de desenvolvimento | Porta 3000 por padrão. Solicitações a `/api` e `/ws` feitas pelo Vite são encaminhadas à API local. |
| Destino do Nginx | Rede dos contêineres | O nome em `sisges-backend:8080` deve ser resolvido dentro do contêiner do frontend. Configure a rede ou um alias correspondente. |

O hook do mural também deriva da URL da API o host para o WebSocket. Uma URL base absoluta em HTTPS produz um destino WSS. Na revisão analisada, a função não obtém o host quando a base é apenas `/api`. Verifique também se o cliente STOMP encaminha as mensagens do servidor à função associada à assinatura. O servidor dessa revisão omite o cabeçalho de assinatura. A consulta periódica por HTTP pode mascarar uma falha na entrega da notificação. [Configuração do cliente](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/vite.config.ts) · [Nginx](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/nginx.docker.conf) · [Hook do mural](https://github.com/SISGES/sisges-react-app/blob/b58d2dfa6eb06dba6e3485325f2087dc5538c2dc/src/hooks/useStompFeed.ts)
