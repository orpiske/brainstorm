package org.brainstorm.cli.command;

import picocli.CommandLine;

@CommandLine.Command(name = "source",
        description = "Create a new brainstorm package for a source worker", sortOptions = false)
public class PackageSourceWorker extends PackageWorker {

    @Override
    public void run() {
        super.run();
    }
}
