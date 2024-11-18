package org.brainstorm.worker.runner.routes;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnDataAcquiredHandlerRoute extends RouteBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(OnDataAcquiredHandlerRoute.class);

    private final String script;
    private final CountDownLatch launchLatch;

    public OnDataAcquiredHandlerRoute(String script, CountDownLatch launchLatch) {
        this.script = script;
        this.launchLatch = launchLatch;
    }

    private void exec(Exchange exchange) {
        final String body = exchange.getIn().getBody(String.class);
        LOG.info("Executing on data acquired handler: {}", body);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(script, body);
            processBuilder.inheritIO();

            final Process process = processBuilder.start();
            final int i = process.waitFor();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void configure() throws Exception {
        fromF("direct:commits")
                .process(this::exec)
                .process(exchange -> launchLatch.countDown());
    }
}
