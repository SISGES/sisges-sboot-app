#!/bin/bash
# Deploy SISGES backend to Azure App Service
# Pré-requisitos: az login, Java 21, Maven

set -e

# Configuração - ajuste conforme necessário
RESOURCE_GROUP="${SISGES_AZURE_RG:-sisges-rg}"
LOCATION="${SISGES_AZURE_LOCATION:-brazilsouth}"
APP_NAME="${SISGES_AZURE_APP:-sisges-api}"
APP_SERVICE_PLAN="${SISGES_AZURE_PLAN:-sisges-plan}"

echo "=== Build do JAR ==="
./mvnw package -DskipTests -q 2>/dev/null || ./mvnw.cmd package -DskipTests -q
JAR_FILE=$(ls target/*.jar | grep -v original | head -1)
if [ -z "$JAR_FILE" ]; then
  echo "Erro: JAR não encontrado em target/"
  exit 1
fi
echo "JAR: $JAR_FILE"

echo ""
echo "=== Criando recursos Azure (se não existirem) ==="
az group create --name "$RESOURCE_GROUP" --location "$LOCATION" --output none 2>/dev/null || true
az appservice plan create --name "$APP_SERVICE_PLAN" --resource-group "$RESOURCE_GROUP" \
  --is-linux --sku B1 --output none 2>/dev/null || true
az webapp create --name "$APP_NAME" --resource-group "$RESOURCE_GROUP" \
  --plan "$APP_SERVICE_PLAN" --runtime "JAVA:21-java21" --output none 2>/dev/null || true

echo ""
echo "=== Configurando variáveis de ambiente ==="
# Defina estas variáveis antes de rodar o script, ou edite abaixo
az webapp config appsettings set --resource-group "$RESOURCE_GROUP" --name "$APP_NAME" --settings \
  "SPRING_PROFILES_ACTIVE=prod" \
  "SECURITY_JWT_SECRET_KEY=${SECURITY_JWT_SECRET_KEY:-altere-em-producao-min-32-chars}" \
  "AZURE_STORAGE_CONNECTION_STRING=${AZURE_STORAGE_CONNECTION_STRING}" \
  "AZURE_STORAGE_CONTAINER=${AZURE_STORAGE_CONTAINER:-sisgesfiles}" \
  "WEBSITES_PORT=8080" \
  --output none

# Database - use variáveis ou mantenha Railway
if [ -n "$SPRING_DATASOURCE_URL" ]; then
  az webapp config appsettings set --resource-group "$RESOURCE_GROUP" --name "$APP_NAME" --settings \
    "SPRING_DATASOURCE_URL=$SPRING_DATASOURCE_URL" \
    "SPRING_DATASOURCE_USERNAME=$SPRING_DATASOURCE_USERNAME" \
    "SPRING_DATASOURCE_PASSWORD=$SPRING_DATASOURCE_PASSWORD" \
    --output none
fi

echo ""
echo "=== Deploy do JAR ==="
az webapp deploy --resource-group "$RESOURCE_GROUP" --name "$APP_NAME" \
  --src-path "$JAR_FILE" --type jar

echo ""
echo "=== Deploy concluído ==="
echo "URL: https://${APP_NAME}.azurewebsites.net"
echo "API: https://${APP_NAME}.azurewebsites.net/api"
