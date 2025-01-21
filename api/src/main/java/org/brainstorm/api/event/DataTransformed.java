package org.brainstorm.api.event;

import java.util.Objects;

import org.brainstorm.api.common.Header;

@SuppressWarnings("unused")
public class DataTransformed extends DataEvent {
    private String inputPath;
    private String outputPath;

    public String getInputPath() {
        return inputPath;
    }

    public void setInputPath(String inputPath) {
        this.inputPath = inputPath;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        DataTransformed that = (DataTransformed) o;
        return Objects.equals(inputPath, that.inputPath) && Objects.equals(outputPath, that.outputPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), inputPath, outputPath);
    }

    @Override
    public String toString() {
        return "DataTransformed{" +
                "inputPath='" + inputPath + '\'' +
                ", outputPath='" + outputPath + '\'' +
                "} " + super.toString();
    }
}
