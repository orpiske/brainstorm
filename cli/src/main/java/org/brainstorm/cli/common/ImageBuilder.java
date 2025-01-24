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

package org.brainstorm.cli.common;

import java.io.File;
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

public final class ImageBuilder {
    private static final Logger LOG = Logger.getLogger(ImageBuilder.class);

    private final Properties credentials;
    private final DockerClientConfig config;
    private final DockerClient dockerClient;

    public ImageBuilder(Properties credentials) {
        this.credentials = credentials;
        this.config = configureClient();
        this.dockerClient = createDockerClient();;
    }

    private DockerClientConfig configureClient() {
        String username = credentials.getProperty("registry.username");
        String password = credentials.getProperty("registry.password");

        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withRegistryUsername(username)
                .withRegistryPassword(password)
                .build();
    }

    private DockerClient createDockerClient() {
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
        return dockerClient;
    }

    public ImageBuilder buildInPath(File baseDir, String outputImage) {
        final File dockerfile = new File(baseDir, "Dockerfile");
        return build(dockerfile, outputImage);
    }

    public ImageBuilder buildInPath(String baseDir, String outputImage) {
        final File dockerfile = new File(baseDir, "Dockerfile");
        return build(dockerfile, outputImage);
    }

    private ImageBuilder build(File dockerfile, String outputImage) {
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

        LOG.infof("Built image %s", imageId);

        return this;
    }

    public void push(String outputImage) {
        try {
            LOG.infof("Pushing image %s", outputImage);
            dockerClient.pushImageCmd(outputImage)
                    .exec(new ResultCallback.Adapter<>()).awaitCompletion();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
