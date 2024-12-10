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

package org.brainstorm.operator.util;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;

/**
 * Matching utilities to check if the desired resource matches the existing resource
 */
public final class Matchers {

    private Matchers() {}
    public static boolean match(Job desired, Job existing) {
        if (existing == null) {
            return false;
        } else {
            return desired.getSpec().getTemplate().getMetadata().getName()
                    .equals(existing.getSpec().getTemplate().getMetadata().getName()) &&
                    desired.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()
                            .equals(
                                    existing.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
        }
    }

    public static boolean match(Deployment desired, Deployment existing) {
        if (existing == null) {
            return false;
        } else {
            return desired.getSpec().getReplicas().equals(existing.getSpec().getReplicas()) &&
                    desired.getSpec().getTemplate().getSpec().getContainers().get(0).getImage()
                            .equals(
                                    existing.getSpec().getTemplate().getSpec().getContainers().get(0).getImage());
        }
    }

    public static boolean match(Service desired, Service existing) {
        if (existing == null) {
            return false;
        }

        final ServiceSpec existingSpec = existing.getSpec();
        final ServiceSpec desiredSpec = desired.getSpec();

        return existingSpec.getExternalName().equals(desiredSpec.getExternalName())
                && existingSpec.getPorts().equals(desiredSpec.getPorts());

    }
}
