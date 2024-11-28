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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.brainstorm.api.pipeline.Pipeline;
import org.brainstorm.api.pipeline.acquisition.Acquisition;
import org.brainstorm.api.pipeline.acquisition.CamelStep;
import org.brainstorm.api.pipeline.transformation.LocalStep;
import org.brainstorm.api.pipeline.transformation.Transformation;
import org.brainstorm.service.util.MapperUtils;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PipelineResourceTest {
    private static final Logger LOG = Logger.getLogger(PipelineResourceTest.class);


    @Order(1)
    @Test
    void testValidateJsonPipeline() throws JsonProcessingException {
        final Pipeline pipeline = newPipeline();

        ObjectMapper objectMapper = MapperUtils.newForDefault();
        final String pipelineStr = objectMapper.writeValueAsString(pipeline);

        LOG.debugf("Generated pipeline data \n%s", pipelineStr);

        given()
                .contentType(ContentType.JSON)
                .body(pipelineStr)
                .when()
                .post(PipelineResource.BASE_URI + "/validate")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Order(2)
    @Test
    void testValidateYAMLPipeline() throws JsonProcessingException {
        final Pipeline pipeline = newPipeline();

        ObjectMapper objectMapper = MapperUtils.newForYaml();
        final String pipelineStr = objectMapper.writeValueAsString(pipeline);

        LOG.debugf("Generated pipeline data \n%s", pipelineStr);
        byte[] data = Base64.getEncoder().encode(pipelineStr.getBytes());
        String encoded = new String(data);

        given()
                .contentType(ContentType.TEXT)
                .body(encoded)
                .when()
                .post(PipelineResource.BASE_URI + "/validate/yaml")
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    private static Pipeline newPipeline() {
        CamelStep camelStep1 = new CamelStep();
        camelStep1.setBootstrapServer("localhost");
        camelStep1.setProducesTo("data.acquired");
        camelStep1.setDependencies(List.of("org.brainstorm.camel:code-fetcher:1.0-SNAPSHOT"));

        Acquisition acquisition = new Acquisition();
        acquisition.getSteps().add(camelStep1);

        LocalStep localStep1 = new LocalStep();
        localStep1.setBootstrapServer("localhost");
        localStep1.setConsumesFrom("data.acquired");
        localStep1.setProducesTo("data.prepared");
        localStep1.setScript("/todo/change1");

        LocalStep localStep2 = new LocalStep();
        localStep2.setBootstrapServer("localhost");
        localStep2.setConsumesFrom("data.prepared");
        localStep2.setProducesTo("data.completed");
        localStep2.setScript("/todo/change2");

        Transformation transformation = new Transformation();

        transformation.getSteps().add(localStep1);
        transformation.getSteps().add(localStep2);

        Pipeline pipeline = new Pipeline();
        pipeline.setAcquisition(acquisition);
        pipeline.setTransformation(transformation);
        return pipeline;
    }

}
