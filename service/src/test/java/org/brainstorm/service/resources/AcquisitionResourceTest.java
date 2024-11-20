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

import jakarta.ws.rs.core.Response;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.brainstorm.api.dto.AcquisitionService;
import org.brainstorm.service.util.RequestResponseUtil;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AcquisitionResourceTest {
    private static final Logger LOG = Logger.getLogger(AcquisitionResourceTest.class);

    @Order(1)
    @Test
    void testActivitiesEmpty() {
        given()
                .when().get(AcquisitionResource.BASE_URI)
                .then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Order(2)
    @Test
    void testGetActivities() {
        AcquisitionService service = new AcquisitionService();
        service.setName("test");
        service.setGav("org.id:test:1.0");

        given()
                .contentType(ContentType.JSON)
                .body(service)
                .when()
                .post(AcquisitionResource.BASE_URI)
                .then()
                .statusCode(Response.Status.CREATED.getStatusCode());
    }

    @Order(3)
    @Test
    void getById() {
        final var response = given()
                .when().get(RequestResponseUtil.toLocationString(AcquisitionResource.BASE_URI, 1));

        LOG.infof("Response: %s", response.getBody().asString());

        response.then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(  "id", notNullValue(),
                        "name", is("test"),
                        "gav", is("org.id:test:1.0"));
    }
}
