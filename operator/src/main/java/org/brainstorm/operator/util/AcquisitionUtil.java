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
import org.brainstorm.api.pipeline.acquisition.AcquisitionStep;
import org.brainstorm.pipeline.Acquisition;
import org.brainstorm.pipeline.AcquisitionReconciler;
import org.jboss.logging.Logger;

import static org.brainstorm.operator.util.Constants.classpathPath;
import static org.brainstorm.operator.util.Constants.routePath;

/**
 * Utilities for handling the acquisition jobs
 */
public final class AcquisitionUtil {
    private static final Logger LOG = Logger.getLogger(AcquisitionUtil.class);
    private static final String TEMPLATE_FILE = "acquisition-worker-job.yaml";

    private AcquisitionUtil() {}

    private static void setupContainer(Acquisition acquisition, JobSpec spec) {
        final AcquisitionStep acquisitionStep = acquisition.getSpec().getAcquisitionStep();
        if (acquisitionStep == null) {
            LOG.warnf("Invalid acquisition %s", acquisition);
            return;
        }

        final List<Container> containers = spec
                .getTemplate()
                .getSpec()
                .getContainers();

        final Container runner = containers.stream().filter(c -> c.getName().equals("camel-runner")).findFirst().get();

        final String image = acquisition.getSpec().getAcquisitionStep().getImage();
        LOG.infof("Building a new acquisition container using %s", image);
        runner.setImage(image);

        EnvVar dataDir = new EnvVarBuilder().withName("WORKER_CP").withValue(classpathPath()).build();
        runner.setEnv(List.of(dataDir));

        runner
                .setCommand(List.of("/opt/brainstorm/worker/run.sh",
                        "-s", acquisition.getSpec().getPipelineInfra().getBootstrapServer(),
                        "--file", routePath(),
                        "--produces-to", acquisitionStep.getProducesTo(),
                        "--wait"));
    }

    public static Job makeDesiredAcquisitionDeployment(
            Acquisition acquisition, String deploymentName, String ns,
            String configMapName) {
        Job desiredJob =
                ReconcilerUtils.loadYaml(Job.class, AcquisitionReconciler.class, TEMPLATE_FILE);

        desiredJob.getMetadata().setName(deploymentName);
        desiredJob.getMetadata().setNamespace(ns);

        final JobSpec jobSpec = desiredJob.getSpec();

        jobSpec.getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build());

        desiredJob.addOwnerReference(acquisition);

        setupContainer(acquisition, jobSpec);

        return desiredJob;
    }
}
