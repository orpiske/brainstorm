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

package org.brainstorm.cli.command;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.jboss.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "new",
        description = "Create a new brainstorm project", sortOptions = false)
public class ProjectNew extends BaseCommand {
    private static final Logger LOG = Logger.getLogger(ProjectNew.class);

    @CommandLine.Option(names = {"--template"}, description = "The project template to use", defaultValue = "simple", arity = "0..1")
    protected String template;

    @CommandLine.Option(names = {"--destination"}, description = "The destination directory for the project", defaultValue = "simple", arity = "0..1")
    protected String destination;

    @Override
    public void run() {
        Properties properties = new Properties();

        try (InputStream resourceAsStream = getClass().getResourceAsStream(
                String.format("/templates/%s/template.properties", template))) {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final String resourcesProperty = properties.getProperty("resources");
        final String[] resources = resourcesProperty.split(",");
        for (String resource : resources) {
            try (InputStream resourceAsStream = getClass().getResourceAsStream(
                    String.format("/templates/%s/%s", template, resource))) {
                if (resourceAsStream == null) {
                    LOG.infof("Resource not found '%s'. Likely incorrect value in the template.properties file", resource);
                }

                final Path destinationPath = Paths.get(destination, resource);
                LOG.infof("Extracting new project template file '%s' to %s", resource, destinationPath);
                if(Files.exists(destinationPath)) {
                    LOG.infof("File already exists %s. Ignoring", destinationPath);
                    continue;
                }

                Files.createDirectories(destinationPath.getParent());
                Files.copy(resourceAsStream, destinationPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


    }
}
