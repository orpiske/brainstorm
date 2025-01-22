package org.brainstorm.cli.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.Set;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.BuildImageResultCallback;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import org.jboss.logging.Logger;
import picocli.CommandLine;

public abstract class PackageWorker extends BaseCommand {
    private static final Logger LOG = Logger.getLogger(PackageWorker.class);

    @CommandLine.Option(names = {"--credentials"}, description = "The path to the properties file containing the credentials to use", defaultValue = "${user.home}/.brainstorm/cli/credentials.properties",
            arity = "0..1")
    protected String credentialsPath;

    @CommandLine.Option(names = { "--base-dir" }, description = "The base dir containing the dockerfile and artifacts to build the container", arity = "0..1")
    private String baseDir;

    @CommandLine.Option(names = { "--push" }, description = "Push the image to the registry", defaultValue = "false", arity = "0..1")
    private boolean push;

    @CommandLine.Option(names = {
            "--output-image" }, description = "The default base image", arity = "0..1")
    protected String outputImage;

    @Override
    public void run() {
        LOG.infof("Building a dockefile-based container...");
        buildDockerfile();
    }

    private Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream in = new FileInputStream(credentialsPath)) {
            props.load(in);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return props;
    }

    private void buildDockerfile() {
        Properties props = loadProperties();

        String username = props.getProperty("registry.username");
        String password = props.getProperty("registry.password");

        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUsername(username)
                .withRegistryPassword(password)
                .build();

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

        final File dockerfile = new File(baseDir, "Dockerfile");
        if (!dockerfile.exists()) {
            LOG.errorf("There is no Dockerfile at %s", dockerfile.getAbsolutePath());
        }

        LOG.infof("Building image %s", outputImage);
        String imageId = dockerClient
                .buildImageCmd()
                .withDockerfile(dockerfile)
                .withPull(true)
                .withBaseDirectory(dockerfile.getParentFile())
                .withNoCache(false)
                .withTags(Set.of(outputImage))
                .exec(new BuildImageResultCallback())
                .awaitImageId();

        try {
            if (push) {
                LOG.infof("Pushing image %s", outputImage);
                dockerClient.pushImageCmd(outputImage)
                        .exec(new ResultCallback.Adapter<>()).awaitCompletion();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        LOG.infof("Built image %s", imageId);

    }
}
