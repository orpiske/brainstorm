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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;

/**
 * Zip archive support
 * 
 * @author Otavio R. Piske <angusyoung@gmail.com>
 */
public class Archive {

	private final List<File> filesToArchive;
	private final String name;

	public Archive(List<File> filesToArchive, String name) {
		this.filesToArchive = filesToArchive;
        this.name = name;
    }

	public Path compress() throws IOException {
		final Path path = Paths.get(name);

		try (OutputStream fo = Files.newOutputStream(path);
			 OutputStream gzo = new GzipCompressorOutputStream(fo);
			 ArchiveOutputStream o = new TarArchiveOutputStream(gzo)) {

			for (File f : filesToArchive) {
				// maybe skip directories for formats like AR that don't store directories
				ArchiveEntry entry = o.createArchiveEntry(f, f.getName());
				// potentially add more flags to entry
				o.putArchiveEntry(entry);
				if (f.isFile()) {
					try (InputStream i = Files.newInputStream(f.toPath())) {
						IOUtils.copy(i, o);
					}
				}
				o.closeArchiveEntry();
			}
		}

		return path;
	}



}
