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

import java.io.StringWriter;
import java.util.List;

import jakarta.ws.rs.core.Response;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.brainstorm.api.pipeline.Acquisition;
import org.brainstorm.api.pipeline.AcquisitionStep;
import org.brainstorm.api.pipeline.Pipeline;
import org.brainstorm.api.pipeline.Transformation;
import org.brainstorm.api.pipeline.TransformationStep;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.nodes.Tag;

import static io.restassured.RestAssured.given;

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

        TransformationStep transformationStep = new TransformationStep();
        transformationStep.setType("run-worker");
        transformationStep.setBootstrapServer("localhost");
        transformationStep.setConsumesFrom("data.acquired");
        transformationStep.setProducesTo("data.prepared");
        transformationStep.setFile("/todo/change");

        Transformation transformation = new Transformation();
        transformation.getSteps().add(transformationStep);

        Pipeline pipeline = new Pipeline();
        pipeline.setAcquisition(acquisition);
        pipeline.setTransformation(transformation);

        Yaml yaml = new Yaml();

        final String pipeline1 = yaml.dumpAs(pipeline, new Tag("pipeline"), DumperOptions.FlowStyle.AUTO);
        System.out.println(pipeline1);

        given()
                .contentType(ContentType.TEXT)
                .body(pipeline1)
                .when()
                .post(PipelineResource.BASE_URI)
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }


}
