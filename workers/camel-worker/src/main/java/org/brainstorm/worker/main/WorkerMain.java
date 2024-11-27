package org.brainstorm.worker.main;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

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

    @Override
    public Integer call() throws Exception {
        CamelContext context = new DefaultCamelContext();

        CountDownLatch launchLatch = new CountDownLatch(1);

        if (file.contains("file://")) {
            loadRoute(context, file);
        } else {
            loadRoute(context, "file://" + file);
        }

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
