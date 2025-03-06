#!/bin/bash

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

USERSYNC_ENABLE="${conf['usersync.enable']}"

if [ "$USERSYNC_ENABLE" = "true" ]; then

  # Enable Ranger User Sync
  cd "${RANGER_HOME}"/usersync &&
  ./setup.sh && ./start.sh

  RANGER_USERSYNC_PID=`ps -ef  | grep -v grep | grep -i "org.apache.ranger.authentication.UnixAuthenticationService" | awk '{print $2}'`

  # prevent the container from exiting
  if [ -z "$RANGER_USERSYNC_PID" ]
  then
    echo "The UserSync process probably exited, no process id found!"
  else
    tail --pid=$RANGER_USERSYNC_PID -f /dev/null
  fi
else
  echo "Disable Ranger."
fi

# Script execution completed
exit 0

