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
import java.util.Map;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import org.brainstorm.pipeline.Pipeline;
import org.brainstorm.pipeline.PipelineReconciler;
import org.jboss.logging.Logger;

import static org.brainstorm.operator.util.Constants.DATA_DIR;

/**
 * Handles the backend service
 */
public final class BackendServiceUtil {
    private static final Logger LOG = Logger.getLogger(BackendServiceUtil.class);
    private static final String RESOURCE_FILE = "service-external-service.yaml";
    private static final String SERVICE_RESOURCE_FILE = "service-deployment.yaml";

    private BackendServiceUtil() {}

    private static void setupBackendContainer(Pipeline pipeline, DeploymentSpec spec) {
        final List<Container> containers = spec
                .getTemplate()
                .getSpec()
                .getContainers();

        final Container service = containers
                .stream().filter(c -> c.getName().equals("service")).findFirst().get();

        service.setCommand(List.of("/opt/jboss/container/java/run/run-java.sh"));

        EnvVar dataDir = new EnvVarBuilder().withName("DATA_DIR").withValue(DATA_DIR).build();
        EnvVar bootstrapHost = new EnvVarBuilder().withName("BOOTSTRAP_HOST")
                .withValue(pipeline.getSpec().getPipelineInfra().getBootstrapServer()).build();

        service.setEnv(List.of(dataDir, bootstrapHost));
    }

    public static Deployment makeDesiredBackendServiceDeployment(
            Pipeline pipeline, String deploymentName, String ns,
            String configMapName) {
        Deployment desiredServiceDeployment =
                ReconcilerUtils.loadYaml(Deployment.class, PipelineReconciler.class, SERVICE_RESOURCE_FILE);

        desiredServiceDeployment.getMetadata().setName(deploymentName);
        desiredServiceDeployment.getMetadata().setNamespace(ns);

        final DeploymentSpec serviceSpec = desiredServiceDeployment.getSpec();

        serviceSpec.getSelector().getMatchLabels().put("app", deploymentName);
        serviceSpec.getSelector().getMatchLabels().put("component", "service");
        serviceSpec.getTemplate().getMetadata().getLabels().put("app", deploymentName);
        serviceSpec.getTemplate().getMetadata().getLabels().put("component", "service");
        serviceSpec.getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build());

        desiredServiceDeployment.addOwnerReference(pipeline);
        setupBackendContainer(pipeline, serviceSpec);

        return desiredServiceDeployment;
    }

    public static Service makeServiceExternalService(Pipeline pipeline, String deploymentName, String ns) {
        Service service = ReconcilerUtils.loadYaml(Service.class, PipelineReconciler.class, RESOURCE_FILE);

        LOG.infof("Creating new external service for deployment: %s", deploymentName);
        service.getMetadata().setName("external-" + deploymentName);
        service.getMetadata().setNamespace(ns);

        ServiceSpec serviceSpec = service.getSpec();
        serviceSpec.setSelector(Map.of("app", deploymentName, "component", "service"));

        service.addOwnerReference(pipeline);

        return service;
    }
}
