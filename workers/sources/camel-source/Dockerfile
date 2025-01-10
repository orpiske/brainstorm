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
RUN mkdir -p /opt/brainstorm/worker && \
    mkdir -p /opt/brainstorm/data && \
    mkdir -p /opt/brainstorm/classpath && \
    mkdir -p /opt/brainstorm/source
VOLUME /opt/brainstorm/data
WORKDIR /opt/brainstorm/worker/
COPY --chmod=755 target/run.sh .
COPY --chmod=744 target/camel-source-jar-with-dependencies.jar ./
CMD /opt/brainstorm/worker/run.sh