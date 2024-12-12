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
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.download.DependencyDownloader;
import org.apache.camel.main.download.DependencyDownloaderClassLoader;
import org.apache.camel.main.download.DependencyDownloaderRoutesLoader;
import org.apache.camel.main.download.MavenDependencyDownloader;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.ResourceLoader;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.PluginHelper;
import org.brainstorm.source.camel.common.GavUtil;
import org.brainstorm.source.camel.common.Topics;
import org.brainstorm.source.camel.common.processors.ShutdownProcessor;
import org.brainstorm.source.camel.common.routes.PipelineEndRoute;
import org.brainstorm.source.camel.routes.DataAcquiredRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class CamelSourceMain implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(CamelSourceMain.class);

    static class ExclusiveEntries {
        @CommandLine.Option(names = {"--file"}, description = "The integration file to use", required = true)
        private String file;

        @CommandLine.Option(names = {"--directory"}, description = "A directory containing route files to execute", required = true)
        private String directory;
    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    ExclusiveEntries exclusiveEntries;

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

    @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display a help message")
    private boolean helpRequested = false;


    private void loadRoute(CamelContext context, String path) {
        final ExtendedCamelContext camelContextExtension = context.getCamelContextExtension();

        downloadDependencies(camelContextExtension);

        DependencyDownloaderRoutesLoader loader = new DependencyDownloaderRoutesLoader(context);
        camelContextExtension.addContextPlugin(RoutesLoader.class, loader);

        final ResourceLoader resourceLoader = PluginHelper.getResourceLoader(context);
        final Resource resource = resourceLoader.resolveResource(path);

        try {
            loader.loadRoutes(resource);
        } catch (Exception e) {
            LOG.error("Failed to load routes from {}", path, e);
            return;
        }

        context.build();
    }

    private void downloadDependencies(ExtendedCamelContext camelContextExtension) {
        final DependencyDownloaderClassLoader cl = createClassLoader();
        final MavenDependencyDownloader downloader = createDownloader(cl);

        if (dependenciesList != null) {
            final String[] dependencies = dependenciesList.split(",");
            for (String dependency : dependencies) {
                downloader.downloadDependency(GavUtil.group(dependency), GavUtil.artifact(dependency), GavUtil.version(dependency));
            }

            cl.getDownloaded().forEach(d -> LOG.debug("Downloaded {}", d));
        }

        Thread.currentThread().setContextClassLoader(cl);
        camelContextExtension.addContextPlugin(DependencyDownloader.class, downloader);
    }

    private static MavenDependencyDownloader createDownloader(DependencyDownloaderClassLoader cl) {
        MavenDependencyDownloader downloader = new MavenDependencyDownloader();
        downloader.setClassLoader(cl);
        downloader.start();
        return downloader;
    }

    private static DependencyDownloaderClassLoader createClassLoader() {
        final ClassLoader parentCL = CamelSourceMain.class.getClassLoader();

        return new DependencyDownloaderClassLoader(parentCL);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CamelSourceMain()).execute(args);

        System.exit(exitCode);
    }

    public static boolean isFileAvailable(File routeFile, int retries, boolean waitForever) {
        if (routeFile.exists()) {
            return true;
        }

        if (!waitForever) {
            if (retries > 0) {
                return false;
            }

            return true;
        }

        return false;
    }

    public static boolean waitForFile(File input, boolean isDirectory, boolean waitForever) throws IOException, InterruptedException {
        int retries = 30;
        int waitSeconds = 1;

        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = isDirectory ? input.toPath() : input.getParentFile().toPath();

        if (input.exists()) {
            LOG.info("File {} already available", input);
            return true;
        }

        // We watch for both the file creation and truncation
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

        do {
            if (!waitForever) {
                LOG.info("Waiting {} seconds for {} to be available", retries * waitSeconds, input);
            } else {
                LOG.info("Waiting indefinitely for {} to be available", input);
            }

            WatchKey watchKey = watchService.poll(1, TimeUnit.SECONDS);

            if (watchKey == null) {
                continue;
            }

            for (WatchEvent<?> event : watchKey.pollEvents()) {

                /*
                  It should return a Path object for ENTRY_CREATE and ENTRY_MODIFY events
                 */
                Object context = event.context();
                if (!(context instanceof Path contextPath)) {
                    LOG.warn("Received an unexpected event of kind {} for context {}", event.kind(), event.context());
                    continue;
                }

                if (contextPath.toString().equals(input.getName())) {
                    LOG.debug("File at the build path {} had a matching event of type: {}", input.getParentFile().getPath(),
                            event.kind());

                    return false;
                } else {
                    LOG.debug("Ignoring a watch event at build path {} of type {} for file: {}", input.getParentFile().getPath(),
                            event.kind(), contextPath.getFileName());
                }
            }
            watchKey.reset();
        } while (!isFileAvailable(input, retries--, waitForever));

        return input.exists();
    }

    @Override
    public Integer call() throws Exception {
        if (exclusiveEntries.file != null) {
            if (exclusiveEntries.file.contains("file://")) {
                LOG.error("Invalid file {} (do not prefix with file://)", exclusiveEntries.file);
                return 1;
            }

            if (!waitForFile(new File(exclusiveEntries.file), false, waitForever)) {
                return 2;
            }
        } else {
            if (!waitForFile(new File(exclusiveEntries.directory), false, waitForever)) {
                return 2;
            }
        }

        CamelContext context = new DefaultCamelContext();

        if (exclusiveEntries.file != null) {
            loadRoute(context, "file://" + exclusiveEntries.file);
        } else {
            File directory = new File(exclusiveEntries.directory);
            final String[] list = directory.list((dir, name) -> name.endsWith(".yaml"));

            if (list != null) {
                File routeFile = new File(exclusiveEntries.directory, list[0]);
                loadRoute(context, "file://" + routeFile.getAbsolutePath());
            } else {
                LOG.warn("No YAML file is available in the directory");
                return 3;
            }
        }

        CountDownLatch launchLatch = new CountDownLatch(1);
        try {
            context.getRegistry().bind(PipelineEndRoute.PROCESSOR, new ShutdownProcessor(launchLatch));
            context.addRoutes(new DataAcquiredRoute(bootstrapServer, bootstrapPort, producesTo, Topics.ACQUISITION_EVENT));
            context.addRoutes(new PipelineEndRoute(Topics.ACQUISITION_EVENT));

            context.start();
        } finally {
            launchLatch.await();
        }

        return 0;
    }
}
