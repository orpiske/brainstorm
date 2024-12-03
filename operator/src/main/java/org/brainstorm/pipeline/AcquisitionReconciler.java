package org.brainstorm.pipeline;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import io.fabric8.kubernetes.api.model.ConfigMapVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Replaceable;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.jboss.logging.Logger;

public class AcquisitionReconciler implements Reconciler<Acquisition> {
    private static final Logger LOG = Logger.getLogger(AcquisitionReconciler.class);

    @Inject
    KubernetesClient kubernetesClient;

    @Override
    public UpdateControl<Acquisition> reconcile(Acquisition resource, Context<Acquisition> context) {
        LOG.infof("Starting reconciliation for %s", resource.getMetadata().getName());
        final AcquisitionSpec spec = resource.getSpec();

        if (spec == null) {
            LOG.warnf("No spec found for %s", resource.getMetadata().getName());
            return UpdateControl.noUpdate();
        } else {
            LOG.infof("Using spec: %s", spec);

            String ns = resource.getMetadata().getNamespace();
            String deploymentName = resource.getMetadata().getName();

            final Deployment desiredDeployment = makeDesiredDeployment(resource, deploymentName, ns, "bs-config");

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

        return UpdateControl.noUpdate();
    }

    private Deployment makeDesiredDeployment(Acquisition acquisition, String deploymentName, String ns,
            String configMapName) {
        Deployment desiredDeployment =
                ReconcilerUtils.loadYaml(Deployment.class, getClass(), "deployment.yaml");

        desiredDeployment.getMetadata().setName(deploymentName);
        desiredDeployment.getMetadata().setNamespace(ns);
        desiredDeployment.getSpec().getSelector().getMatchLabels().put("app", deploymentName);
        desiredDeployment.getSpec().getTemplate().getMetadata().getLabels().put("app", deploymentName);
        desiredDeployment
                .getSpec()
                .getTemplate()
                .getSpec()
                .getVolumes()
                .get(0)
                .setConfigMap(new ConfigMapVolumeSourceBuilder().withName(configMapName).build());
        desiredDeployment.addOwnerReference(acquisition);

        String dependencies = acquisition.getSpec().getDependencies().stream().collect(Collectors.joining(","));
        desiredDeployment
                .getSpec()
                .getTemplate()
                .getSpec()
                .getContainers()
                .get(0)
                .setCommand(List.of("/opt/brainstorm/worker/run.sh", "-s", acquisition.getSpec().getBootstrapServer(),
                        "-f", acquisition.getSpec().getFile(),
                        "-d", dependencies,
                        "--produces-to", acquisition.getSpec().getProducesTo()));
        return desiredDeployment;
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