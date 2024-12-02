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

package org.brainstorm.service.controller;

import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.context.SmallRyeManagedExecutor;
import org.brainstorm.api.pipeline.Pipeline;
import org.brainstorm.api.pipeline.acquisition.AbstractAcquisitionStep;
import org.brainstorm.api.pipeline.transformation.AbstractTransformationStep;
import org.brainstorm.service.util.BrainstormConfiguration;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
public class PipelineController {
    private static final Logger LOG = Logger.getLogger(PipelineController.class);

    @Inject
    BrainstormConfiguration configuration;

    @Inject
    @Channel("acquisition")
    Emitter<String> stepEmitter;

    @Inject
    @Channel("transformation")
    Emitter<String> transformationEmitter;

    SmallRyeManagedExecutor managedExecutor = SmallRyeManagedExecutor.builder()
            .withExecutorService(Executors.newCachedThreadPool())
            .build();

    private ObjectMapper mapper = new ObjectMapper();

    private void execute(AbstractAcquisitionStep step) {
        try {
            stepEmitter.send(mapper.writeValueAsString(step));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void execute(AbstractTransformationStep step) {
        try {
            transformationEmitter.send(mapper.writeValueAsString(step));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void execute(Pipeline pipeline) {
        LOG.info("Executing pipeline");

        LOG.info("Executing acquisition steps");
        final var steps = pipeline.getAcquisition().getSteps();
        for (var step : steps) {
            execute(step);
        }
        LOG.info("Finished acquisition steps");

        LOG.info("Executing transformation steps");
        final var transformationSteps = pipeline.getTransformation().getSteps();
        for (var step : transformationSteps) {
            execute(step);
        }
        LOG.info("Finished transformation steps");
    }

    @ConsumeEvent(value = "pipeline", blocking = true)
    public void executeInternally(Pipeline pipeline) {
        LOG.info("Executing pipeline internallzy (blocking)");

        managedExecutor.execute(() -> execute(pipeline));
    }

}
