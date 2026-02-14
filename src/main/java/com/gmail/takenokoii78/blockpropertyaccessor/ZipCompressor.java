package com.gmail.takenokoii78.blockpropertyaccessor;

import org.jspecify.annotations.NullMarked;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@NullMarked
public class ZipCompressor {
    private final Path directory;

    public ZipCompressor(Path directory) {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException();
        }

        this.directory = directory;
    }

    public void compress(Path out) {
        if (Files.exists(directory)) {
            try (final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(out.toFile())))) {
                directory(directory.getNameCount(), directory, zip);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void directory(int rootCount, Path path, ZipOutputStream zip) throws IOException {
        try (final Stream<Path> stream = Files.list(path)) {
            stream.forEach(p -> {
                try {
                    final String name = p.subpath(rootCount, p.getNameCount())
                        .toString()
                        .replace(File.separatorChar, '/');

                    if (Files.isDirectory(p)) {
                        zip.putNextEntry(new ZipEntry(name + '/'));
                        zip.closeEntry();
                        directory(rootCount, p, zip);
                    }
                    else {
                        zip.putNextEntry(new ZipEntry(name));
                        Files.copy(p, zip);
                        zip.closeEntry();
                    }
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}
