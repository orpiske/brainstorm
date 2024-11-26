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


import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Base64;

import org.brainstorm.api.pipeline.Pipeline;
import org.brainstorm.service.util.YamlUtils;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.Yaml;

@Transactional
@Path("/api/v1/pipeline/")
public class PipelineResource {
    private static final Logger LOG = Logger.getLogger(PipelineResource.class);
    static final String BASE_URI = "/api/v1/pipeline";

    @POST
    @Produces(MediaType.MEDIA_TYPE_WILDCARD)
    @Consumes(MediaType.MEDIA_TYPE_WILDCARD)
    @Path("/")
    public Response add(String body) {
        LOG.debugf("About to process pipeline: %s", body);

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(body.trim());
            String data = new String(decodedBytes);

            Yaml yaml = YamlUtils.getYamlForClass(Pipeline.class);
            final Pipeline pipeline = yaml.loadAs(data, Pipeline.class);

            LOG.debugf("Pipeline created: %s", pipeline);

            return Response.ok().build();
        } catch (Exception e) {
            LOG.error(e);
            return Response.serverError().build();
        }
    }
}
