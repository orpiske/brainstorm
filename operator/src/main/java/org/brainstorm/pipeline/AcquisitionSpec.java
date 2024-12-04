package org.brainstorm.pipeline;

import org.brainstorm.api.pipeline.acquisition.AcquisitionStep;
import org.brainstorm.api.pipeline.infra.PipelineInfra;

public class AcquisitionSpec {
    private PipelineInfra pipelineInfra;
    private AcquisitionStep acquisitionStep;

    public PipelineInfra getPipelineInfra() {
        return pipelineInfra;
    }

    public void setPipelineInfra(PipelineInfra pipelineInfra) {
        this.pipelineInfra = pipelineInfra;
    }

    public AcquisitionStep getAcquisitionStep() {
        return acquisitionStep;
    }

    public void setAcquisitionStep(AcquisitionStep acquisitionStep) {
        this.acquisitionStep = acquisitionStep;
    }

    @Override
    public String toString() {
        return "AcquisitionSpec{" +
                "acquisitionStep=" + acquisitionStep +
                '}';
    }
}