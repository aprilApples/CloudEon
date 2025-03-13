#!/bin/bash
set -e

mkdir -p /conf
mkdir -p /workspace/logs

# Tips: 这里的 service-render-output/xxx 就是 service-render 目录下的 freemarker 模板文件编译出来的！
\cp -f /opt/service-render-output/limits.conf /etc/security/limits.conf
\cp -f /opt/service-render-output/solr.xml $SOLR_HOME/server/solr/solr.xml
\cp -f /opt/service-render-output/solr.in.sh $SOLR_HOME/bin/solr.in.sh
\cp -f /opt/service-render-output/solr_server_env.sh $SOLR_HOME

ln -s /workspace/logs server/logs

$SOLR_HOME/bin/solr start -force

source /opt/service-common/start-solr-exporter.sh


# Ranger Audit
setup_status=$?
# 检查第一个脚本是否执行成功
if [ $setup_status -eq 0 ]; then
  source /opt/service-common/start-ranger-audit.sh
else
  echo "setup.sh failed with status $setup_status."
  exit $setup_status
fi

sleep 5
until find /workspace/logs -mmin -1 -type f -name '*.log' ! -name '*gc*' | grep -q .
do
  echo "`date`: Waiting for logs..."
  sleep 2
done

find /workspace/logs -mmin -1 -type f -name '*.log' ! -name '*gc*' -exec tail -F {} +

echo "---------------------------------------------开始----------------------------------------------"

tail -f /dev/null