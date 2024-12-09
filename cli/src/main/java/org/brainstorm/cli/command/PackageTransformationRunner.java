package org.brainstorm.cli.command;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.RegistryException;
import com.google.cloud.tools.jib.api.RegistryImage;
import com.google.cloud.tools.jib.api.buildplan.AbsoluteUnixPath;
import org.jboss.logging.Logger;
import picocli.CommandLine;

@CommandLine.Command(name = "runner",
        description = "Create a new brainstorm package for an transformation runner", sortOptions = false)
public class PackageTransformationRunner extends PackageWorker {
    private static final Logger LOG = Logger.getLogger(PackageTransformationRunner.class);

    @CommandLine.ArgGroup(exclusive = false)
    SimpleContainer simpleContainer;

    static class SimpleContainer {
        @CommandLine.Option(names = {
                "--base-image" }, description = "The default base image", defaultValue = "quay.io/bstorm/runner-worker:latest", arity = "0..1")
        protected String baseImage;

        @CommandLine.Option(names = { "--script" }, description = "The transformation script to use", arity = "0..1")
        private String ingestion;
    }

    @CommandLine.ArgGroup(exclusive = false)
    DockerfileBased dockerfileBased;

    static class DockerfileBased {
        @CommandLine.Option(names = { "--base-dir" }, description = "The base dir containing the dockerfile and artifacts to build the container", arity = "0..1")
        private String baseDir;
    }

    @CommandLine.Option(names = {
            "--output-image" }, description = "The default base image", arity = "0..1")
    protected String outputImage;

    @Override
    public void run() {
        if (simpleContainer != null) {
            LOG.infof("Building a simple container...");
            buildSimple();
        } else {
            LOG.infof("Building a dockefile-based container...");
            buildDockerfile();
        }

    }

    private void buildSimple() {
        try {
            Jib.from(simpleContainer.baseImage)
                    .addLayer(List.of(Paths.get(simpleContainer.ingestion)), AbsoluteUnixPath.get("/opt/brainstorm/data/"))
                    .containerize(Containerizer.to(RegistryImage.named(outputImage).addCredential(username, password)));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (RegistryException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CacheDirectoryCreationException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InvalidImageReferenceException e) {
            throw new RuntimeException(e);
        }
    }


    private void buildDockerfile() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost == null) {
            dockerHost = "unix:///var/run/docker.sock";
        }


        ZerodepDockerHttpClient client = new ZerodepDockerHttpClient.Builder()
                .dockerHost(URI.create(dockerHost))
                .build();

        DockerClient dockerClient = DockerClientBuilder
                .getInstance(config)
                .withDockerHttpClient(client)
                .build();

        final File dockerfile = new File(dockerfileBased.baseDir, "Dockerfile");
        if (!dockerfile.exists()) {
            LOG.errorf("There is no Dockerfile at %s", dockerfile.getAbsolutePath());
        }

        String imageId = dockerClient
                .buildImageCmd()
                .withDockerfile(dockerfile)
                .withPull(true)
                .withBaseDirectory(dockerfile.getParentFile())
                .withNoCache(false)
                .withTag(outputImage)
                .exec(new BuildImageResultCallback())
                .awaitImageId();

        LOG.infof("Built image %s", imageId);

    }
}
