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

package org.brainstorm.source.camel.common;

public final class Topics {
    private Topics() {}

    public static final String DATA_ACQUIRED = "data.acquired";
    public static final String ACQUISITION_EVENT = "data.acquisition.event";
    public static final String EVENT_DATA_READY = "event.data.ready";
    public static final String EVENT_DATA_CONSUMED = "event.data.consumed";
    public static final String EVENT_DATA_PROCESSED = "event.data.processed";
}
