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

package org.brainstorm.transformer.quarkus.event;

import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import io.smallrye.common.annotation.Identifier;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

@ApplicationScoped
public class KafkaProviders {

    @Inject
    EventSource eventSource;

    @Inject
    @Identifier("default-kafka-broker")
    Map<String, Object> config;

    @Produces
    KafkaConsumer<String, String> getConsumer() {
        if (eventSource.isHelpOnly()) {
            return null;
        }

        config.put("bootstrap.servers", String.format("%s:%s", eventSource.getBootstrapServers(), eventSource.getBootstrapServerPort()));
        config.put("group.id", "transformer-quarkus");

        return new KafkaConsumer<>(config,
                new StringDeserializer(),
                new StringDeserializer());
    }

    @Produces
    KafkaProducer<String, String> getProducer() {
        if (eventSource.isHelpOnly()) {
            return null;
        }

        config.put("bootstrap.servers", String.format("%s:%s", eventSource.getBootstrapServers(), eventSource.getBootstrapServerPort()));
        config.put("group.id", "transformer-quarkus");

        return new KafkaProducer<>(config,
                new StringSerializer(),
                new StringSerializer());
    }
}
