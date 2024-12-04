package org.brainstorm.pipeline;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Replaceable;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.brainstorm.api.pipeline.acquisition.AcquisitionStep;
import org.jboss.logging.Logger;

public class AcquisitionReconciler implements Reconciler<Acquisition> {
    private static final Logger LOG = Logger.getLogger(AcquisitionReconciler.class);
    public static final String DATA_DIR = "/opt/brainstorm/data";

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public UpdateControl<Acquisition> reconcile(Acquisition resource, Context<Acquisition> context) {
        LOG.infof("Starting reconciliation for %s", resource.getMetadata().getName());
        final AcquisitionSpec spec = resource.getSpec();

        if (spec == null) {
            LOG.warnf("No spec found for %s", resource.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        LOG.infof("Using spec: %s", spec);

        String ns = resource.getMetadata().getNamespace();
        String deploymentName = resource.getMetadata().getName();

        deployService(resource, context, "service", ns);
        deployRunner(resource, context, deploymentName, ns);

        return UpdateControl.noUpdate();
    }

    private void deployRunner(Acquisition resource, Context<Acquisition> context, String deploymentName, String ns) {
        final Deployment desiredDeployment = makeDesiredAcquisitionDeployment(resource, deploymentName, ns,
                "bs-config");

        Deployment existingDeployment;
        try {
             existingDeployment = context.getSecondaryResource(Deployment.class).orElse(null);
        } catch (Exception e) {
            LOG.warnf("There is not existing deployment");
            existingDeployment = null;
        }

        if (!match(desiredDeployment, existingDeployment)) {
            LOG.infof("Creating or updating Deployment %s in %s", desiredDeployment.getMetadata().getName(), ns);

            kubernetesClient.apps().deployments().inNamespace(ns).resource(desiredDeployment)
                    .createOr(Replaceable::update);
        }
    }

    private void deployService(Acquisition resource, Context<Acquisition> context, String deploymentName, String ns) {
        final Deployment desiredDeployment = makeDesiredServiceDeployment(resource, deploymentName, ns,
                "bs-config");

        Deployment existingDeployment;
        try {
            existingDeployment = context.getSecondaryResource(Deployment.class).orElse(null);
        } catch (Exception e) {
            LOG.warnf("There is not existing deployment");
            existingDeployment = null;
        }

        if (!match(desiredDeployment, existingDeployment)) {
            LOG.infof("Creating or updating Deployment %s in %s", desiredDeployment.getMetadata().getName(), ns);

            kubernetesClient.apps().deployments().inNamespace(ns).resource(desiredDeployment)
                    .createOr(Replaceable::update);
        }
    }

    private Deployment makeDesiredServiceDeployment(Acquisition acquisition, String deploymentName, String ns,
            String configMapName) {
        Deployment desiredServiceDeployment =
                ReconcilerUtils.loadYaml(Deployment.class, getClass(), "acquisition-service-deployment.yaml");

        desiredServiceDeployment.getMetadata().setName(deploymentName);
        desiredServiceDeployment.getMetadata().setNamespace(ns);

        final DeploymentSpec serviceSpec = desiredServiceDeployment.getSpec();

        serviceSpec.getSelector().getMatchLabels().put("app", deploymentName);
        serviceSpec.getTemplate().getMetadata().getLabels().put("app", deploymentName);
        serviceSpec.getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build());

        desiredServiceDeployment.addOwnerReference(acquisition);

        setupBackendContainer(acquisition, serviceSpec);

        return desiredServiceDeployment;
    }

    private Deployment makeDesiredAcquisitionDeployment(Acquisition acquisition, String deploymentName, String ns,
            String configMapName) {
        Deployment desiredRunnerDeployment =
                ReconcilerUtils.loadYaml(Deployment.class, getClass(), "acquisition-worker-deployment.yaml");

        desiredRunnerDeployment.getMetadata().setName(deploymentName);
        desiredRunnerDeployment.getMetadata().setNamespace(ns);

        final DeploymentSpec runnerSpec = desiredRunnerDeployment.getSpec();

        runnerSpec.getSelector().getMatchLabels().put("app", deploymentName);
        runnerSpec.getTemplate().getMetadata().getLabels().put("app", deploymentName);
        runnerSpec.getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build());

        desiredRunnerDeployment.addOwnerReference(acquisition);

        setupAcquisitionContainer(acquisition, runnerSpec);

        return desiredRunnerDeployment;
    }

    private static void setupAcquisitionContainer(Acquisition acquisition, DeploymentSpec spec) {
        final AcquisitionStep acquisitionStep = acquisition.getSpec().getAcquisitionStep();
        if (acquisitionStep == null) {
            LOG.warnf("Invalid acquisition %s", acquisition);
            return;
        }

        String dependencies = acquisitionStep.getDependencies().stream().collect(Collectors.joining(","));
        final List<Container> containers = spec
                .getTemplate()
                .getSpec()
                .getContainers();

        final Container runner = containers.stream().filter(c -> c.getName().equals("camel-runner")).findFirst().get();

        runner
                .setCommand(List.of("/opt/brainstorm/worker/run.sh", "-s", acquisition.getSpec().getPipelineInfra().getBootstrapServer(),
                        "-f", DATA_DIR + "/route.yaml",
                        "-d", dependencies,
                        "--produces-to", acquisitionStep.getProducesTo()));
    }


    private static void setupBackendContainer(Acquisition acquisition, DeploymentSpec spec) {
        final List<Container> containers = spec
                .getTemplate()
                .getSpec()
                .getContainers();

        final Container service = containers
                .stream().filter(c -> c.getName().equals("service")).findFirst().get();


        service.setCommand(List.of("/opt/jboss/container/java/run/run-java.sh"));

        EnvVar dataDir = new EnvVarBuilder().withName("DATA_DIR").withValue(DATA_DIR).build();
        EnvVar bootstrapHost = new EnvVarBuilder().withName("BOOTSTRAP_HOST").withValue(acquisition.getSpec().getPipelineInfra().getBootstrapServer()).build();

        service.setEnv(List.of(dataDir, bootstrapHost));
    }

    private boolean match(Deployment desiredDeployment, Deployment deployment) {
        if (deployment == null) {
            return false;
        } else {
            return desiredDeployment.getSpec().getReplicas().equals(deployment.getSpec().getReplicas()) &&
                    desiredDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()
                            .equals(
                                    deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
        }
    }
}