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

package org.brainstorm.api.pipeline.sink;

public class SinkStep {
    private String image;
    private String consumesFrom;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getConsumesFrom() {
        return consumesFrom;
    }

    public void setConsumesFrom(String consumesFrom) {
        this.consumesFrom = consumesFrom;
    }

    @Override
    public String toString() {
        return "SinkStep{" +
                "image='" + image + '\'' +
                ", producesTo='" + consumesFrom + '\'' +
                '}';
    }
}
