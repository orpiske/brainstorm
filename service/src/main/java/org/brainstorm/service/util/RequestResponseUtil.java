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

package org.brainstorm.service.util;

import java.net.URI;

import jakarta.ws.rs.core.Response;

import org.jboss.logging.Logger;

public final class RequestResponseUtil {
    private static final Logger LOG = Logger.getLogger(RequestResponseUtil.class);
    private RequestResponseUtil() {}

    public static String toLocationString(String baseURI, long id) {
        return baseURI + "/id/" + id;
    }

    public static URI toLocation(String baseURI, long id) {
        return URI.create(toLocationString(baseURI, id));
    }

    public static <T> Response validateFetchedObject(T object) {
        try {
            if (object == null) {
                LOG.debugf("Record not found with that id");
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            return Response.ok(object).build();
        } catch (Exception e) {
            LOG.errorf(e, "Unable to fetch record: %s", e.getMessage());
            return Response.serverError().build();
        }
    }
}

