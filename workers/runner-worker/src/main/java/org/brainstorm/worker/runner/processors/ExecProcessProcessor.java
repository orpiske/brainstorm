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

package org.brainstorm.worker.runner.processors;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.camel.Exchange;
import org.apache.camel.InvalidPayloadException;
import org.apache.camel.Processor;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecProcessProcessor implements Processor {
    private static final Logger LOG = LoggerFactory.getLogger(ExecProcessProcessor.class);
    private final String script;

    public ExecProcessProcessor(String script) {
        this.script = script;
    }

    @Override
    public void process(Exchange exchange) throws InvalidPayloadException {
        final String body = exchange.getIn().getMandatoryBody(String.class);
        LOG.info("Executing script {} on data acquired handler: {}", script, body);

        File scriptFile = new File(script);
        if (!scriptFile.exists()) {
            LOG.error("The script {} does not exist", script);
            return;
        }

        if (scriptFile.isDirectory()) {
            LOG.error("The script {} is a directory and cannot be executed", script);
            return;
        }

        if (!scriptFile.canExecute()) {
            LOG.error("The script {} is not executable", script);
            return;
        }

        try {
            LOG.info("Building process for execution");
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command(script, body);
            processBuilder.inheritIO();

            final Process process = processBuilder.start();
            final int i = process.waitFor();
            if (i != 0) {
                LOG.warn("Transformation did not complete successfully");
            }

            File stepOut = new File(scriptFile.getParentFile(), "step.out");
            if (stepOut.exists()) {
                LOG.info("A step.out file exists, therefore using it to set the body");
                final String stepOutData = FileUtils.readFileToString(stepOut, StandardCharsets.UTF_8);
                exchange.getIn().setBody(stepOutData);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
