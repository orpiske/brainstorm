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

import java.io.File;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.Response;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AcquisitionResourceTest {
    private static final Logger LOG = Logger.getLogger(AcquisitionResourceTest.class);

    @Order(1)
    @Test
    void testActivitiesEmpty() {
        byte[] data = Base64.getEncoder().encode("abc\ndef\nghi\njkl".getBytes());
        String body = new String(data);

        given()
                .contentType(ContentType.TEXT)
                .body(body)
                .when()
                .post(AcquisitionResource.BASE_URI)
                .then()
                .statusCode(Response.Status.OK.getStatusCode());

        final File file = new File("target/data/route.yaml");
        Awaitility.await().atMost(3, TimeUnit.SECONDS).untilAsserted(() ->
                Assertions.assertTrue(file.exists(), "A route file should have been created"));

    }
}
