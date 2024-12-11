package org.brainstorm.pipeline;

import java.util.Objects;

import org.brainstorm.api.pipeline.acquisition.AcquisitionStep;
import org.brainstorm.api.pipeline.infra.PipelineInfra;
import org.brainstorm.api.pipeline.transformation.TransformationSteps;

public class PipelineSpec {
    private PipelineInfra pipelineInfra;
    private AcquisitionStep acquisitionStep;
    private TransformationSteps transformationSteps;

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

    public TransformationSteps getTransformationSteps() {
        return transformationSteps;
    }

    public void setTransformationSteps(TransformationSteps transformationSteps) {
        this.transformationSteps = transformationSteps;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipelineSpec that = (PipelineSpec) o;
        return Objects.equals(pipelineInfra, that.pipelineInfra) && Objects.equals(acquisitionStep,
                that.acquisitionStep) && Objects.equals(transformationSteps, that.transformationSteps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineInfra, acquisitionStep, transformationSteps);
    }

    @Override
    public String toString() {
        return "PipelineSpec{" +
                "pipelineInfra=" + pipelineInfra +
                ", acquisitionStep=" + acquisitionStep +
                ", transformationSteps=" + transformationSteps +
                '}';
    }
}