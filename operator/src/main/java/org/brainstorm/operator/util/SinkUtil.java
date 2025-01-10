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
import org.brainstorm.api.pipeline.sink.SinkStep;
import org.brainstorm.pipeline.Pipeline;
import org.brainstorm.pipeline.PipelineReconciler;
import org.jboss.logging.Logger;

import static org.brainstorm.operator.util.Constants.classpathPath;
import static org.brainstorm.operator.util.Constants.sinkRoutePath;

/**
 * Utilities for handling the sink jobs
 */
public final class SinkUtil {
    private static final Logger LOG = Logger.getLogger(SinkUtil.class);
    private static final String TEMPLATE_FILE = "sink-worker-job.yaml";

    private SinkUtil() {}

    private static void setupContainer(Pipeline pipeline, JobSpec spec) {
        final SinkStep sinkStep = pipeline.getSpec().getSinkStep();
        if (sinkStep == null) {
            LOG.warnf("Invalid sinkStep %s", pipeline);
            return;
        }

        final List<Container> containers = spec
                .getTemplate()
                .getSpec()
                .getContainers();

        final Container runner = containers.stream().filter(c -> c.getName().equals("sink-runner")).findFirst().get();

        final String image = pipeline.getSpec().getSinkStep().getImage();
        LOG.infof("Building a new sink container using %s", image);
        runner.setImage(image);

        EnvVar dataDir = new EnvVarBuilder().withName("WORKER_CP").withValue(classpathPath()).build();
        runner.setEnv(List.of(dataDir));

        runner
                .setCommand(List.of("/opt/brainstorm/worker/run.sh",
                        "-s", pipeline.getSpec().getPipelineInfra().getBootstrapServer(),
                        "--step", sinkRoutePath(),
                        "--consumes-from", sinkStep.getConsumesFrom(),
                        "--wait"));
    }

    public static Job makeDesiredSinkDeployment(
            Pipeline pipeline, String deploymentName, String ns,
            String configMapName) {
        Job desiredJob =
                ReconcilerUtils.loadYaml(Job.class, PipelineReconciler.class, TEMPLATE_FILE);

        desiredJob.getMetadata().setName("sink-job");
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
