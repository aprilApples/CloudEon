#!/bin/bash

# A FreeMarker template for enabling/disabling Ranger Hive plugin

# Check if RANGER_HOME is set
if [ -z "$RANGER_HOME" ]; then
  echo "RANGER_HOME is not set. Please set RANGER_HOME to the root directory of Ranger installation."
  exit 1
fi

# Read the value of ranger.enable from a configuration file or environment variable
# For the purpose of this example, we assume it's passed as an environment variable
# Replace this with the actual source of your configuration if needed
RANGER_ENABLE="${conf['ranger.enable']}"

# Execute the corresponding script based on the value of ranger.enable
if [ "$RANGER_ENABLE" = "true" ]; then
  # Enable Ranger Hive plugin
  cp -f /opt/service-render-output/ranger-install.properties $RANGER_HOME/ranger-yarn-plugin/install.properties
  cp -f /opt/service-render-output/ranger-xasecure-audit.xml $RANGER_HOME/ranger-yarn-plugin/install/conf.templates/enable/xasecure-audit.xml
  bash $RANGER_HOME/ranger-yarn-plugin/enable-yarn-plugin.sh
else
  echo "Disable Ranger."
fi

# Script execution completed
exit 0
