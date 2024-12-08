package org.brainstorm.cli.command;

import picocli.CommandLine;

public abstract class PackageWorker extends BaseCommand {
    @CommandLine.Option(names = {"--username"}, description = "The username for the registry", arity = "0..1")
    protected String username;

    @CommandLine.Option(names = {"--password"}, description = "The password for the registry", interactive = true, arity = "0..1")
    protected String password;

    @Override
    public void run() {

    }
}
