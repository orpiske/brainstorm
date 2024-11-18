package org.brainstorm.worker.routes;

import java.util.concurrent.CountDownLatch;

import org.apache.camel.builder.RouteBuilder;

public class DataAcquiredRoute extends RouteBuilder {
    private final String bootstrapHost;
    private final int bootstrapPort;
    private final CountDownLatch launchLatch;

    public DataAcquiredRoute(String bootstrapHost, int bootstrapPort, CountDownLatch launchLatch) {
        this.bootstrapHost = bootstrapHost;
        this.bootstrapPort = bootstrapPort;
        this.launchLatch = launchLatch;
    }

    @Override
    public void configure() throws Exception {
        from("direct:completed")
                .toF("kafka:commits?brokers=%s:%d", bootstrapHost, bootstrapPort)
                .process(exchange -> launchLatch.countDown());
    }
}
