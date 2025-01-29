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

package org.brainstorm.camel.datasets.component;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;

import org.brainstorm.transformer.quarkus.event.EventController;
import org.jboss.logging.Logger;

@ApplicationScoped
@Priority(1)
@Alternative
public class ${name}Controller implements EventController {
    private static final Logger LOG = Logger.getLogger(${name}Controller.class);

    @Override
    public boolean handle(String event) {
        // Your pipeline code goes here
        LOG.debugf("Polled Record:(%s)\n", event);

        return true;
    }
}
