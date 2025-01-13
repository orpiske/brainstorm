package org.brainstorm.cli.command;

import picocli.CommandLine;

@CommandLine.Command(name = "sink",
        description = "Create a new brainstorm package for an sink worker", sortOptions = false)
public class PackageSinkWorker extends PackageWorker {
    @Override
    public void run() {
        super.run();
    }
}
