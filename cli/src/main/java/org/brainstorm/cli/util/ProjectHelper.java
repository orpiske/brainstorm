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

package org.brainstorm.cli.util;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.brainstorm.cli.command.ProjectBuild;
import org.brainstorm.cli.types.project.PipelineProject;
import org.jboss.logging.Logger;

public final class ProjectHelper {
    private static final Logger LOG = Logger.getLogger(ProjectHelper.class);
    public static final String DEFAULT_PROJECT_FILE = "brainstorm.json";

    private ProjectHelper() {}

    public static PipelineProject loadProject(File projectDir) throws IOException {
        File file = new File(projectDir, DEFAULT_PROJECT_FILE);

        ObjectMapper mapper = new ObjectMapper();
        final PipelineProject project = mapper.readValue(file, PipelineProject.class);
        return project;
    }

    public static void runCommandOnProject(
            String organizationName, String registryName, String version, File projectDir, String commandStr) {
        String command = commandStr.replace("%organizationName%", organizationName)
                .replace("%registryName%", registryName)
                .replace("%version%", version);
        LOG.infof("About to run command: {}", command);
        ProcessRunner.run(projectDir, command.split(" "));
    }
}
