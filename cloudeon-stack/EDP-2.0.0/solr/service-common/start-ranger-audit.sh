#!/bin/bash

RENDER_DIR=/opt/service-render-output
AUDIT_SETUP_DIR=/opt/ranger/solr_for_audit_setup

mkdir -p $AUDIT_SETUP_DIR/conf
mkdir -p $AUDIT_SETUP_DIR/resources
mkdir -p $AUDIT_SETUP_DIR/solr_cloud/scripts

cp -f $RENDER_DIR/ranger_audit_solrconfig.xml.j2 $AUDIT_SETUP_DIR/conf/solrconfig.xml.j2
cp -f $RENDER_DIR/managed-schema $AUDIT_SETUP_DIR/conf/managed-schema
cp -f $RENDER_DIR/ranger_audit_solrconfig.xml $AUDIT_SETUP_DIR/conf/solrconfig.xml
cp -f $RENDER_DIR/log4j.properties.j2 $AUDIT_SETUP_DIR/resources/log4j.properties.j2
cp -f $RENDER_DIR/add_ranger_audits_conf_to_zk.sh.j2 $AUDIT_SETUP_DIR/solr_cloud/scripts
cp -f $RENDER_DIR/create_ranger_audits_collection.sh.j2 $AUDIT_SETUP_DIR/solr_cloud/scripts
cp -f $RENDER_DIR/ranger_audit_solr.in.sh.j2 $AUDIT_SETUP_DIR/solr_cloud/scripts/solr.in.sh.j2
cp -f $RENDER_DIR/ranger_audit_solr.sh.j2 $AUDIT_SETUP_DIR/solr_cloud/scripts/solr.sh.j2
cp -f $RENDER_DIR/ranger_audit_start_solr.sh.j2 $AUDIT_SETUP_DIR/solr_cloud/scripts/start_solr.sh.j2
cp -f $RENDER_DIR/ranger_audit_stop_solr.sh.j2 $AUDIT_SETUP_DIR/solr_cloud/scripts/stop_solr.sh.j2
cp -f $RENDER_DIR/ranger_audit_solr.xml.j2 $AUDIT_SETUP_DIR/solr_cloud/solr.xml.j2
cp -f $RENDER_DIR/install.properties $AUDIT_SETUP_DIR/install.properties
cp -f $RENDER_DIR/ranger_audit_setup.sh $AUDIT_SETUP_DIR/setup.sh

chmod -R 775 $AUDIT_SETUP_DIR

# 执行第一个脚本
$AUDIT_SETUP_DIR/setup.sh
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
  echo "ranger_audit_setup.sh failed with status $setup_status."
  exit $setup_status
fi

