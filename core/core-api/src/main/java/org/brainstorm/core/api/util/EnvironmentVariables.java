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

package org.brainstorm.core.api.util;

public final class EnvironmentVariables {
    public static final String BOOTSTRAP_HOST = "BOOTSTRAP_HOST";
    public static final String BOOTSTRAP_PORT = "BOOTSTRAP_PORT";
    public static final String STEP = "STEP";
    public static final String CONSUMES_FROM = "CONSUMES_FROM";
    public static final String PRODUCES_TO = "PRODUCES_TO";
    public static final String DATA_DIRECTORY = "DATA_DIRECTORY";
    public static final String WORKER_CP = "WORKER_CP";
    public static final String SOURCE_ROUTE_PATH = "SOURCE_ROUTE_PATH";

    private EnvironmentVariables() {}
}
