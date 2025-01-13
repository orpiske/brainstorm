package org.brainstorm.cli.command;

import picocli.CommandLine;

@CommandLine.Command(name = "runner",
        description = "Create a new brainstorm package for an transformation runner", sortOptions = false)
public class PackageTransformationRunner extends PackageWorker {
    @Override
    public void run() {
        super.run();
    }


}
