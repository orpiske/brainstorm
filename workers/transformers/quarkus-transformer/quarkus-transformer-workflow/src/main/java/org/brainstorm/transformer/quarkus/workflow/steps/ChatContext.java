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

package org.brainstorm.transformer.quarkus.workflow.steps;

/**
 * Carries chat context, along with I/O payload
 * @param <T>
 * @param <V>
 */
public class ChatContext<T, V> {
    private final String name;
    private final T inputData;
    private final V outputData;
    private final String rawData;

    public ChatContext(String name, T inputData, V outputData, String rawData) {
        this.name = name;
        this.inputData = inputData;
        this.outputData = outputData;
        this.rawData = rawData;
    }

    public String getName() {
        return name;
    }

    public T getInputData() {
        return inputData;
    }

    public V getOutputData() {
        return outputData;
    }

    public String getRawData() {
        return rawData;
    }
}
