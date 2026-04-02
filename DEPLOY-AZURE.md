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
- `SISGES_AZURE_APP` – Nome do app (padrão: sisges-backend-fubpacfwgjh5f8cg)
- `SISGES_AZURE_PLAN` – App Service Plan (padrão: sisges-plan)

## URL após deploy (produção)

- **App**: https://sisges-backend-fubpacfwgjh5f8cg.brazilsouth-01.azurewebsites.net
- **API**: https://sisges-backend-fubpacfwgjh5f8cg.brazilsouth-01.azurewebsites.net/api
- **Swagger**: https://sisges-backend-fubpacfwgjh5f8cg.brazilsouth-01.azurewebsites.net/swagger-ui.html

## Deploy manual (se o script falhar)

**1. Aumentar timeout de startup (obrigatório se o app demorar a subir):**
```powershell
az webapp config appsettings set --resource-group sisges-rg --name sisges-backend-fubpacfwgjh5f8cg --settings "WEBSITES_CONTAINER_START_TIME_LIMIT=600" --output none
```

**2. Build e deploy:**
```powershell
cd sisges-sboot-app
.\mvnw.cmd package -DskipTests -q
$jar = (Get-ChildItem target -Filter "*.jar" | Where-Object { $_.Name -notmatch "original" })[0].FullName
az webapp deploy --resource-group sisges-rg --name sisges-backend-fubpacfwgjh5f8cg --src-path $jar --type jar
```

**Importante:** No portal Azure, confira o app **sisges-backend-fubpacfwgjh5f8cg** (não sisges-api) para ver o histórico de deploy.

## CORS

Se o frontend estiver em outro domínio, configure CORS no Azure ou no `CorsConfiguration` do backend.
