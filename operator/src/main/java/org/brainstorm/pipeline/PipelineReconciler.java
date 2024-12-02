package org.brainstorm.pipeline;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class PipelineReconciler implements Reconciler<Pipeline> {
    @Override
    public UpdateControl<Pipeline> reconcile(Pipeline resource, Context<Pipeline> context) {
        return UpdateControl.noUpdate();
    }
}