# Deploy SISGES backend to Azure App Service
# Pré-requisitos: az login, Java 21, Maven

$ErrorActionPreference = "Stop"

# Configuração - ajuste conforme necessário
$RESOURCE_GROUP = if ($env:SISGES_AZURE_RG) { $env:SISGES_AZURE_RG } else { "sisges-rg" }
$LOCATION = if ($env:SISGES_AZURE_LOCATION) { $env:SISGES_AZURE_LOCATION } else { "brazilsouth" }
$APP_NAME = if ($env:SISGES_AZURE_APP) { $env:SISGES_AZURE_APP } else { "sisges-api" }
$APP_SERVICE_PLAN = if ($env:SISGES_AZURE_PLAN) { $env:SISGES_AZURE_PLAN } else { "sisges-plan" }

Write-Host "=== Build do JAR ===" -ForegroundColor Cyan
& .\mvnw.cmd package -DskipTests -q
$jarFiles = Get-ChildItem -Path target -Filter "*.jar" | Where-Object { $_.Name -notmatch "original" }
if (-not $jarFiles) {
    Write-Host "Erro: JAR não encontrado em target/" -ForegroundColor Red
    exit 1
}
$JAR_FILE = $jarFiles[0].FullName
Write-Host "JAR: $JAR_FILE"

Write-Host ""
Write-Host "=== Criando recursos Azure (se não existirem) ===" -ForegroundColor Cyan
az group create --name $RESOURCE_GROUP --location $LOCATION --output none 2>&1 | Out-Null
az appservice plan create --name $APP_SERVICE_PLAN --resource-group $RESOURCE_GROUP `
    --is-linux --sku B1 --output none 2>&1 | Out-Null
az webapp create --name $APP_NAME --resource-group $RESOURCE_GROUP `
    --plan $APP_SERVICE_PLAN --runtime "JAVA:21-java21" --output none 2>&1 | Out-Null

Write-Host ""
Write-Host "=== Configurando variáveis de ambiente ===" -ForegroundColor Cyan
$connStr = if ($env:AZURE_STORAGE_CONNECTION_STRING) { $env:AZURE_STORAGE_CONNECTION_STRING } else { "" }
$jwtSecret = if ($env:SECURITY_JWT_SECRET_KEY) { $env:SECURITY_JWT_SECRET_KEY } else { "altere-em-producao-min-32-chars" }

$null = az webapp config appsettings set --resource-group $RESOURCE_GROUP --name $APP_NAME --settings `
    "SPRING_PROFILES_ACTIVE=prod" `
    "SECURITY_JWT_SECRET_KEY=$jwtSecret" `
    "AZURE_STORAGE_CONNECTION_STRING=$connStr" `
    "AZURE_STORAGE_CONTAINER=sisgesfiles" `
    "WEBSITES_PORT=8080" `
    --output none 2>&1

if ($env:SPRING_DATASOURCE_URL) {
    $null = az webapp config appsettings set --resource-group $RESOURCE_GROUP --name $APP_NAME --settings `
        "SPRING_DATASOURCE_URL=$env:SPRING_DATASOURCE_URL" `
        "SPRING_DATASOURCE_USERNAME=$env:SPRING_DATASOURCE_USERNAME" `
        "SPRING_DATASOURCE_PASSWORD=$env:SPRING_DATASOURCE_PASSWORD" `
        --output none 2>&1
}

Write-Host ""
Write-Host "=== Deploy do JAR ===" -ForegroundColor Cyan
az webapp deploy --resource-group $RESOURCE_GROUP --name $APP_NAME `
    --src-path $JAR_FILE --type jar

Write-Host ""
Write-Host "=== Deploy concluído ===" -ForegroundColor Green
Write-Host "URL: https://${APP_NAME}.azurewebsites.net"
Write-Host "API: https://${APP_NAME}.azurewebsites.net/api"
