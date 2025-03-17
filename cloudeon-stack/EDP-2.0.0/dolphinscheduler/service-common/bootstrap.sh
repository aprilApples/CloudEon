#!/usr/bin/env bash
set -xeuo pipefail

mkdir -p /workspace/logs

\cp -f /etc/hdfs-config/* $HADOOP_CONF_DIR
\cp -f /etc/yarn-config/* $HADOOP_CONF_DIR

if [ -d /etc/flink-config ]; then
    \cp -f /etc/flink-config/* $FLINK_HOME/conf/
    # 支持flink on yarn模式
    \cp -f /opt/service-render-output/flink-conf.yaml $FLINK_HOME/conf/flink-conf.yaml
fi
mkdir -p $DOLPHINSCHEDULER_HOME/conf
\cp -f $HADOOP_CONF_DIR/*.xml $DOLPHINSCHEDULER_HOME/conf/
\cp -f /opt/service-render-output/dolphinscheduler_env.sh $DOLPHINSCHEDULER_HOME/bin/env/

\cp -f /opt/service-render-output/common.properties $DOLPHINSCHEDULER_HOME/master-server/conf/
# bootstrap.yaml中有k8s配置，干扰运行。其他只有spring.application.name配置项，已迁移到application.yaml
rm -f $DOLPHINSCHEDULER_HOME/master-server/conf/bootstrap.yaml
\cp -f /opt/service-render-output/jvm_args_env.sh $DOLPHINSCHEDULER_HOME/master-server/bin/
\cp -f /opt/service-render-output/dolphinscheduler_env.sh $DOLPHINSCHEDULER_HOME/master-server/conf/
\cp -f /opt/service-render-output/master-application.yaml $DOLPHINSCHEDULER_HOME/master-server/conf/application.yaml

\cp -f /opt/service-render-output/common.properties $DOLPHINSCHEDULER_HOME/worker-server/conf/
rm -f $DOLPHINSCHEDULER_HOME/worker-server/conf/bootstrap.yaml
\cp -f /opt/service-render-output/jvm_args_env.sh $DOLPHINSCHEDULER_HOME/worker-server/bin/
\cp -f /opt/service-render-output/dolphinscheduler_env.sh $DOLPHINSCHEDULER_HOME/worker-server/conf/
\cp -f /opt/service-render-output/worker-application.yaml $DOLPHINSCHEDULER_HOME/worker-server/conf/application.yaml

\cp -f /opt/service-render-output/common.properties $DOLPHINSCHEDULER_HOME/api-server/conf/
rm -f $DOLPHINSCHEDULER_HOME/api-server/conf/bootstrap.yaml
\cp -f /opt/service-render-output/jvm_args_env.sh $DOLPHINSCHEDULER_HOME/api-server/bin/
\cp -f /opt/service-render-output/dolphinscheduler_env.sh $DOLPHINSCHEDULER_HOME/api-server/conf/
\cp -f /opt/service-render-output/api-application.yaml $DOLPHINSCHEDULER_HOME/api-server/conf/application.yaml

\cp -f /opt/service-render-output/common.properties $DOLPHINSCHEDULER_HOME/alert-server/conf/
rm -f $DOLPHINSCHEDULER_HOME/alert-server/conf/bootstrap.yaml
\cp -f /opt/service-render-output/jvm_args_env.sh $DOLPHINSCHEDULER_HOME/alert-server/bin/
\cp -f /opt/service-render-output/dolphinscheduler_env.sh $DOLPHINSCHEDULER_HOME/alert-server/conf/
\cp -f /opt/service-render-output/alert-application.yaml $DOLPHINSCHEDULER_HOME/alert-server/conf/application.yaml

\cp -f /opt/service-render-output/common.properties $DOLPHINSCHEDULER_HOME/tools/conf/
\cp -f /opt/service-render-output/jvm_args_env.sh $DOLPHINSCHEDULER_HOME/tools/bin/
\cp -f /opt/service-render-output/dolphinscheduler_env.sh $DOLPHINSCHEDULER_HOME/tools/conf/
\cp -f /opt/service-render-output/tool-application.yaml $DOLPHINSCHEDULER_HOME/tools/conf/application.yaml

source /opt/service-render-output/check_init_db.sh

if [[ "${ROLE_FULL_NAME}" == "ds-master" ]]; then
  ln -s /workspace/logs master-server/logs
  \cp -f $DOLPHINSCHEDULER_HOME/master-server/conf/* $DOLPHINSCHEDULER_HOME/conf/
  dolphinscheduler-daemon.sh start master-server
fi

if [[ "${ROLE_FULL_NAME}" == "ds-worker" ]]; then
  ln -s /workspace/logs worker-server/logs
  \cp -f $DOLPHINSCHEDULER_HOME/worker-server/conf/* $DOLPHINSCHEDULER_HOME/conf/
  dolphinscheduler-daemon.sh start worker-server
fi

if [[ "${ROLE_FULL_NAME}" == "ds-api" ]]; then
  ln -s /workspace/logs api-server/logs
  \cp -f $DOLPHINSCHEDULER_HOME/api-server/conf/* $DOLPHINSCHEDULER_HOME/conf/
  dolphinscheduler-daemon.sh start api-server
fi
if [[ "${ROLE_FULL_NAME}" == "ds-alert" ]]; then
  ln -s /workspace/logs alert-server/logs
  \cp -f $DOLPHINSCHEDULER_HOME/alert-server/conf/* $DOLPHINSCHEDULER_HOME/conf/
  dolphinscheduler-daemon.sh start alert-server
fi

until find /workspace/logs -mmin -1 -type f -name '*.log' ! -name '*gc*' | grep -q .
do
  echo "`date`: Waiting for logs..."
  sleep 2
done
find /workspace/logs -mmin -1 -type f -name '*.log' ! -name '*gc*' -exec tail -F {} +

echo "---------------------------------------------开始----------------------------------------------"
tail -f /dev/null
