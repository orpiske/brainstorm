/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.brainstorm.transformer.quarkus.workflow.steps;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.brainstorm.transformer.quarkus.workflow.BufferedStreamingResponseHandler;
import org.jboss.logging.Logger;

/**
 * Control the chain-of-though interaction with the LLM API
 */
public class Steps {
    private static final Logger LOG = Logger.getLogger(Steps.class);

    /**
     * A unit of conversation is its user message and the response from the LLM API
     */
    public static class ConversationUnit {
        private final ConversationUnit lastConversationUnit;
        private UserMessage userMessage;
        private String response;

        public ConversationUnit() {
            lastConversationUnit = null;
        }

        public ConversationUnit(ConversationUnit lastConversationUnit, UserMessage userMessage) {
            this.lastConversationUnit = lastConversationUnit;
            this.userMessage = userMessage;
        }

        public UserMessage userMessage() {
            return userMessage;
        }

        void setUserMessage(UserMessage userMessage) {
            this.userMessage = userMessage;
        }

        public String response() {
            return response;
        }

        void setResponse(String response) {
            this.response = response;
        }

        public ConversationUnit lastConversationUnit() {
            return lastConversationUnit;
        }

        public ConversationUnit newConversationUnit() {
            return new ConversationUnit(this, null);
        }
    }

    /**
     * Holds the chat runtime metadata
     */
    public static class ChatMeta {
        private Object context;
        private ConversationUnit conversationUnit;
        private Throwable exception;

        private ChatMeta() {
            conversationUnit = new ConversationUnit();
        }

        public <T> T context(Class<T> payloadType) {
            return payloadType.cast(context);
        }

        public void setContext(Object context) {
            this.context = context;
        }

        public ConversationUnit conversationUnit() {
            return conversationUnit;
        }

        public void setConversationUnit(ConversationUnit conversationUnit) {
            this.conversationUnit = conversationUnit;
        }

        public Throwable exception() {
            return exception;
        }

        public void setException(Throwable exception) {
            this.exception = exception;
        }

        public boolean isFailed() {
            return exception != null;
        }
    }

    private ChatMeta chatMeta;
    private StreamingChatLanguageModel chatModel;
    private Consumer<ChatMeta> errorConsumer;
    private boolean errorHandled = false;

    public static final class ChatStep {
        public Steps chat(Consumer<ChatMeta> consumer) {
            ChatMeta chatMeta = new ChatMeta();
            consumer.accept(chatMeta);

            return new Steps(chatMeta);
        }

        public Steps noContextChat() {
            return new Steps(new ChatMeta());
        }

        public static Steps using(StreamingChatLanguageModel chatModel) {
            return new Steps(chatModel, new ChatMeta());
        }
    }

    public Steps(ChatMeta lastInputMeta) {
        this.chatMeta = lastInputMeta;
    }

    public Steps(StreamingChatLanguageModel model, ChatMeta lastInputMeta) {
        this.chatModel = model;
        this.chatMeta = lastInputMeta;
    }

    /**
     * Sets a context to be shared through all the chat
     * @param consumer A consumer method that can set the context
     * @return
     */
    public Steps withContext(Consumer<ChatMeta> consumer) {
        if (chatMeta.exception() != null) {
            handleError();

            return this;
        }

        consumer.accept(chatMeta);

        return this;
    }

    /**
     * The prompt to use for the conversation
     * @param consumer
     * @return
     */
    public Steps usingPrompt(Function<ChatMeta, UserMessage> consumer) {
        if (chatMeta.exception() != null) {
            handleError();

            return this;
        }

        final UserMessage userMessage = consumer.apply(chatMeta);

        chatMeta.conversationUnit = chatMeta.conversationUnit.newConversationUnit();
        chatMeta.conversationUnit.setUserMessage(userMessage);
        return this;
    }

    /**
     * An optional method to execute after the chat with the LLM
     * @param consumer
     * @return
     */
    public Steps andThen(Consumer<ChatMeta> consumer) {
        if (chatMeta.exception() != null) {
            if (chatMeta.exception() != null) {
                handleError();
            }

            return this;
        }

        consumer.accept(chatMeta);
        return this;
    }

    /**
     * Sets a context to be shared through all the chat
     * @param consumer A consumer method that can set the context
     * @return
     */
    public Steps onError(Consumer<ChatMeta> consumer) {
        errorConsumer = consumer;

        return this;
    }

    /**
     * Calls the LLM API
     * @return
     */
    public Steps chat() {
        UserMessage userMessage = chatMeta.conversationUnit.userMessage;
        if (userMessage == null) {
            // Skipping this one

            return this;
        }

        if (chatMeta.exception() != null) {
            handleError();

            return this;
        }

        CountDownLatch latch = new CountDownLatch(1);

        BufferedStreamingResponseHandler handler = new BufferedStreamingResponseHandler(latch);
        try {
            chatModel.generate(userMessage, handler);

            if (!latch.await(2, TimeUnit.MINUTES)) {
                LOG.error("Calling the LLM took too long, response might be incomplete.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            // call abort
            return this;
        }

        String response = handler.getResponse();
        if (handler.isFailed()) {
            chatMeta.exception = handler.getError();
            LOG.warnf("The workflow has failed due to an exception: %s", chatMeta.exception != null ? chatMeta.exception.getMessage() : "undefined");
        }

        chatMeta.conversationUnit.setResponse(response);

        return this;
    }

    private void handleError() {
        if (errorConsumer != null && !errorHandled) {
            errorConsumer.accept(chatMeta);
            errorHandled = true;
        }
    }

    public ChatMeta eval() {
        return chatMeta;
    }

}
