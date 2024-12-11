package org.brainstorm.pipeline;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("pipeline.brainstorm.org")
public class Pipeline extends CustomResource<PipelineSpec, PipelineStatus> implements Namespaced { }