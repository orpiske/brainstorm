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

package org.brainstorm.service.resources;

import java.util.Base64;
import java.util.List;

import jakarta.ws.rs.core.Response;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.brainstorm.api.pipeline.Acquisition;
import org.brainstorm.api.pipeline.AcquisitionStep;
import org.brainstorm.api.pipeline.Pipeline;
import org.brainstorm.api.pipeline.Transformation;
import org.brainstorm.api.pipeline.TransformationStep;
import org.brainstorm.service.util.YamlUtils;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.yaml.snakeyaml.Yaml;

import static io.restassured.RestAssured.given;

@Disabled("Needs adjustments")
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PipelineResourceTest {
    private static final Logger LOG = Logger.getLogger(PipelineResourceTest.class);


    @Order(1)
    @Test
    void testNewPipeline() {
        AcquisitionStep acquisitionStep = new AcquisitionStep();
        acquisitionStep.setType("camel-worker");
        acquisitionStep.setBootstrapServer("localhost");
        acquisitionStep.setProducesTo("data.acquired");
        acquisitionStep.setDependencies(List.of("org.brainstorm.camel:code-fetcher:1.0-SNAPSHOT"));

        Acquisition acquisition = new Acquisition();
        acquisition.getSteps().add(acquisitionStep);

        TransformationStep transformationStep1 = new TransformationStep();
        transformationStep1.setType("run-worker");
        transformationStep1.setBootstrapServer("localhost");
        transformationStep1.setConsumesFrom("data.acquired");
        transformationStep1.setProducesTo("data.prepared");
        transformationStep1.setScript("/todo/change1");

        TransformationStep transformationStep2 = new TransformationStep();
        transformationStep2.setType("run-worker");
        transformationStep2.setBootstrapServer("localhost");
        transformationStep2.setConsumesFrom("data.prepared");
        transformationStep2.setProducesTo("data.completed");
        transformationStep2.setScript("/todo/change2");


        Transformation transformation = new Transformation();
        transformation.getSteps().add(transformationStep1);
        transformation.getSteps().add(transformationStep2);

        Pipeline pipeline = new Pipeline();
        pipeline.setAcquisition(acquisition);
        pipeline.setTransformation(transformation);

        Yaml yaml = YamlUtils.getYamlForClass(Pipeline.class);
        final String pipelineStr = yaml.dump(pipeline);

        LOG.debugf("Generated pipeline data \n%s", pipelineStr);
        byte[] data = Base64.getEncoder().encode(pipelineStr.getBytes());
        String encoded = new String(data);

        given()
                .contentType(ContentType.TEXT)
                .body(encoded)
                .when()
                .post(PipelineResource.BASE_URI)
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }


}
