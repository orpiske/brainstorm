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

FROM eclipse-temurin:21-ubi9-minimal as base
LABEL maintainer="orpiske@apache.org"
RUN mkdir -p /opt/brainstorm/worker
ADD target/run.sh /opt/brainstorm/worker/run.sh
ADD target/runner-worker-jar-with-dependencies.jar /opt/brainstorm/worker/
RUN chmod +x opt/brainstorm/worker/run.sh
CMD opt/brainstorm/worker/run.sh