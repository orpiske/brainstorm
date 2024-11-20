package org.brainstorm.worker.common.routes;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineEndRoute extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineEndRoute.class);
    public static final String PROCESSOR = "onDataProcessed";
    private final String consumesFrom;

    public PipelineEndRoute(String consumesFrom) {
        this.consumesFrom = consumesFrom;
    }

    @Override
    public void configure() throws Exception {
        fromF("direct:%s", consumesFrom)
                .process(PROCESSOR);
    }
}
