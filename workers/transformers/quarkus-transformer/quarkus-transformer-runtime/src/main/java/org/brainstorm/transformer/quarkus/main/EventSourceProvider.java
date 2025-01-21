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

package org.brainstorm.transformer.quarkus.main;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.brainstorm.transformer.quarkus.event.EventSource;
import picocli.CommandLine;

public class EventSourceProvider {

    @Inject
    CommandLine.ParseResult parseResult;

    @Produces
    @ApplicationScoped
    @Priority(1)
    @Alternative
    EventSource getEventSource() {
        final EventSource eventSource = new EventSource();

        if (!parseResult.isUsageHelpRequested()) {
            String bootstrapHost = parseResult.matchedOption("bootstrap-server").getValue().toString();
            int bootstrapPort = parseResult.matchedOptionValue("bootstrap-server-port", 9092);
            String consumesFrom = parseResult.matchedOption("consumes-from").getValue().toString();
            String producesTo = parseResult.matchedOption("produces-to").getValue().toString();

            eventSource.setBootstrapServers(bootstrapHost);
            eventSource.setBootstrapServerPort(String.valueOf(bootstrapPort));
            eventSource.setConsumesFrom(consumesFrom);
            eventSource.setProducesTo(producesTo);

            return eventSource;
        }
        eventSource.setHelpOnly(true);
        return eventSource;
    }
}
