package org.brainstorm.pipeline;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import org.jboss.logging.Logger;

public class AcquisitionReconciler implements Reconciler<Acquisition> {
    private static final Logger LOG = Logger.getLogger(AcquisitionReconciler.class);

    @Override
    public UpdateControl<Acquisition> reconcile(Acquisition resource, Context<Acquisition> context) {
        LOG.infof("Starting reconciliation for %s", resource.getMetadata().getName());
        final AcquisitionSpec spec = resource.getSpec();

        if (spec == null) {
            LOG.warnf("No spec found for %s", resource.getMetadata().getName());
            return UpdateControl.noUpdate();
        } else {
            LOG.infof("Using spec: %s", spec);
        }

        return UpdateControl.noUpdate();
    }
}