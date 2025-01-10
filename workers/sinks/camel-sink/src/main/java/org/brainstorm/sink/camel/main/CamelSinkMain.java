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

package org.brainstorm.sink.camel.main;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.brainstorm.core.camel.common.BrainstormRoutesLoader;
import org.brainstorm.core.util.io.FileUtil;
import org.brainstorm.sink.camel.routes.DataCompletedRoute;
import org.brainstorm.source.camel.common.processors.ShutdownProcessor;
import org.brainstorm.source.camel.common.routes.NotifyingPipelineEndRoute;
import org.brainstorm.source.camel.common.routes.PipelineEndRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class CamelSinkMain implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(CamelSinkMain.class);
    private static final String DEFAULT_ROUTE_FILE = "routes.yaml";

    @CommandLine.Option(names = {"-s", "--bootstrap-server"}, description = "The Kafka bootstrap server to use", required = true)
    private String bootstrapServer;

    @CommandLine.Option(names = {"-p", "--bootstrap-server-port"}, description = "The Kafka bootstrap server port to use", defaultValue = "9092")
    private int bootstrapPort;

    @CommandLine.Option(names = {"--consumes-from"}, description = "The Kafka topic from which to consume the trigger event")
    private String consumesFrom;

    @CommandLine.Option(names = {"--step"}, description = "The step to run on sink (should contain a file named " + DEFAULT_ROUTE_FILE + ")", required = true)
    private String step;

    @CommandLine.Option(names = {"-d", "--dependencies"}, description = "The list of dependencies to include in runtime (comma-separated)")
    private String dependenciesList;

    @CommandLine.Option(names = {"--wait"}, description = "Wait forever until a file is created", defaultValue = "false")
    private boolean waitForever;

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested = false;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CamelSinkMain()).execute(args);

        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        final Path routeFile = Paths.get(step, DEFAULT_ROUTE_FILE).toAbsolutePath();

        if (!FileUtil.waitForFile(routeFile.toFile(), false, waitForever)) {
            return 2;
        }

        CamelContext context = new DefaultCamelContext();
        context.getRegistry().bind("global.bootstrapServer", bootstrapServer);
        context.getRegistry().bind("global.bootstrapServerPort", bootstrapPort);

        BrainstormRoutesLoader routesLoader = new BrainstormRoutesLoader(dependenciesList);

        String routeFileUrl = String.format("file://%s", routeFile);
        routesLoader.loadRoute(context, routeFileUrl);

        CountDownLatch launchLatch = new CountDownLatch(1);
        try {
            context.addRoutes(new DataCompletedRoute(bootstrapServer, bootstrapPort, consumesFrom, "data-completed"));
            context.getRegistry().bind(PipelineEndRoute.PROCESSOR, new ShutdownProcessor(launchLatch));
            context.addRoutes(new PipelineEndRoute("end-of-pipeline"));

            context.start();
        } finally {
            launchLatch.await();
        }

        return 0;
    }
}
