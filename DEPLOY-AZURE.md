# Deploy do SISGES Backend no Azure

## Pré-requisitos

1. **Azure CLI** instalado e logado:
   ```bash
   az login
   ```

2. **Java 21** e **Maven** (ou use o Maven Wrapper incluído)

## Deploy rápido (Windows)

```powershell
cd sisges-sboot-app

# Defina as variáveis (obrigatório em produção)
$env:SECURITY_JWT_SECRET_KEY = "sua-chave-secreta-min-32-caracteres"
$env:AZURE_STORAGE_CONNECTION_STRING = "sua-connection-string-azure-blob"

# Opcional: usar banco Azure em vez do Railway
# $env:SPRING_DATASOURCE_URL = "jdbc:postgresql://..."
# $env:SPRING_DATASOURCE_USERNAME = "..."
# $env:SPRING_DATASOURCE_PASSWORD = "..."

.\deploy-azure.ps1
```

## Deploy (Linux/Mac)

```bash
cd sisges-sboot-app

export SECURITY_JWT_SECRET_KEY="sua-chave-secreta-min-32-caracteres"
export AZURE_STORAGE_CONNECTION_STRING="sua-connection-string-azure-blob"

chmod +x deploy-azure.sh
./deploy-azure.sh
```

## Variáveis de ambiente

| Variável | Obrigatório | Descrição |
|----------|-------------|-----------|
| `SECURITY_JWT_SECRET_KEY` | Sim (prd) | Chave JWT, mínimo 32 caracteres |
| `AZURE_STORAGE_CONNECTION_STRING` | Sim (prd) | Connection string do Blob Storage |
| `AZURE_STORAGE_CONTAINER` | Não | Nome do container (padrão: sisgesfiles) |
| `SPRING_DATASOURCE_URL` | Não | URL do PostgreSQL (padrão: Railway) |
| `SPRING_DATASOURCE_USERNAME` | Não | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Não | Senha do banco |

## Customização

Defina antes de rodar o script:

- `SISGES_AZURE_RG` – Resource group (padrão: sisges-rg)
- `SISGES_AZURE_LOCATION` – Região (padrão: brazilsouth)
- `SISGES_AZURE_APP` – Nome do app (padrão: sisges-api)
- `SISGES_AZURE_PLAN` – App Service Plan (padrão: sisges-plan)

## URL após deploy

- **App**: https://sisges-api.azurewebsites.net
- **API**: https://sisges-api.azurewebsites.net/api
- **Swagger**: https://sisges-api.azurewebsites.net/swagger-ui.html

## CORS

Se o frontend estiver em outro domínio, configure CORS no Azure ou no `CorsConfiguration` do backend.
