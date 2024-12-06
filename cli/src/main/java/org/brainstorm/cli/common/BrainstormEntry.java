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
import java.util.Objects;

public class BrainstormEntry {
    public enum EntryType {
        ACQUISITION,
        CODE
    }

    private EntryType type;
    private File file;

    public BrainstormEntry(EntryType type, String file) {
        this.type = type;
        this.file = new File(file);
    }

    public BrainstormEntry(EntryType type, File file) {
        this.type = type;
        this.file = file;
    }

    public EntryType getType() {
        return type;
    }

    public File getFile() {
        return file;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BrainstormEntry that = (BrainstormEntry) o;
        return type == that.type && Objects.equals(file, that.file);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, file);
    }
}
