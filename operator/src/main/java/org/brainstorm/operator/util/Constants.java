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

import java.io.File;

import org.brainstorm.api.pipeline.transformation.TransformationStep;

/**
 * Common constants (i.e.: paths from the containers and stuff like that) along with a few
 * utilities for them
 */
public class Constants {
    public static final String BASE_DIR = "/opt/brainstorm";
    public static final String CLASSPATH_DIR = BASE_DIR + "/classpath";
    public static final String DATA_DIR = BASE_DIR + "/data";
    public static final String ACQUISITION_DIR = BASE_DIR + "/acquisition";
    public static final String STEP_DIR = BASE_DIR + "/step";
    public static final String DEFAULT_TRANSFORM_SCRIPT_NAME = "transform.sh";

    public static String classpathPath() {
        return CLASSPATH_DIR;
    }

    public static String routePath() {
        return ACQUISITION_DIR + File.separator + "routes.yaml";
    }

    public static String getTransformationScript(TransformationStep transformationStep) {
        String script = transformationStep.getScript();
        if (script == null || script.isEmpty()) {
            script = DEFAULT_TRANSFORM_SCRIPT_NAME;
        }
        return stepPath(script);
    }

    public static String stepPath(String script) {
        return STEP_DIR + File.separator + script;
    }
}
