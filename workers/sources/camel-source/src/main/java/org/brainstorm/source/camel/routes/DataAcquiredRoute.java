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

package org.brainstorm.source.camel.routes;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.brainstorm.api.event.DataAcquired;
import org.brainstorm.source.camel.common.Topics;

public class DataAcquiredRoute extends RouteBuilder {
    private final String bootstrapHost;
    private final int bootstrapPort;
    private final String producesTo;
    private final String notifies;
    private final String dataDirectory;

    public DataAcquiredRoute(String bootstrapHost, int bootstrapPort, String producesTo, String notifies, String dataDirectory) {
        this.bootstrapHost = bootstrapHost;
        this.bootstrapPort = bootstrapPort;
        this.producesTo = producesTo;
        this.notifies = notifies;
        this.dataDirectory = dataDirectory;
    }

    public void process(Exchange exchange) {
        DataAcquired dataAcquired = new DataAcquired();

        final String name = exchange.getMessage().getHeader("name", String.class);
        final String address = exchange.getMessage().getHeader("SOURCE_ADDRESS", String.class);
        final String dataDir = exchange.getMessage().getHeader("DATA_DIRECTORY", String.class);

        dataAcquired.setName(name);
        dataAcquired.setAddress(address);
        dataAcquired.setPath(dataDir);

        exchange.getMessage().setBody(dataAcquired);
    }

    @Override
    public void configure() {
        fromF("timer:startTimer?period=1000&repeatCount=1")
            .routeId("DataAcquiredRoute")
            .setHeader("DATA_DIRECTORY", constant(dataDirectory))
            .to("direct:data.start");


        fromF("direct:%s", Topics.DATA_ACQUIRED)
                .process(this::process)
                .marshal().json()
                .toF("kafka:%s?brokers=%s:%d", producesTo , bootstrapHost, bootstrapPort)
                .toF("direct:%s", notifies)
                .log("Data acquired");
    }
}
