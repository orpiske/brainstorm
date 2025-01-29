package org.brainstorm.transformer.quarkus.workflow;

import java.util.concurrent.CountDownLatch;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.StreamingResponseHandler;
import dev.langchain4j.model.output.Response;
import org.jboss.logging.Logger;

/**
 * Handles model response, saving it to a string buffer
 */
public class BufferedStreamingResponseHandler implements StreamingResponseHandler<AiMessage> {
    private static final Logger LOG = Logger.getLogger(BufferedStreamingResponseHandler.class);

    private final CountDownLatch latch;
    private StringBuffer responseBuffer = new StringBuffer();
    private volatile Throwable error;

    public BufferedStreamingResponseHandler(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void onNext(String s) {
        responseBuffer.append(s);
    }

    @Override
    public void onError(Throwable throwable) {
        LOG.errorf("Failed: %s", throwable.getMessage());
        this.error = throwable;

        latch.countDown();
    }

    @Override
    public void onComplete(Response<AiMessage> response) {
        try {
            StreamingResponseHandler.super.onComplete(response);
        } finally {
            latch.countDown();
        }
    }

    public String getResponse() {
        return responseBuffer.toString();
    }

    public Throwable getError() {
        return error;
    }

    public boolean isFailed() {
        return error != null;
    }
}
