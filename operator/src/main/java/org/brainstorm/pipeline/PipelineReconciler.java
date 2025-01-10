package org.brainstorm.pipeline;

import java.util.List;

import jakarta.inject.Inject;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Replaceable;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.brainstorm.api.pipeline.transformation.TransformationStep;
import org.brainstorm.operator.util.TopicGenerator;
import org.jboss.logging.Logger;

import static org.brainstorm.operator.util.Constants.CONFIG_MAP_NAME;
import static org.brainstorm.operator.util.SourceUtil.makeDesiredSourceDeployment;
import static org.brainstorm.operator.util.BackendServiceUtil.makeDesiredBackendServiceDeployment;
import static org.brainstorm.operator.util.BackendServiceUtil.makeServiceExternalService;
import static org.brainstorm.operator.util.Matchers.match;
import static org.brainstorm.operator.util.SinkUtil.makeDesiredSinkDeployment;
import static org.brainstorm.operator.util.TransformationUtil.makeDesiredTransformationJob;

public class PipelineReconciler implements Reconciler<Pipeline> {
    private static final Logger LOG = Logger.getLogger(PipelineReconciler.class);


    @Inject
    KubernetesClient kubernetesClient;

    /**
     * Main reconciliation method. Everything starts here.
     * @param resource the resource that has been created or updated
     * @param context the context with which the operation is executed
     * @return
     */
    @Override
    public UpdateControl<Pipeline> reconcile(Pipeline resource, Context<Pipeline> context) {
        LOG.infof("Starting reconciliation for %s", resource.getMetadata().getName());
        TopicGenerator.getInstance().reset();

        final PipelineSpec spec = resource.getSpec();

        if (spec == null) {
            LOG.warnf("No spec found for %s", resource.getMetadata().getName());
            return UpdateControl.noUpdate();
        }

        LOG.infof("Using spec: %s", spec);

        String ns = resource.getMetadata().getNamespace();
        String deploymentName = resource.getMetadata().getName();

        deployService(resource, context, "service", ns);
        deploySourceRunner(resource, context, deploymentName, ns);
        deployTransformations(resource, context, deploymentName, ns);
        deploySinkRunner(resource, context, deploymentName, ns);

        return UpdateControl.noUpdate();
    }

    private void deploySourceRunner(Pipeline resource, Context<Pipeline> context, String deploymentName, String ns) {
        final Job desiredJob = makeDesiredSourceDeployment(resource, deploymentName, ns,
                CONFIG_MAP_NAME);
        try {
            Job existingJob = context.getSecondaryResource(Job.class).orElse(null);

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

    private void deployTransformations(Pipeline resource, Context<Pipeline> context, String deploymentName, String ns) {
        final List<TransformationStep> steps = resource.getSpec().getTransformationSteps().getSteps();

        for (TransformationStep step : steps) {
            final Job desiredJob = makeDesiredTransformationJob(resource, step.getName(), ns,
                    CONFIG_MAP_NAME, step);

            try {
                Job existingJob = context.getSecondaryResource(Job.class).orElse(null);

                if (!match(desiredJob, existingJob)) {
                    LOG.infof("Creating or updating transformation job %s in %s", desiredJob.getMetadata().getName(), ns);

                    kubernetesClient.batch().v1().jobs().inNamespace(ns).resource(desiredJob)
                            .serverSideApply();
                }

            } catch (Exception e) {
                LOG.warnf("Creating or updating transformation job named %s", desiredJob.getMetadata().getName());
                kubernetesClient.batch().v1().jobs().inNamespace(ns).resource(desiredJob)
                        .serverSideApply();
            }
        }
    }

    private void deployService(Pipeline resource, Context<Pipeline> context, String deploymentName, String ns) {
        final Deployment desiredDeployment = makeDesiredBackendServiceDeployment(resource, deploymentName, ns,
                CONFIG_MAP_NAME);

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

    private void deploySinkRunner(Pipeline resource, Context<Pipeline> context, String deploymentName, String ns) {
        final Job desiredJob = makeDesiredSinkDeployment(resource, deploymentName, ns,
                CONFIG_MAP_NAME);
        try {
            Job existingJob = context.getSecondaryResource(Job.class).orElse(null);

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
}