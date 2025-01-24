package org.brainstorm.cli.command;

import org.brainstorm.cli.common.CredentialsHelper;
import org.brainstorm.cli.common.ImageBuilder;
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
        LOG.infof("Building a container using Dockerfile method...");

        ImageBuilder imageBuilder = new ImageBuilder(CredentialsHelper.loadProperties(credentialsPath));
        imageBuilder.buildInPath(baseDir, outputImage);

        if (push) {
            imageBuilder.push(outputImage);
        }
    }


}
