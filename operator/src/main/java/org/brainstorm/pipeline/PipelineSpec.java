package org.brainstorm.pipeline;

import java.util.Objects;

import org.brainstorm.core.api.pipeline.source.SourceStep;
import org.brainstorm.core.api.pipeline.infra.PipelineInfra;
import org.brainstorm.core.api.pipeline.sink.SinkStep;
import org.brainstorm.core.api.pipeline.transformation.TransformationSteps;

public class PipelineSpec {
    private PipelineInfra pipelineInfra;
    private SourceStep sourceStep;
    private TransformationSteps transformationSteps;
    private SinkStep sinkStep;

    public PipelineInfra getPipelineInfra() {
        return pipelineInfra;
    }

    public void setPipelineInfra(PipelineInfra pipelineInfra) {
        this.pipelineInfra = pipelineInfra;
    }

    public SourceStep getSourceStep() {
        return sourceStep;
    }

    public void setSourceStep(SourceStep sourceStep) {
        this.sourceStep = sourceStep;
    }

    public TransformationSteps getTransformationSteps() {
        return transformationSteps;
    }

    public void setTransformationSteps(TransformationSteps transformationSteps) {
        this.transformationSteps = transformationSteps;
    }

    public SinkStep getSinkStep() {
        return sinkStep;
    }

    public void setSinkStep(SinkStep sinkStep) {
        this.sinkStep = sinkStep;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PipelineSpec that = (PipelineSpec) o;
        return Objects.equals(pipelineInfra, that.pipelineInfra) && Objects.equals(sourceStep,
                that.sourceStep) && Objects.equals(transformationSteps,
                that.transformationSteps) && Objects.equals(sinkStep, that.sinkStep);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pipelineInfra, sourceStep, transformationSteps, sinkStep);
    }

    @Override
    public String toString() {
        return "PipelineSpec{" +
                "pipelineInfra=" + pipelineInfra +
                ", acquisitionStep=" + sourceStep +
                ", transformationSteps=" + transformationSteps +
                ", sinkStep=" + sinkStep +
                '}';
    }
}