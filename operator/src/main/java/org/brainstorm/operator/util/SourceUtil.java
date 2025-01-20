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

package org.brainstorm.operator.util;

import java.util.List;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import org.brainstorm.core.api.pipeline.source.SourceStep;
import org.brainstorm.pipeline.Pipeline;
import org.brainstorm.pipeline.PipelineReconciler;
import org.jboss.logging.Logger;

import static org.brainstorm.operator.util.Constants.classpathPath;
import static org.brainstorm.operator.util.Constants.sourceRoutePath;

/**
 * Utilities for handling the source jobs
 */
public final class SourceUtil {
    private static final Logger LOG = Logger.getLogger(SourceUtil.class);
    private static final String TEMPLATE_FILE = "source-worker-job.yaml";

    private SourceUtil() {}

    private static void setupContainer(Pipeline pipeline, JobSpec spec) {
        final SourceStep sourceStep = pipeline.getSpec().getSourceStep();
        if (sourceStep == null) {
            LOG.warnf("Invalid pipeline %s", pipeline);
            return;
        }

        final List<Container> containers = spec
                .getTemplate()
                .getSpec()
                .getContainers();

        final Container runner = containers.stream().filter(c -> c.getName().equals("source-runner")).findFirst().get();

        final String image = pipeline.getSpec().getSourceStep().getImage();
        LOG.infof("Building a new pipeline container using %s", image);
        runner.setImage(image);

        final List<EnvVar> envVars = buildEnvironment(pipeline);

        runner.setEnv(envVars);
    }

    private static List<EnvVar> buildEnvironment(Pipeline pipeline) {
        EnvVar bootstrapHost = new EnvVarBuilder().withName("BOOTSTRAP_HOST")
                .withValue(pipeline.getSpec().getPipelineInfra().getBootstrapServer()).build();
        EnvVar bootstrapPort = new EnvVarBuilder().withName("BOOTSTRAP_PORT")
                .withValue(String.valueOf(pipeline.getSpec().getPipelineInfra().getPort())).build();
        EnvVar producesTo = new EnvVarBuilder().withName("PRODUCES_TO").withValue(TopicNameGenerator.getInstance().current()).build();

        EnvVar dataDirectory = new EnvVarBuilder().withName("DATA_DIRECTORY").withValue(Constants.DATA_DIR).build();
        EnvVar workerCp = new EnvVarBuilder().withName("WORKER_CP").withValue(classpathPath()).build();
        EnvVar routePath = new EnvVarBuilder().withName("SOURCE_ROUTE_PATH").withValue(sourceRoutePath()).build();

        return List.of(bootstrapHost, bootstrapPort, producesTo, dataDirectory, workerCp, routePath);
    }

    public static Job makeDesiredSourceDeployment(Pipeline pipeline, String ns, String configMapName) {
        Job desiredJob =
                ReconcilerUtils.loadYaml(Job.class, PipelineReconciler.class, TEMPLATE_FILE);

        desiredJob.getMetadata().setName("source-job");
        desiredJob.getMetadata().setNamespace(ns);

        final JobSpec jobSpec = desiredJob.getSpec();

        jobSpec.getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build());

        desiredJob.addOwnerReference(pipeline);

        setupContainer(pipeline, jobSpec);

        return desiredJob;
    }
}
