#!/bin/bash

NACOS_HOST="localhost"
NACOS_PORT="8848"
NACOS_ADDR="http://${NACOS_HOST}:${NACOS_PORT}"
NAMESPACE="alogrithm-cloud"
GROUP="DEFAULT_GROUP"
USERNAME="nacos"
PASSWORD="your_nacos_password"

echo "=============================================="
echo "   Nacos Configuration Importer"
echo "=============================================="
echo ""
echo "Target: ${NACOS_ADDR}"
echo "Namespace: ${NAMESPACE}"
echo ""

# 1. Login
echo "[1/4] Authenticating..."
LOGIN_RESP=$(curl -s -X POST "${NACOS_ADDR}/nacos/v1/auth/login" -d "username=${USERNAME}&password=${PASSWORD}")
ACCESS_TOKEN=$(echo "${LOGIN_RESP}" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "Error: Authentication failed!"
    echo "Response: ${LOGIN_RESP}"
    exit 1
fi
echo "Success!"

# 2. Check/Create Namespace
echo ""
echo "[2/4] Verifying namespace..."
NAMESPACE_CHECK=$(curl -s -X GET "${NACOS_ADDR}/nacos/v1/console/namespaces" -H "Authorization: Bearer ${ACCESS_TOKEN}")

if echo "${NAMESPACE_CHECK}" | grep -q "\"namespace\":\"${NAMESPACE}\""; then
    echo "Namespace '${NAMESPACE}' already exists"
else
    echo "Creating namespace '${NAMESPACE}'..."
    curl -s -X POST "${NACOS_ADDR}/nacos/v1/console/namespaces" \
        -d "customNamespaceId=${NAMESPACE}" \
        -d "namespaceName=Alogrithm Cloud" \
        -d "namespaceDesc=Alogrithm Cloud Configuration Namespace" \
        -d "accessToken=${ACCESS_TOKEN}" > /dev/null
    echo "Namespace created"
fi

# 3. Scan Files
echo ""
echo "[3/4] Scanning configuration files..."
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

CONFIG_FILES=()
for file in "${SCRIPT_DIR}"/*; do
    if [ -f "$file" ]; then
        filename=$(basename "$file")
        extension="${filename##*.}"
        if [[ "$extension" == "yml" || "$extension" == "yaml" || "$extension" == "properties" ]]; then
            CONFIG_FILES+=("$filename")
        fi
    fi
done

TOTAL_FILES=${#CONFIG_FILES[@]}
echo "Found ${TOTAL_FILES} files"

# 4. Import
echo ""
echo "[4/4] Importing configurations..."
SUCCESS=0
FAILED=0

for filename in "${CONFIG_FILES[@]}"; do
    file="${SCRIPT_DIR}/${filename}"
    extension="${filename##*.}"
    
    if [[ "$extension" == "properties" ]]; then
        file_type="properties"
    else
        file_type="yaml"
    fi
    
    echo -n "  ${filename} ... "
    
    response=$(curl -s -X POST "${NACOS_ADDR}/nacos/v1/cs/configs" \
        --data-urlencode "dataId=${filename}" \
        --data-urlencode "group=${GROUP}" \
        --data-urlencode "content=$(cat "${file}")" \
        --data-urlencode "type=${file_type}" \
        --data-urlencode "tenant=${NAMESPACE}" \
        --data-urlencode "accessToken=${ACCESS_TOKEN}")
    
    if [[ "$response" == "true" ]]; then
        echo "OK"
        ((SUCCESS++))
    else
        echo "FAILED"
        ((FAILED++))
    fi
done

echo ""
echo "=============================================="
echo "   Summary"
echo "=============================================="
echo "  Total:   ${TOTAL_FILES}"
echo "  Success: ${SUCCESS}"
echo "  Failed:  ${FAILED}"
echo ""
echo "  Console: ${NACOS_ADDR}/nacos"
echo "=============================================="

if [ $FAILED -eq 0 ]; then
    echo ""
    echo "Usage:"
    echo "  Local:  java -jar app.jar --spring.profiles.active=default"
    echo "  Prod:   java -jar app.jar --spring.profiles.active=prod"
fi
