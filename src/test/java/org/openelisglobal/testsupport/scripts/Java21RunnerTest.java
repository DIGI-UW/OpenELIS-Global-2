package org.openelisglobal.testsupport.scripts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class Java21RunnerTest {

    @Test
    public void runnerHonorsExistingJava21HomeWithoutSdkman() throws Exception {
        Path root = Files.createTempDirectory("java21-runner-");
        try {
            Path javaHome = Files.createDirectories(root.resolve("java-home/bin"));
            Path commands = Files.createDirectory(root.resolve("commands"));
            Path callLog = root.resolve("calls.log");
            writeExecutable(javaHome.resolve("java"), """
                    #!/bin/sh
                    echo '    java.specification.version = 21' >&2
                    echo '    java.home = %s' >&2
                    """.formatted(root.resolve("java-home")));
            writeExecutable(commands.resolve("probe-java-home"), """
                    #!/bin/sh
                    printf '%s\n' "$JAVA_HOME" > "$CALL_LOG"
                    """);

            ProcessBuilder builder = new ProcessBuilder("/bin/bash",
                    Path.of("scripts/run-java21").toAbsolutePath().toString(), "probe-java-home")
                    .redirectErrorStream(true);
            builder.environment().put("JAVA_HOME", root.resolve("java-home").toString());
            builder.environment().remove("JAVA_HOME_21");
            builder.environment().put("SDKMAN_DIR", root.resolve("missing-sdkman").toString());
            builder.environment().put("CALL_LOG", callLog.toString());
            builder.environment().put("PATH", javaHome + ":" + commands + ":/usr/bin:/bin");

            Process process = builder.start();
            assertTrue("Java runner timed out", process.waitFor(10, TimeUnit.SECONDS));
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            assertEquals("runner failed: " + output, 0, process.exitValue());
            assertEquals(root.resolve("java-home").toString(), Files.readString(callLog).trim());
        } finally {
            deleteRecursively(root);
        }
    }

    private static void writeExecutable(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            });
        }
    }
}
