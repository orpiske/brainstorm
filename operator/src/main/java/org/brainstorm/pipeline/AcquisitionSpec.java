package org.brainstorm.pipeline;

import java.util.ArrayList;
import java.util.List;

public class AcquisitionSpec {
    private String bootstrapServer;
    private int port;
    private String producesTo;
    private String file;
    private List<String> dependencies = new ArrayList<>();

    public String getBootstrapServer() {
        return bootstrapServer;
    }

    public void setBootstrapServer(String bootstrapServer) {
        this.bootstrapServer = bootstrapServer;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProducesTo() {
        return producesTo;
    }

    public void setProducesTo(String producesTo) {
        this.producesTo = producesTo;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public String toString() {
        return "AcquisitionSpec{" +
                "bootstrapServer='" + bootstrapServer + '\'' +
                ", port=" + port +
                ", producesTo='" + producesTo + '\'' +
                ", file='" + file + '\'' +
                ", dependencies=" + dependencies +
                '}';
    }
}