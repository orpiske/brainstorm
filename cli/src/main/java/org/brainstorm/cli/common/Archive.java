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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;


public class Archive {
	private static final Logger LOG = Logger.getLogger(Archive.class);

	private final List<BrainstormEntry> filesToArchive;

	public Archive(List<BrainstormEntry> filesToArchive) {
		this.filesToArchive = filesToArchive;
    }

	public Path compress() throws IOException {
		final Path path = Files.createTempFile("brainstorm", ".tar.gz");
		LOG.infof("Compressing %d files to %s", filesToArchive.size(), path);

		path.toFile().deleteOnExit();

		try (OutputStream fo = Files.newOutputStream(path);
			 OutputStream gzo = new GzipCompressorOutputStream(fo);
			 ArchiveOutputStream<TarArchiveEntry> o = new TarArchiveOutputStream(gzo)) {

			for (BrainstormEntry bsEntry : filesToArchive) {
				// maybe skip directories for formats like AR that don't store directories
				TarArchiveEntry entry;
				if (bsEntry.getType() == BrainstormEntry.EntryType.ACQUISITION) {
					entry = o.createArchiveEntry(bsEntry.getFile(), "acquisition" + File.separator + bsEntry.getFile().getName());
				} else {
					if (bsEntry.getType() == BrainstormEntry.EntryType.CODE) {
						entry = o.createArchiveEntry(bsEntry.getFile(), "classpath" + File.separator + bsEntry.getFile().getName());
					} else {
						entry = o.createArchiveEntry(bsEntry.getFile(), "other" + File.separator + bsEntry.getFile().getName());
					}
				}

				// potentially add more flags to entry
				o.putArchiveEntry(entry);
				if (bsEntry.getFile().isFile()) {
					try (InputStream i = Files.newInputStream(bsEntry.getFile().toPath())) {
						IOUtils.copy(i, o);
					}
				}
				o.closeArchiveEntry();
			}
		}

		return path;
	}



}
