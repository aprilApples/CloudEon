#!/bin/bash

# Solr host and port
SOLR_HOST="localhost"
SOLR_PORT="${SOLR_PORT:-8983}"

# Health check endpoint
HEALTH_ENDPOINT="/solr/admin/info/system"

# Function to check Solr readiness
check_solr_readiness() {
  status_code=$(curl -s -o /dev/null -w "%{http_code}" http://${SOLR_HOST}:${SOLR_PORT}${HEALTH_ENDPOINT})

  if [ "$status_code" -eq 200 ]; then
    echo "Solr is ready."
    exit 0
  else
    echo "Solr is not ready yet. Status code: $status_code"
    exit 1
  fi
}

# 执行脚本判断是否启动成功
check_solr_readiness
  if [ $? -eq 0 ]; then
    exit 0
  fi
exit 1
