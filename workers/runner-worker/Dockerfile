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

FROM fedora:40 as base
LABEL maintainer="orpiske@apache.org"
RUN dnf install -y java-21-openjdk-headless && \
    mkdir -p /opt/brainstorm/data && \
    mkdir -p /opt/brainstorm/step && \
    dnf clean all
VOLUME /opt/brainstorm/data
WORKDIR /opt/brainstorm/worker/
COPY --chmod=755 target/run.sh .
COPY --chmod=644 target/runner-worker-jar-with-dependencies.jar ./
CMD /opt/brainstorm/worker/run.sh