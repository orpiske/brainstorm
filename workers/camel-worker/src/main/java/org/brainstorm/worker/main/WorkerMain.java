package org.brainstorm.worker.main;

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
import org.brainstorm.worker.common.GavUtil;
import org.brainstorm.worker.common.Topics;
import org.brainstorm.worker.common.processors.ShutdownProcessor;
import org.brainstorm.worker.common.routes.PipelineEndRoute;
import org.brainstorm.worker.routes.DataAcquiredRoute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class WorkerMain implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(WorkerMain.class);

    @CommandLine.Option(names = {"-f", "--file"}, description = "The integration file to use", required = true)
    private String file;

    @CommandLine.Option(names = {"-d", "--dependencies"}, description = "The list of dependencies to include in runtime (comma-separated)", required = true)
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

        final String[] dependencies = dependenciesList.split(",");
        for (String dependency : dependencies) {
            downloader.downloadDependency(GavUtil.group(dependency), GavUtil.artifact(dependency), GavUtil.version(dependency));
        }
        Thread.currentThread().setContextClassLoader(cl);

        cl.getDownloaded().forEach(d -> LOG.debug("Downloaded {}", d));
        camelContextExtension.addContextPlugin(DependencyDownloader.class, downloader);
    }

    private static MavenDependencyDownloader createDownloader(DependencyDownloaderClassLoader cl) {
        MavenDependencyDownloader downloader = new MavenDependencyDownloader();
        downloader.setClassLoader(cl);
        downloader.start();
        return downloader;
    }

    private static DependencyDownloaderClassLoader createClassLoader() {
        final ClassLoader parentCL = WorkerMain.class.getClassLoader();
        return new DependencyDownloaderClassLoader(parentCL);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new WorkerMain()).execute(args);

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

    public static boolean waitForFile(File routeFile, boolean waitForever) throws IOException, InterruptedException {
        int retries = 30;
        int waitSeconds = 1;

        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = routeFile.getParentFile().toPath();

        if (routeFile.exists()) {
            LOG.info("File {} already available", routeFile);
            return true;
        }

        // We watch for both the file creation and truncation
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);


        do {
            if (!waitForever) {
                LOG.info("Waiting {} seconds for {} to be available", retries * waitSeconds, routeFile);
            } else {
                LOG.info("Waiting indefinitely for {} to be available", routeFile);
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
                if (!(context instanceof Path)) {
                    LOG.warn("Received an unexpected event of kind {} for context {}", event.kind(), event.context());
                    continue;
                }

                Path contextPath = (Path) context;

                if (contextPath.toString().equals(routeFile.getName())) {
                    LOG.debug("File at the build path {} had a matching event of type: {}", routeFile.getParentFile().getPath(),
                            event.kind());

                    return false;
                } else {
                    LOG.debug("Ignoring a watch event at build path {} of type {} for file: {}", routeFile.getParentFile().getPath(),
                            event.kind(), contextPath.getFileName());
                }
            }
            watchKey.reset();
        } while (!isFileAvailable(routeFile, retries--, waitForever));

        return routeFile.exists();
    }

    @Override
    public Integer call() throws Exception {
        if (file.contains("file://")) {
            LOG.error("Invalid file {} (do not prefix with file://)", file);
            return 1;
        }

        if (!waitForFile(new File(file), waitForever)) {
            return 2;
        }

        CamelContext context = new DefaultCamelContext();

        CountDownLatch launchLatch = new CountDownLatch(1);

        loadRoute(context, "file://" + file);

        context.getRegistry().bind(PipelineEndRoute.PROCESSOR, new ShutdownProcessor(launchLatch));

        context.addRoutes(new DataAcquiredRoute(bootstrapServer, bootstrapPort, producesTo, Topics.ACQUISITION_EVENT));
        context.addRoutes(new PipelineEndRoute(Topics.ACQUISITION_EVENT));

        try {
            context.start();
        } finally {
            launchLatch.await();
        }

        return 0;
    }
}
