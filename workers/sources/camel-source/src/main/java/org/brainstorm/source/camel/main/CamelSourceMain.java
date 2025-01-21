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

package org.brainstorm.source.camel.main;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.brainstorm.core.camel.common.BrainstormRoutesLoader;
import org.brainstorm.core.util.io.FileUtil;
import org.brainstorm.source.camel.common.Topics;
import org.brainstorm.source.camel.common.processors.ProcessorNames;
import org.brainstorm.source.camel.common.processors.ShutdownProcessor;
import org.brainstorm.source.camel.common.routes.PipelineEndRoute;
import org.brainstorm.source.camel.routes.DataAcquiredRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class CamelSourceMain implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(CamelSourceMain.class);

    @CommandLine.Option(names = {"--file"}, description = "The integration file to use", required = true)
    private String file;

    @CommandLine.Option(names = {"-d", "--dependencies"}, description = "The list of dependencies to include in runtime (comma-separated)")
    private String dependenciesList;

    @CommandLine.Option(names = {"-s", "--bootstrap-server"}, description = "The Kafka bootstrap server to use", required = true)
    private String bootstrapServer;

    @CommandLine.Option(names = {"-p", "--bootstrap-server-port"}, description = "The Kafka bootstrap server port to use", defaultValue = "9092")
    private int bootstrapPort;

    @CommandLine.Option(names = {"--consumes-from"}, description = "The Kafka topic from which to consume the trigger event")
    private String consumesFrom;

    @CommandLine.Option(names = {"--produces-to"}, description = "The Kafka topic produce the completion event")
    private String producesTo;

    @CommandLine.Option(names = {"--wait"}, description = "Wait forever until a file is created", defaultValue = "false")
    private boolean waitForever;

    @CommandLine.Option(names = {"--data-directory"}, description = "The data directory", required = true)
    private String dataDirectory;

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested = false;


    public static void main(String[] args) {
        int exitCode = new CommandLine(new CamelSourceMain()).execute(args);

        System.exit(exitCode);
    }


    @Override
    public Integer call() throws Exception {
        if (file.contains("file://")) {
            LOG.error("Invalid file {} (do not prefix with file://)", file);
            return 1;
        }

        if (!FileUtil.waitForFile(new File(file), false, waitForever)) {
            return 2;
        }


        CamelContext context = new DefaultCamelContext();
        BrainstormRoutesLoader routesLoader = new BrainstormRoutesLoader(dependenciesList);

        routesLoader.loadRoute(context, "file://" + file);


        CountDownLatch launchLatch = new CountDownLatch(1);
        try {
            context.getRegistry().bind(ProcessorNames.ON_DATA_PROCESSED, new ShutdownProcessor(launchLatch));
            context.addRoutes(new DataAcquiredRoute(bootstrapServer, bootstrapPort, producesTo, Topics.ACQUISITION_EVENT,
                    dataDirectory));
            context.addRoutes(new PipelineEndRoute(Topics.ACQUISITION_EVENT));

            context.start();
        } finally {
            launchLatch.await();
        }

        return 0;
    }
}
