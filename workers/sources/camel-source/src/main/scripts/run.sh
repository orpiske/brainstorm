#!/bin/sh
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
install_path=$(dirname $0)
jar_file=camel-source-jar-with-dependencies.jar

WORKER_CP=${WORKER_CP:-/opt/brainstorm/classpath/}
mainClass=org.brainstorm.source.camel.main.CamelSourceMain

echo "Running ..."

fullClassPath=$(for jarFile in ${WORKER_CP}/*.jar ; do echo "${jarFile}:" ; done)

java -cp "${fullClassPath}""${install_path}"/${jar_file} ${mainClass} \
    --bootstrap-server "${BOOTSTRAP_HOST}" \
    --bootstrap-server-port "${BOOTSTRAP_PORT:-9092}" \
    --produces-to "${PRODUCES_TO}" \
    --data-directory "${DATA_DIRECTORY}" \
    --file "${SOURCE_ROUTE_PATH}"