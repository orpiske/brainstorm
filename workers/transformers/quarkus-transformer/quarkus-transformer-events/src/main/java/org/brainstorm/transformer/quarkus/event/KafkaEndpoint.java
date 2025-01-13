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

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.jboss.logging.Logger;

@ApplicationScoped
public class KafkaEndpoint {
    private static final Logger LOG = Logger.getLogger(KafkaEndpoint.class);

    @Inject
    KafkaConsumer<String, String> consumer;

    @Inject
    KafkaProducer<String, String> producer;

    @Inject
    EventSource eventSource;

    @Inject
    EventController eventController;

    final ExecutorService executor = Executors.newSingleThreadExecutor();

    volatile boolean done = false;

    public KafkaEndpoint() {
        LOG.info("KafkaEndpoint created");
    }

    public void run() {
        LOG.infof("Starting the event consumer");
        String topic = eventSource.getTopicName();

        LOG.infof("Consuming from topic: %s", topic);

        consumer.subscribe(Collections.singleton(topic));
        executor.submit(this::doPoll);
        LOG.infof("Waiting for completion ...");

        Quarkus.waitForExit();
        LOG.infof("Consumer done");
    }


    private void doPoll() {
        while (!done) {
            final ConsumerRecords<String, String> consumerRecords = consumer.poll(Duration.ofSeconds(1));
            consumerRecords.forEach(record -> eventController.handle(record.value()));
        }
        LOG.infof("Closing consumer");
        consumer.close();
    }

    public void terminate(@Observes ShutdownEvent ev) {
        if (eventSource.isHelpOnly()) {
            return;
        }

        LOG.infof("Shutting down ... %s", ev);
        done = true;


        producer.close();
        executor.shutdown();
    }
}
