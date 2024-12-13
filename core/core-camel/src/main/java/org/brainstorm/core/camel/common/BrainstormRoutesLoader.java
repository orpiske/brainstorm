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

package org.brainstorm.core.camel.common;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.main.download.DependencyDownloader;
import org.apache.camel.main.download.DependencyDownloaderClassLoader;
import org.apache.camel.main.download.DependencyDownloaderRoutesLoader;
import org.apache.camel.main.download.MavenDependencyDownloader;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.ResourceLoader;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.PluginHelper;

public class BrainstormRoutesLoader {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BrainstormRoutesLoader.class);

    private final String dependenciesList;

    public BrainstormRoutesLoader(String dependenciesList) {
        this.dependenciesList = dependenciesList;
    }

    public void loadRoute(CamelContext context, String path) {
        final ExtendedCamelContext camelContextExtension = context.getCamelContextExtension();

        downloadDependencies(camelContextExtension);

        DependencyDownloaderRoutesLoader loader = new DependencyDownloaderRoutesLoader(context);
        camelContextExtension.addContextPlugin(RoutesLoader.class, loader);

        final ResourceLoader resourceLoader = PluginHelper.getResourceLoader(context);
        final Resource resource = resourceLoader.resolveResource(path);

        try {
            loader.loadRoutes(resource);
        } catch (Exception e) {
            LOG.error("Failed to load routes from {}", path, e);
            return;
        }

        context.build();
    }

    private void downloadDependencies(ExtendedCamelContext camelContextExtension) {
        final DependencyDownloaderClassLoader cl = createClassLoader();
        final MavenDependencyDownloader downloader = createDownloader(cl);

        if (dependenciesList != null) {
            final String[] dependencies = dependenciesList.split(",");
            for (String dependency : dependencies) {
                downloader.downloadDependency(GavUtil.group(dependency), GavUtil.artifact(dependency), GavUtil.version(dependency));
            }

            cl.getDownloaded().forEach(d -> LOG.debug("Downloaded {}", d));
        }

        Thread.currentThread().setContextClassLoader(cl);
        camelContextExtension.addContextPlugin(DependencyDownloader.class, downloader);
    }

    private static MavenDependencyDownloader createDownloader(DependencyDownloaderClassLoader cl) {
        MavenDependencyDownloader downloader = new MavenDependencyDownloader();
        downloader.setClassLoader(cl);
        downloader.start();
        return downloader;
    }

    private static DependencyDownloaderClassLoader createClassLoader() {
        final ClassLoader parentCL = BrainstormRoutesLoader.class.getClassLoader();

        return new DependencyDownloaderClassLoader(parentCL);
    }
}
