package org.openelisglobal.addon.packaging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

/**
 * Reads an add-on package (a zip) into memory, refusing anything a signed
 * package has no reason to contain. Nothing is ever written to disk: files are
 * kept as bytes, so a zip entry can never escape a target directory.
 *
 * <p>
 * Sizes are counted while inflating, not taken from the zip headers, which an
 * attacker controls.
 */
public final class AddonPackageReader {

    /** Largest package accepted (the zip as uploaded). */
    public static final int MAX_PACKAGE_BYTES = 10 * 1024 * 1024;
    /** Largest total of all files once inflated. */
    public static final int MAX_INFLATED_BYTES = 20 * 1024 * 1024;
    public static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    public static final int MAX_FILES = 500;

    private static final String NAME_PATTERN = "[A-Za-z0-9._-]+(/[A-Za-z0-9._-]+)*";

    private AddonPackageReader() {
    }

    /** File name → content, sorted by name. */
    public static Map<String, byte[]> read(byte[] zip) {
        if (zip == null || zip.length == 0) {
            throw new AddonPackageException("package.empty", "The package is empty");
        }
        if (zip.length > MAX_PACKAGE_BYTES) {
            throw new AddonPackageException("package.tooLarge", "The package is larger than 10 MB");
        }
        Map<String, byte[]> files = new TreeMap<>();
        long total = 0;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            while ((entry = in.getNextEntry()) != null) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                checkName(name);
                if (files.containsKey(name)) {
                    throw new AddonPackageException("package.duplicate", "The package contains " + name + " twice");
                }
                if (files.size() >= MAX_FILES) {
                    throw new AddonPackageException("package.tooManyFiles", "The package has too many files");
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                int n;
                while ((n = in.read(buffer)) > 0) {
                    total += n;
                    if (out.size() + n > MAX_FILE_BYTES || total > MAX_INFLATED_BYTES) {
                        throw new AddonPackageException("package.tooLarge", "The package inflates to too much data");
                    }
                    out.write(buffer, 0, n);
                }
                files.put(name, out.toByteArray());
            }
        } catch (ZipException e) {
            throw new AddonPackageException("package.notZip", "The package is not a valid zip file");
        } catch (IOException e) {
            throw new AddonPackageException("package.notZip", "The package could not be read");
        }
        if (files.isEmpty()) {
            throw new AddonPackageException("package.notZip", "The package is not a valid zip file");
        }
        return files;
    }

    static void checkName(String name) {
        if (name.length() > 160 || !name.matches(NAME_PATTERN)) {
            throw new AddonPackageException("package.badPath", "The package contains an invalid path: " + name);
        }
        for (String part : name.split("/")) {
            if (part.equals(".") || part.equals("..")) {
                throw new AddonPackageException("package.badPath", "The package contains an invalid path: " + name);
            }
        }
    }
}
