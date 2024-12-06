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

package org.brainstorm.cli.command;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import org.brainstorm.cli.common.Archive;
import org.brainstorm.cli.common.BrainstormEntry;
import org.brainstorm.cli.services.AcquisitionService;
import picocli.CommandLine;

@CommandLine.Command(name = "package",
        description = "Create a new brainstorm package", sortOptions = false)
public class Package extends BaseCommand {
    @CommandLine.Option(names = {"--package"}, description = "", arity = "0..1")
    private String addPackage;

    @CommandLine.Option(names = {"--ingestion"}, description = "", arity = "0..1")
    private String ingestion;

    @CommandLine.Option(names = {"--address"}, description = "The service address", arity = "0..1", required = true, defaultValue = "http://localhost:8080")
    private String address;

    AcquisitionService acquisitionService;

    @Override
    public void run() {
        List<BrainstormEntry> packageFiles = new ArrayList<>();

        packageFiles.add(new BrainstormEntry(BrainstormEntry.EntryType.ACQUISITION, ingestion));
        packageFiles.add(new BrainstormEntry(BrainstormEntry.EntryType.CODE, addPackage));


        Archive archive = new Archive(packageFiles);
        try {
            Path path = archive.compress();

            upload(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void upload(Path path) {
        try {
            acquisitionService = QuarkusRestClientBuilder.newBuilder()
                    .baseUri(URI.create(address))
                    .build(AcquisitionService.class);

            FileInputStream fis = new FileInputStream(path.toFile());

            acquisitionService.addPackage(fis);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
