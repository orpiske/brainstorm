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
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.brainstorm.cli.types.project.Organization;
import org.brainstorm.cli.types.project.PipelineProject;
import org.brainstorm.cli.types.project.Registry;
import org.brainstorm.cli.types.project.Transformation;
import org.brainstorm.cli.util.ProjectHelper;
import picocli.CommandLine;

@CommandLine.Command(name = "configure",
        description = "Configure a brainstorm project", sortOptions = false)
public class ProjectConfigure extends BaseCommand {
    @CommandLine.Option(names = {"--path"}, description = "The project path", defaultValue = ".", arity = "0..1")
    protected String path;

    @CommandLine.Option(names = {"--registry"}, description = "The registry value", arity = "0..1")
    protected String registry;

    @CommandLine.Option(names = {"--organization"}, description = "The organization value", arity = "0..1")
    protected String organization;

    @CommandLine.Option(names = {"--version"}, description = "The version value", arity = "0..1")
    protected String version;

    @CommandLine.Option(names = {"--build-command"}, description = "The build command value", arity = "0..1")
    protected String buildCommand;

    @CommandLine.Option(names = {"--clean-command"}, description = "The clean command value", arity = "0..1")
    protected String cleanCommand;

    @CommandLine.Option(names = {"--source-file"}, description = "The source route file", defaultValue = "/source/routes.yaml", arity = "0..1")
    protected String sourceFile;

    @CommandLine.Option(names = {"--source-image"}, description = "The source image", arity = "0..1")
    protected String sourceImage;

    @CommandLine.Option(names = {"--reset-source-artifacts"}, description = "Whether to clear existing source artifacts", defaultValue = "false", arity = "0..1")
    protected boolean resetSourceArtifacts;

    @CommandLine.Option(names = {"--source-artifacts"}, description = "The source artifacts", arity = "0..*")
    protected List<String> sourceArtifacts;

    @CommandLine.Option(names = {"--sink-file"}, description = "The sink route file", defaultValue = "/sink/routes.yaml", arity = "0..1")
    protected String sinkFile;

    @CommandLine.Option(names = {"--sink-image"}, description = "The sink image", arity = "0..1")
    protected String sinkImage;

    @CommandLine.Option(names = {"--reset-sink-artifacts"}, description = "Whether to clear existing sink artifacts", defaultValue = "false", arity = "0..1")
    protected boolean resetSinkArtifacts;

    @CommandLine.Option(names = {"--sink-artifacts"}, description = "The sink artifacts", arity = "0..*")
    protected List<String> sinkArtifacts;

    @CommandLine.Option(names = {"--reset-sink-transformations"}, description = "Whether to clear existing transformations", defaultValue = "false", arity = "0..1")
    protected boolean resetTransformations;

    @CommandLine.Option(names = {"--transformations"}, description = "The artifacts", arity = "0..*")
    protected List<String> transformations;

    @Override
    public void run() {
        try {
            final File projectDir = new File(path);
            final PipelineProject project = ProjectHelper.loadProject(projectDir);

            configureIf(registry, project.getRegistry()::setName);
            configureIf(organization, project.getOrganization()::setName);
            configureIf(buildCommand, project.getCode().getLifecycle().getBuild()::setCommand);
            configureIf(cleanCommand, project.getCode().getLifecycle().getClean()::setCommand);
            configureIf(sourceFile, project.getSource()::setFile);
            configureIf(sourceImage, project.getSource().getImage()::setName);
            addOrSetSourceArtifacts(project);

            configureIf(sinkFile, project.getSink()::setFile);
            configureIf(sinkImage, project.getSink().getImage()::setName);
            addOrSetSinkArtifacts(project);

            ObjectMapper mapper = new ObjectMapper();

            File projectFile = ProjectHelper.getProjectFile(projectDir);
            mapper.writerWithDefaultPrettyPrinter().writeValue(projectFile, project);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addOrSetSourceArtifacts(PipelineProject project) {
        if (resetSourceArtifacts) {
            project.getTransformations().clear();
        }

        if (transformations != null) {
            for (String transformation : transformations) {
                configureIf(transformation, (t) -> {
                    final Transformation bean = new Transformation();
                    bean.setName(transformation);
                    project.getTransformations().add(bean);
                });
            }
        }
    }

    private void addOrSetSinkArtifacts(PipelineProject project) {
        if (resetSinkArtifacts) {
            project.getSink().getArtifacts().clear();
        }

        if (sinkArtifacts != null) {

            for (String sinkArtifact : sinkArtifacts) {
                configureIf(sinkArtifact, project.getSink().getArtifacts()::add);
            }
        }
    }

    private void addOrSetTransformations(PipelineProject project) {
        if (resetTransformations) {
            project.getSink().getArtifacts().clear();
        }

        if (sinkArtifacts != null) {

            for (String sinkArtifact : sinkArtifacts) {
                configureIf(sinkArtifact, project.getSink().getArtifacts()::add);
            }
        }
    }

    public <T> void configureIf(T source, Consumer<T> consumer) {
        if (source != null) {
            consumer.accept(source);
        }
    }

    public void configure(Registry bean) {
        bean.setName(registry);
    }

    public void configure(Organization bean) {
        bean.setName(organization);
    }

}
