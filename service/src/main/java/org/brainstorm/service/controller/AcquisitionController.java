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

package org.brainstorm.service.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.context.SmallRyeManagedExecutor;
import org.brainstorm.service.util.BrainstormConfiguration;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AcquisitionController {
    private static final Logger LOG = Logger.getLogger(AcquisitionController.class);

    @Inject
    BrainstormConfiguration configuration;

    SmallRyeManagedExecutor managedExecutor = SmallRyeManagedExecutor.builder()
            .withExecutorService(Executors.newCachedThreadPool())
            .build();


    private void add(String route) {
        String dataPath = configuration.data().path();
        File outputFile = new File(dataPath, "route.yaml");

        if (!outputFile.getParentFile().exists()) {
            if (!outputFile.getParentFile().mkdirs()) {
                LOG.errorf("Unable to create directory %s", outputFile.getParentFile());
            }
        }

        try (OutputStream fos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            fos.write(route.getBytes());
        } catch (FileNotFoundException e) {
            LOG.error("File not found: %s", outputFile, e);
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOG.error("I/O error trying to write: %s", outputFile, e);
            throw new RuntimeException(e);
        }
    }

    @ConsumeEvent(value = "acquisition", blocking = true)
    public void addInternally(String route) {
        LOG.info("Executing pipeline internally (blocking)");

        managedExecutor.execute(() -> add(route));
    }
}
