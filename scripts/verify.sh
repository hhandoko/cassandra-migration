#!/bin/bash

###
# File     : verify.sh
# License  :
#   Copyright (c) 2016 - 2017 Citadel Technology Solutions Pte Ltd
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#           http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
###

echo $JAVA_HOME
./gradlew check \
  -Dorg.gradle.java.home=$JAVA_HOME \
  -Dorg.gradle.jvmargs="-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError" \
  -Dit.config.file=src/test/resources/application.it-test.conf \
  -Dcassandra.migration.cluster.contactpoints=127.0.0.1 \
  -Dcassandra.migration.cluster.port=9042 \
  -Dcassandra.migration.disable_embedded=true \
  --info
