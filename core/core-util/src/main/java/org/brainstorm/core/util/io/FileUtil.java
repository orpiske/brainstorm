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

package org.brainstorm.core.util.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

    private FileUtil() {}

    private static boolean isFileAvailable(File routeFile, int retries, boolean waitForever) {
        if (routeFile.exists()) {
            return true;
        }

        if (!waitForever) {
            if (retries > 0) {
                return false;
            }

            return true;
        }

        return false;
    }

    public static boolean waitForFile(File input, boolean isDirectory, boolean waitForever) throws IOException, InterruptedException {
        int retries = 30;
        int waitSeconds = 1;

        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = isDirectory ? input.toPath() : input.getParentFile().toPath();

        if (input.exists()) {
            LOG.info("File {} already available", input);
            return true;
        }

        // We watch for both the file creation and truncation
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY);

        do {
            if (!waitForever) {
                LOG.info("Waiting {} seconds for {} to be available", retries * waitSeconds, input);
            } else {
                LOG.info("Waiting indefinitely for {} to be available", input);
            }

            WatchKey watchKey = watchService.poll(1, TimeUnit.SECONDS);

            if (watchKey == null) {
                continue;
            }

            for (WatchEvent<?> event : watchKey.pollEvents()) {

                /*
                  It should return a Path object for ENTRY_CREATE and ENTRY_MODIFY events
                 */
                Object context = event.context();
                if (!(context instanceof Path contextPath)) {
                    LOG.warn("Received an unexpected event of kind {} for context {}", event.kind(), event.context());
                    continue;
                }

                if (contextPath.toString().equals(input.getName())) {
                    LOG.debug("File at the build path {} had a matching event of type: {}", input.getParentFile().getPath(),
                            event.kind());

                    return false;
                } else {
                    LOG.debug("Ignoring a watch event at build path {} of type {} for file: {}", input.getParentFile().getPath(),
                            event.kind(), contextPath.getFileName());
                }
            }
            watchKey.reset();
        } while (!isFileAvailable(input, retries--, waitForever));

        return input.exists();
    }
}
