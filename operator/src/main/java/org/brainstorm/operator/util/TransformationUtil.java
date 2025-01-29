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
import org.brainstorm.core.api.pipeline.transformation.TransformationStep;
import org.brainstorm.core.api.pipeline.transformation.TransformationSteps;
import org.brainstorm.core.api.util.EnvironmentVariables;
import org.brainstorm.pipeline.Pipeline;
import org.brainstorm.pipeline.PipelineReconciler;
import org.jboss.logging.Logger;

import static org.brainstorm.operator.util.Constants.getTransformationStep;

/**
 * Utilities for handling the transformation jobs
 */
public final class TransformationUtil {
    private static final Logger LOG = Logger.getLogger(TransformationUtil.class);
    private static final String RESOURCE_FILE = "transformer-worker-job.yaml";
    public static final String TRANSFORMER_WORKER_CONTAINER_NAME = "transformer-worker";

    private TransformationUtil() {}

    private static void setupContainer(
            Pipeline pipeline, JobSpec spec, TransformationStep transformationStep) {
        final TransformationSteps transformationSteps = pipeline.getSpec().getTransformationSteps();
        if (transformationSteps == null) {
            LOG.warnf("Invalid transformation steps for pipeline  %s", pipeline);
            return;
        }

        final List<Container> containers = spec
                .getTemplate()
                .getSpec()
                .getContainers();

        final Container runner = containers.stream().filter(c -> c.getName().equals(TRANSFORMER_WORKER_CONTAINER_NAME)).findFirst().get();

        final String image = transformationStep.getImage();
        LOG.infof("Building a new pipeline container using %s", image);
        runner.setImage(image);

        final String step = getTransformationStep(transformationStep);

        final List<EnvVar> envVars = buildEnvironment(pipeline, step);

        runner.setEnv(envVars);
    }

    private static List<EnvVar> buildEnvironment(Pipeline pipeline, String step) {
        EnvVar bootstrapHost = new EnvVarBuilder().withName(EnvironmentVariables.BOOTSTRAP_HOST)
                .withValue(pipeline.getSpec().getPipelineInfra().getBootstrapServer()).build();
        EnvVar bootstrapPort = new EnvVarBuilder().withName(EnvironmentVariables.BOOTSTRAP_PORT)
                .withValue(String.valueOf(pipeline.getSpec().getPipelineInfra().getPort())).build();
        EnvVar stepEnv = new EnvVarBuilder().withName(EnvironmentVariables.STEP).withValue(step).build();
        EnvVar consumesFrom = new EnvVarBuilder().withName(EnvironmentVariables.CONSUMES_FROM).withValue(TopicNameGenerator.getInstance().current()).build();
        EnvVar producesTo = new EnvVarBuilder().withName(EnvironmentVariables.PRODUCES_TO).withValue(TopicNameGenerator.getInstance().next()).build();
        EnvVar dataDirectory = new EnvVarBuilder().withName(EnvironmentVariables.DATA_DIRECTORY).withValue(Constants.DATA_DIR).build();


        return List.of(bootstrapHost, bootstrapPort, stepEnv, consumesFrom, producesTo, dataDirectory);
    }

    public static Job makeDesiredTransformationJob(
            Pipeline pipeline, String jobName, String ns,
            String configMapName, TransformationStep transformationStep) {
        Job desiredRunnerJob =
                ReconcilerUtils.loadYaml(Job.class, PipelineReconciler.class, RESOURCE_FILE);

        desiredRunnerJob.getMetadata().setName(jobName);
        desiredRunnerJob.getMetadata().setNamespace(ns);

        final JobSpec runnerSpec = desiredRunnerJob.getSpec();

        runnerSpec.getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build());

        desiredRunnerJob.addOwnerReference(pipeline);

        setupContainer(pipeline, runnerSpec, transformationStep);

        return desiredRunnerJob;
    }
}
