#!/bin/bash

RANGER_HOME=/opt/ranger

mkdir -p /opt/ranger
cp -rf /opt/service-render-output/solr_for_audit_setup $RANGER_HOME/

# 执行第一个脚本
$RANGER_HOME/solr_for_audit_setup/setup.sh
setup_status=$?

# 检查第一个脚本是否执行成功
if [ $setup_status -eq 0 ]; then
  echo "setup.sh executed successfully. Continuing to the next script."

  # 执行第二个脚本
  $SOLR_HOME/ranger_audit_server/scripts/add_ranger_audits_conf_to_zk.sh
  add_conf_status=$?

  # 检查第二个脚本是否执行成功
  if [ $add_conf_status -eq 0 ]; then
    echo "add_ranger_audits_conf_to_zk.sh executed successfully. Continuing to the next script."

    # 执行第三个脚本
    $SOLR_HOME/ranger_audit_server/scripts/create_ranger_audits_collection.sh
    create_collection_status=$?

    # 检查第三个脚本是否执行成功
    if [ $create_collection_status -eq 0 ]; then
      echo "create_ranger_audits_collection.sh executed successfully."
    else
      echo "create_ranger_audits_collection.sh failed with status $create_collection_status."
      exit $create_collection_status
    fi
  else
    echo "add_ranger_audits_conf_to_zk.sh failed with status $add_conf_status."
    exit $add_conf_status
  fi
else
  echo "setup.sh failed with status $setup_status."
  exit $setup_status
fi

