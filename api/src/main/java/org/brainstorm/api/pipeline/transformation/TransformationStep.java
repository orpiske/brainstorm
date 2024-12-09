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

package org.brainstorm.api.pipeline.transformation;

import java.util.Objects;

public class TransformationStep {
    private String name;
    private String image;
    private String consumesFrom;
    private String producesTo;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getProducesTo() {
        return producesTo;
    }

    public void setProducesTo(String producesTo) {
        this.producesTo = producesTo;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TransformationStep that = (TransformationStep) o;
        return Objects.equals(name, that.name) && Objects.equals(image, that.image) && Objects.equals(
                consumesFrom, that.consumesFrom) && Objects.equals(producesTo, that.producesTo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, image, consumesFrom, producesTo);
    }

    @Override
    public String toString() {
        return "TransformationStep{" +
                "name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", consumesFrom='" + consumesFrom + '\'' +
                ", producesTo='" + producesTo + '\'' +
                '}';
    }
}
