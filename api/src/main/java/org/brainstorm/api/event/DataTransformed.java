package org.brainstorm.api.event;

import org.brainstorm.api.common.Header;

@SuppressWarnings("unused")
public class DataTransformed {
    private Header header;
    private String name;
    private String address;
    private String inputPath;
    private String outputPath;

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

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
    public String toString() {
        return "DataTransformed{" +
                "header=" + header +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", inputPath='" + inputPath + '\'' +
                ", outputPath='" + outputPath + '\'' +
                '}';
    }
}
