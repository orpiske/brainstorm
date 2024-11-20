package org.brainstorm.worker.routes;

import org.apache.camel.builder.RouteBuilder;
import org.brainstorm.worker.common.Topics;

public class DataAcquiredRoute extends RouteBuilder {
    private final String bootstrapHost;
    private final int bootstrapPort;
    private final String producesTo;
    private final String notifies;

    public DataAcquiredRoute(String bootstrapHost, int bootstrapPort, String producesTo, String notifies) {
        this.bootstrapHost = bootstrapHost;
        this.bootstrapPort = bootstrapPort;
        this.producesTo = producesTo;
        this.notifies = notifies;
    }

    @Override
    public void configure() {
        fromF("direct:%s", Topics.DATA_ACQUIRED)
                .toF("kafka:%s?brokers=%s:%d", producesTo , bootstrapHost, bootstrapPort)
                .toF("direct:%s", notifies);
    }
}
