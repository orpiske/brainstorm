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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.brainstorm.cli.common.CredentialsHelper;
import org.brainstorm.cli.common.ImageBuilder;
import org.brainstorm.cli.types.project.PipelineProject;
import org.brainstorm.cli.types.project.Transformation;
import org.brainstorm.cli.util.ProcessRunner;
import org.jboss.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "build",
        description = "Build a brainstorm project", sortOptions = false)
public class ProjectBuild extends BaseCommand {
    private static final Logger LOG = Logger.getLogger(ProjectBuild.class);

    @CommandLine.Option(names = {"--path"}, description = "The project path", defaultValue = ".", arity = "0..1")
    protected String path;

    @CommandLine.Option(names = {"--credentials"}, description = "The path to the properties file containing the credentials to use", defaultValue = "${user.home}/.brainstorm/cli/credentials.properties",
            arity = "0..1")
    protected String credentialsPath;

    @Override
    public void run() {
        final File projectDir = new File(path);
        File file = new File(projectDir, "brainstorm.json");

        ObjectMapper mapper = new ObjectMapper();
        try {
            final PipelineProject project = mapper.readValue(file, PipelineProject.class);

            final String organizationName = project.getOrganization().getName();
            final String registryName = project.getRegistry().getName();
            final String version = project.getVersion();

            cleanProject(project, organizationName, registryName, version, file);
            buildProject(project, organizationName, registryName, version, file);

            ImageBuilder imageBuilder = new ImageBuilder(CredentialsHelper.loadProperties(credentialsPath));

            buildContainers(projectDir, project, imageBuilder);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void buildContainers(File projectDir, PipelineProject project, ImageBuilder imageBuilder) {
        final Map<File, String> images = evalImagesToBuild(projectDir, project);
        images.forEach((k,v) -> buildImage(imageBuilder, k, v));
    }

    private void buildImage(ImageBuilder imageBuilder, File dir, String name) {
        LOG.infof("Building image %s from %s", name, dir);
        imageBuilder.buildInPath(dir, name);
        imageBuilder.push(name);
    }

    private static Map<File, String> evalImagesToBuild(File projectDir, PipelineProject project) {
        Map<File, String> images = new HashMap<>();
        prepareSource(projectDir, project, images);

        prepareSink(projectDir, project, images);

        final List<Transformation> transformations = project.getTransformations();
        if (transformations != null) {
            File transformationsDir = new File(projectDir, "transformation");
            for (int i = 0; i < transformations.size(); i++) {
                final Transformation transformation = transformations.get(i);
                String transformationNumber = String.format("%02d", i + 1);

                File transformationDir = new File(transformationsDir, transformationNumber);
                String transformationImage = transformation.getName();

                images.put(transformationDir, transformationImage);
            }
        }
        return images;
    }

    private static void copyArtifacts(File baseDir, List<String> artifacts) {
        for (String artifact : artifacts) {
            File sourceArtifactFile = new File(baseDir.getParentFile(), artifact);
            File destinationArtifactFile = new File(baseDir, sourceArtifactFile.getName());

            try {
                LOG.infof("Copying artifact from %s to %s", sourceArtifactFile, destinationArtifactFile);
                Files.copy(sourceArtifactFile.toPath(), destinationArtifactFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                destinationArtifactFile.deleteOnExit();
            }
        }
    }

    private static void prepareSink(File projectDir, PipelineProject project, Map<File, String> images) {
        File sinkDir = new File(projectDir, "sink");
        String sinkOutputImage = project.getSink().getImage().getName();

        final List<String> artifacts = project.getSink().getArtifacts();
        copyArtifacts(sinkDir, artifacts);

        images.put(sinkDir, sinkOutputImage);
    }

    private static void prepareSource(File projectDir, PipelineProject project, Map<File, String> images) {
        File sourceDir = new File(projectDir, "source");
        String sourceOutputImage = project.getSource().getImage().getName();
        final List<String> artifacts = project.getSource().getArtifacts();
        copyArtifacts(sourceDir, artifacts);
        images.put(sourceDir, sourceOutputImage);
    }

    private static void cleanProject(
            PipelineProject project, String organizationName, String registryName, String version, File file) {
        final String commandStr = project.getCode().getLifecycle().getClean().getCommand();
        String command = commandStr.replace("%organizationName%", organizationName)
                .replace("%registryName%", registryName)
                .replace("%version%", version);
        LOG.infof("About to run clean command: {}", command);
        ProcessRunner.run(file.getParentFile(), command.split(" "));
    }

    private static void buildProject(
            PipelineProject project, String organizationName, String registryName, String version, File file) {
        final String commandStr = project.getCode().getLifecycle().getBuild().getCommand();
        String command = commandStr.replace("%organizationName%", organizationName)
                .replace("%registryName%", registryName)
                .replace("%version%", version);
        LOG.infof("About to run build command: {}", command);
        ProcessRunner.run(file.getParentFile(), command.split(" "));
    }


}
