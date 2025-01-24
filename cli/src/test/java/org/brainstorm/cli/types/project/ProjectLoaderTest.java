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

package org.brainstorm.cli.types.project;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ProjectLoaderTest {

    @Test
    public void testLoadProject() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            final URL resource = getClass().getResource("/sample-project.json");
            final PipelineProject pipelineProject =
                    mapper.readValue(resource, PipelineProject.class);

            Assertions.assertNotNull(pipelineProject);
            Assertions.assertEquals(pipelineProject.getOrganization().getName(),"orpiske");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
