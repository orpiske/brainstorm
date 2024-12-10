package org.brainstorm.pipeline;

import java.io.File;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentSpec;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Replaceable;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.brainstorm.api.pipeline.acquisition.AcquisitionStep;
import org.brainstorm.api.pipeline.transformation.TransformationStep;
import org.brainstorm.api.pipeline.transformation.TransformationSteps;
import org.jboss.logging.Logger;

public class AcquisitionReconciler implements Reconciler<Acquisition> {
    private static final Logger LOG = Logger.getLogger(AcquisitionReconciler.class);
    public static final String BASE_DIR = "/opt/brainstorm";
    public static final String CLASSPATH_DIR = BASE_DIR + "/classpath";
    public static final String DATA_DIR = BASE_DIR + "/data";
    public static final String ACQUISITION_DIR = BASE_DIR + "/acquisition";
    public static final String STEP_DIR = BASE_DIR + "/step";
    public static final String DEFAULT_TRANSFORM_SCRIPT_NAME = "transform.sh";

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
        deployAcquisitionRunner(resource, context, deploymentName, ns);
        deployTransformations(resource, context, deploymentName, ns);

        return UpdateControl.noUpdate();
    }

    private void deployAcquisitionRunner(Acquisition resource, Context<Acquisition> context, String deploymentName, String ns) {
        final Job desiredJob = makeDesiredAcquisitionDeployment(resource, deploymentName, ns,
                "bs-config");

        Job existingJob;
        try {
            existingJob = context.getSecondaryResource(Job.class).orElse(null);

            if (!match(desiredJob, existingJob)) {
                LOG.infof("Creating or updating job %s in %s", desiredJob.getMetadata().getName(), ns);

                kubernetesClient.batch().v1().jobs().inNamespace(ns).resource(desiredJob)
                        .serverSideApply();
            }
        } catch (IllegalArgumentException e) {
            LOG.warnf("Creating or updating job named %s", desiredJob.getMetadata().getName());
            kubernetesClient.batch().v1().jobs().inNamespace(ns).resource(desiredJob)
                    .serverSideApply();
        }
    }

    private void deployTransformations(Acquisition resource, Context<Acquisition> context, String deploymentName, String ns) {
        final List<TransformationStep> steps = resource.getSpec().getTransformationSteps().getSteps();

        for (TransformationStep step : steps) {
            final Deployment desiredDeployment = makeDesiredTransformationDeployment(resource, step.getName(), ns,
                    "bs-config", step);

            Deployment existingDeployment;
            try {
                existingDeployment = context.getSecondaryResource(Deployment.class).orElse(null);
            } catch (Exception e) {
                LOG.warnf("There is no existing deployment");
                existingDeployment = null;
            }

            if (!match(desiredDeployment, existingDeployment)) {
                LOG.infof("Creating or updating Deployment %s in %s", desiredDeployment.getMetadata().getName(), ns);

                kubernetesClient.apps().deployments().inNamespace(ns).resource(desiredDeployment)
                        .createOr(Replaceable::update);
            }
        }
    }

    private void deployService(Acquisition resource, Context<Acquisition> context, String deploymentName, String ns) {
        final Deployment desiredDeployment = makeDesiredServiceDeployment(resource, deploymentName, ns,
                "bs-config");

        Deployment existingDeployment;
        try {
            existingDeployment = context.getSecondaryResource(Deployment.class).orElse(null);
        } catch (Exception e) {
            LOG.warnf("There is no existing deployment");
            existingDeployment = null;
        }

        if (!match(desiredDeployment, existingDeployment)) {
            LOG.infof("Creating or updating Deployment %s in %s", desiredDeployment.getMetadata().getName(), ns);

            kubernetesClient.apps().deployments().inNamespace(ns).resource(desiredDeployment)
                    .createOr(Replaceable::update);
        }

        final Service desiredExternalService = makeServiceExternalService(resource, deploymentName, ns);
        Service existingExternalService;
        try {
            existingExternalService = context.getSecondaryResource(Service.class).orElse(null);
        } catch (Exception e) {
            LOG.warnf("There is no existing service");
            existingExternalService = null;
        }
        if (!match(desiredExternalService, existingExternalService)) {
            LOG.infof("Creating or updating Service %s in %s", desiredExternalService.getMetadata().getName(), ns);

            kubernetesClient.services().inNamespace(ns).resource(desiredExternalService)
                    .createOr(Replaceable::update);
        }
    }

    private Deployment makeDesiredServiceDeployment(
            Acquisition acquisition, String deploymentName, String ns,
            String configMapName) {
        Deployment desiredServiceDeployment =
                ReconcilerUtils.loadYaml(Deployment.class, getClass(), "service-deployment.yaml");

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

        desiredServiceDeployment.addOwnerReference(acquisition);
        setupBackendContainer(acquisition, serviceSpec);

        return desiredServiceDeployment;
    }

    private Service makeServiceExternalService(Acquisition acquisition, String deploymentName, String ns) {
        Service service = ReconcilerUtils.loadYaml(Service.class, getClass(), "service-external-service.yaml");

        LOG.infof("Creating new external service for deployment: %s", deploymentName);
        service.getMetadata().setName("external-" + deploymentName);
        service.getMetadata().setNamespace(ns);

        ServiceSpec serviceSpec = service.getSpec();
        serviceSpec.setSelector(Map.of("app", deploymentName, "component", "service"));

        service.addOwnerReference(acquisition);

        return service;
    }

    private Job makeDesiredAcquisitionDeployment(
            Acquisition acquisition, String deploymentName, String ns,
            String configMapName) {
        Job desiredJob =
                ReconcilerUtils.loadYaml(Job.class, getClass(), "acquisition-worker-job.yaml");

        desiredJob.getMetadata().setName(deploymentName);
        desiredJob.getMetadata().setNamespace(ns);

        final JobSpec jobSpec = desiredJob.getSpec();

        jobSpec.getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build());

        desiredJob.addOwnerReference(acquisition);

        setupAcquisitionContainer(acquisition, jobSpec);

        return desiredJob;
    }

    private static void setupAcquisitionContainer(Acquisition acquisition, JobSpec spec) {
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

    private Deployment makeDesiredTransformationDeployment(
            Acquisition acquisition, String deploymentName, String ns,
            String configMapName, TransformationStep transformationStep) {
        Deployment desiredRunnerDeployment =
                ReconcilerUtils.loadYaml(Deployment.class, getClass(), "runner-worker-deployment.yaml");

        desiredRunnerDeployment.getMetadata().setName(deploymentName);
        desiredRunnerDeployment.getMetadata().setNamespace(ns);

        final DeploymentSpec runnerSpec = desiredRunnerDeployment.getSpec();

        runnerSpec.getSelector().getMatchLabels().put("app", deploymentName);
        runnerSpec.getSelector().getMatchLabels().put("component", "runner-worker");
        runnerSpec.getSelector().getMatchLabels().put("step", transformationStep.getConsumesFrom());
        runnerSpec.getTemplate().getMetadata().getLabels().put("app", deploymentName);
        runnerSpec.getTemplate().getMetadata().getLabels().put("component", "runner-worker");
        runnerSpec.getTemplate().getMetadata().getLabels().put("step", transformationStep.getConsumesFrom());

        runnerSpec.getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build());

        desiredRunnerDeployment.addOwnerReference(acquisition);

        setupTransformationContainer(acquisition, runnerSpec, transformationStep);

        return desiredRunnerDeployment;
    }

    private static void setupTransformationContainer(
            Acquisition acquisition, DeploymentSpec spec, TransformationStep transformationStep) {
        final TransformationSteps transformationSteps = acquisition.getSpec().getTransformationSteps();
        if (transformationSteps == null) {
            LOG.warnf("Invalid transformation steps for acquisition  %s", acquisition);
            return;
        }

        final List<Container> containers = spec
                .getTemplate()
                .getSpec()
                .getContainers();

        final Container runner = containers.stream().filter(c -> c.getName().equals("runner-worker")).findFirst().get();

        final String image = transformationStep.getImage();
        LOG.infof("Building a new acquisition container using %s", image);
        runner.setImage(image);

        final String script = getTransformationScript(transformationStep);

        runner
                .setCommand(List.of("/opt/brainstorm/worker/run.sh",
                        "-s", acquisition.getSpec().getPipelineInfra().getBootstrapServer(),
                        "--script", script,
                        "--consumes-from", transformationStep.getConsumesFrom(),
                        "--produces-to", transformationStep.getProducesTo()));
    }

    private static String getTransformationScript(TransformationStep transformationStep) {
        String script = transformationStep.getScript();
        if (script == null || script.isEmpty()) {
            script = DEFAULT_TRANSFORM_SCRIPT_NAME;
        }
        return stepPath(script);
    }

    private static String stepPath(String script) {
        return STEP_DIR + File.separator + script;
    }

    private static String classpathPath() {
        return CLASSPATH_DIR;
    }

    private static String routePath() {
        return ACQUISITION_DIR + "/routes.yaml";
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
        EnvVar bootstrapHost = new EnvVarBuilder().withName("BOOTSTRAP_HOST")
                .withValue(acquisition.getSpec().getPipelineInfra().getBootstrapServer()).build();

        service.setEnv(List.of(dataDir, bootstrapHost));
    }

    private boolean match(Job desiredJob, Job existingJob) {
        if (existingJob == null) {
            return false;
        } else {
            return desiredJob.getSpec().getTemplate().getMetadata().getName()
                    .equals(existingJob.getSpec().getTemplate().getMetadata().getName()) &&
                    desiredJob.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()
                            .equals(
                                    existingJob.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
        }
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

    public boolean match(Service desiredService, Service existingService) {
        if (existingService == null) {
            return false;
        }

        final ServiceSpec existingSpec = existingService.getSpec();
        final ServiceSpec desiredSpec = desiredService.getSpec();

        return existingSpec.getExternalName().equals(desiredSpec.getExternalName())
                && existingSpec.getPorts().equals(desiredSpec.getPorts());

    }
}