package org.brainstorm.worker.common.routes;

import org.apache.camel.builder.RouteBuilder;
import org.brainstorm.worker.common.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineStepRoute extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(PipelineStepRoute.class);
    public static final String PROCESSOR = "onDataConsumed";
    private final String consumesFrom;
    private final String producesTo;

    public PipelineStepRoute(String consumesFrom, String producesTo) {
        this.consumesFrom = consumesFrom;
        this.producesTo = producesTo;
    }

    @Override
    public void configure() throws Exception {
        fromF("direct:%s", consumesFrom)
                .process(PROCESSOR)
                .toF("direct:%s", producesTo);
    }
}
