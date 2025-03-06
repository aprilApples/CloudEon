#!/usr/bin/env bash
set -xeuo pipefail

rm -rf logs
mkdir -p /workspace/logs
ln -s /workspace/logs $RANGER_HOME/admin/ews/logs

cd $RANGER_HOME/admin/
rm -f $RANGER_HOME/admin/install.properties
rm -f $RANGER_HOME/admin/setup.sh
\cp -f /opt/service-render-output/install.properties $RANGER_HOME/admin/install.properties
\cp -f /opt/service-render-output/setup.sh $RANGER_HOME/admin/setup.sh
\cp -f /opt/service-render-output/ranger-admin-services.sh $RANGER_HOME/admin/ews/ranger-admin-services.sh
\cp -f /opt/service-render-output/jmx_prometheus.yaml $RANGER_HOME/admin/jmx_prometheus.yaml
source $RANGER_HOME/admin/setup.sh
$RANGER_HOME/admin/ews/ranger-admin-services.sh start


\cp -f /opt/service-render-output/ranger-usersync.sh $RANGER_HOME/usersync/ranger-usersync.sh
\cp -f /opt/service-render-output/ranger-usersync-install.properties $RANGER_HOME/usersync/install.properties
source $RANGER_HOME/usersync/ranger-usersync.sh


until find /workspace/logs -mmin -1 -type f -name '*.log' ! -name '*gc*' | grep -q .
do
  echo "`date`: Waiting for logs..."
  sleep 2
done
find /workspace/logs -mmin -1 -type f -name '*.log' ! -name '*gc*' -exec tail -F {} +

echo "---------------------------------------------开始----------------------------------------------"
tail -f /dev/null