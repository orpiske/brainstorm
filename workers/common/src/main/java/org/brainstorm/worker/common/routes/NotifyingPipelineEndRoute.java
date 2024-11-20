package org.brainstorm.worker.common.routes;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotifyingPipelineEndRoute extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(NotifyingPipelineEndRoute.class);
    public static final String PROCESSOR = "onDataProcessed";
    private final String bootstrapHost;
    private final int bootstrapPort;
    private final String consumesFrom;
    private final String notifies;

    public NotifyingPipelineEndRoute(String bootstrapHost, int bootstrapPort, String consumesFrom, String notifies) {
        this.consumesFrom = consumesFrom;
        this.bootstrapHost = bootstrapHost;
        this.bootstrapPort = bootstrapPort;
        this.notifies = notifies;
    }

    @Override
    public void configure() throws Exception {
        fromF("direct:%s", consumesFrom)
                .toF("kafka:%s?brokers=%s:%d", notifies, bootstrapHost, bootstrapPort)
                .process(PROCESSOR);
    }
}
