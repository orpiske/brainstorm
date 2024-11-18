package org.brainstorm.worker.runner.routes;

import org.apache.camel.builder.RouteBuilder;

public class OnDataAcquiredRoute extends RouteBuilder {
    private final String bootstrapHost;
    private final int bootstrapPort;

    public OnDataAcquiredRoute(String bootstrapHost, int bootstrapPort) {
        this.bootstrapHost = bootstrapHost;
        this.bootstrapPort = bootstrapPort;
    }

    @Override
    public void configure() throws Exception {
        fromF("kafka:commits?brokers=%s:%d", bootstrapHost, bootstrapPort)
                .to("direct:commits");
    }
}
