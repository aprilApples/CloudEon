#!/bin/bash

# Solr host and port
SOLR_HOST="localhost"
START_PORT="${SOLR_PORT:-8983}"
ZOOKEEPER_PORT="${ZOOKEEPER_PORT:-2181}"
METRICS_PORT="${SOLR_METRICS_PORT:-9854}"

# Health check endpoint
HEALTH_ENDPOINT="/solr/admin/info/system"

# Maximum number of retries
MAX_RETRIES=5

# Delay between retries in seconds
RETRY_DELAY=10

# Function to check Solr readiness
check_solr_readiness() {
  local retries=0
  local status_code

  sleep 10

  while [ $retries -lt $MAX_RETRIES ]; do
    status_code=$(curl -s -o /dev/null -w "%{http_code}" http://${SOLR_HOST}:"${START_PORT}"${HEALTH_ENDPOINT})
    status_code1=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8983/solr/admin/info/system)

    echo "Solr is ready yet. Status code: $ZOOKEEPER_CLUSTER_HOST hah ..."
    echo "Solr is ready yet. Status code: $status_code hah ..."
    echo "Solr is ready yet. Status code: $status_code1 hah..."
    if [ "$status_code" -eq 200 ]; then
      echo "Solr is ready."
      return 0
    else
      echo "Solr is not ready yet. Status code: $status_code. Retrying in$RETRY_DELAY seconds..."
      sleep $RETRY_DELAY
      (( retries++ ))
    fi
  done

  echo "Solr is not ready after $MAX_RETRIES attempts."
  return 1
}

# Execute the script to check if Solr has started successfully
if check_solr_readiness; then
  source $SOLR_HOME/solr_server_env.sh
  echo $ZOOKEEPER_CLUSTER_HOST
  nohup $SOLR_HOME/contrib/prometheus-exporter/bin/solr-exporter -p ${METRICS_PORT} -z ${ZOOKEEPER_CLUSTER_HOST} -f $SOLR_HOME/contrib/prometheus-exporter/conf/solr-exporter-config.xml -n 16 > /workspace/logs/solr-exporter.log 2>&1 &
#  nohup $SOLR_HOME/contrib/prometheus-exporter/bin/solr-exporter -p 9854 -b http://localhost:8983/solr -f $SOLR_HOME/contrib/prometheus-exporter/conf/solr-exporter-config.xml -n 8 > /workspace/logs/solr-exporter.log 2>&1 &
  echo "--------------------------------------------开启监控--------------------------------------------"
else
  echo "--------------------------------------------开启监控失败--------------------------------------------"
fi
