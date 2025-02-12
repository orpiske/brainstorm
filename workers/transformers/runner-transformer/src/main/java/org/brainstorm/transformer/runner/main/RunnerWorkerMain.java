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

package org.brainstorm.transformer.runner.main;

import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.brainstorm.source.camel.common.Topics;
import org.brainstorm.source.camel.common.processors.ProcessorNames;
import org.brainstorm.source.camel.common.processors.ShutdownProcessor;
import org.brainstorm.source.camel.common.routes.NotifyingPipelineEndRoute;
import org.brainstorm.source.camel.common.routes.PipelineStepRoute;
import org.brainstorm.transformer.runner.processors.ExecProcessProcessor;
import org.brainstorm.source.camel.common.routes.PipelineStartRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class RunnerWorkerMain implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(RunnerWorkerMain.class);
    private static final String DEFAULT_SCRIPT_NAME = "transform.sh";

    @CommandLine.Option(names = {"--step"}, description = "The step to run on event (should contain a file named " + DEFAULT_SCRIPT_NAME + ")", required = true)
    private String step;

    @CommandLine.Option(names = {"-s", "--bootstrap-server"}, description = "The Kafka bootstrap server to use", required = true)
    private String bootstrapServer;

    @CommandLine.Option(names = {"-p", "--bootstrap-server-port"}, description = "The Kafka bootstrap server port to use", defaultValue = "9092")
    private int bootstrapPort;

    @CommandLine.Option(names = {"--consumes-from"}, description = "The Kafka topic from which to consume the trigger event")
    private String consumesFrom;

    @CommandLine.Option(names = {"--produces-to"}, description = "The Kafka topic produce the completion event")
    private String producesTo;

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested = false;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new RunnerWorkerMain()).execute(args);

        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        CamelContext context = new DefaultCamelContext();

        CountDownLatch launchLatch = new CountDownLatch(1);

        String script = Paths.get(step, DEFAULT_SCRIPT_NAME).toAbsolutePath().toString();
        context.getRegistry().bind(ProcessorNames.ON_DATA_CONSUMED, new ExecProcessProcessor(script));
        context.getRegistry().bind(ProcessorNames.ON_DATA_PROCESSED, new ShutdownProcessor(launchLatch));

        context.addRoutes(new PipelineStartRoute(bootstrapServer, bootstrapPort, consumesFrom, Topics.EVENT_DATA_CONSUMED));
        context.addRoutes(new PipelineStepRoute(Topics.EVENT_DATA_CONSUMED, Topics.EVENT_DATA_READY));
        context.addRoutes(new NotifyingPipelineEndRoute(bootstrapServer, bootstrapPort, Topics.EVENT_DATA_READY, producesTo));

        try {
            context.start();
        } finally {
            launchLatch.await();
        }

        return 0;
    }
}
