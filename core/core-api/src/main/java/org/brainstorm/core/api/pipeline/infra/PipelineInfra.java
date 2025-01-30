/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brainstorm.core.api.pipeline.infra;

import java.util.ArrayList;
import java.util.List;

public class PipelineInfra {
    private String bootstrapServer;
    private int port;
    private List<String> environmentVariables = new ArrayList<>();

    public String getBootstrapServer() {
        return bootstrapServer;
    }

    public void setBootstrapServer(String bootstrapServer) {
        this.bootstrapServer = bootstrapServer;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(List<String> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    @Override
    public String toString() {
        return "PipelineInfra{" +
                "bootstrapServer='" + bootstrapServer + '\'' +
                ", port=" + port +
                '}';
    }
}
