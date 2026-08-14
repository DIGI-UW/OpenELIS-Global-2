package org.openelisglobal.testsupport.fixtures;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;

public class FixtureDatabaseTargetingTest {

    private final List<Path> temporaryDirectories = new ArrayList<>();

    @After
    public void cleanTemporaryDirectories() throws IOException {
        for (Path directory : temporaryDirectories) {
            try (var paths = Files.walk(directory)) {
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

    @Test
    public void fixtureLoaderValidatesExplicitContainerBeforeGeneratingFixtures() throws Exception {
        TestEnvironment environment = testEnvironment("""
                #!/bin/sh
                printf 'docker %s\n' "$*" >> "$CALL_LOG"
                if [ "$1" = "inspect" ]; then
                  echo false
                  exit 0
                fi
                exit 1
                """, """
                #!/bin/sh
                printf 'python %s\n' "$*" >> "$CALL_LOG"
                exit 0
                """);

        ProcessResult result = environment.run("src/test/resources/load-test-fixtures.sh",
                Map.of("DB_CONTAINER", "missing-database"), "--profile=harness", "--no-verify");

        assertNotEquals("a missing explicit database must fail closed", 0, result.exitCode());
        List<String> calls = Files.readAllLines(environment.callLog());
        assertFalse("target validation must occur before any fixture generation", calls.isEmpty());
        assertTrue("the first external operation must validate the requested database: " + calls,
                calls.get(0).startsWith("docker inspect --format {{.State.Running}} missing-database"));
        assertFalse("fixture generation must not run after target validation fails: " + calls,
                calls.stream().anyMatch(call -> call.startsWith("python ")));
    }

    @Test
    public void resetUsesExplicitContainerWithoutGlobalRediscovery() throws Exception {
        TestEnvironment environment = testEnvironment("""
                #!/bin/sh
                printf 'docker %s\n' "$*" >> "$CALL_LOG"
                case "$1" in
                  inspect)
                    echo true
                    ;;
                  ps)
                    echo wrong-database
                    ;;
                  exec)
                    cat >/dev/null
                    ;;
                esac
                exit 0
                """, null);

        ProcessResult result = environment.run("src/test/resources/reset-test-database.sh",
                Map.of("DB_CONTAINER", "ogc1054-database"), "--force");

        assertTrue("reset command failed: " + result.output(), result.exitCode() == 0);
        String calls = Files.readString(environment.callLog());
        assertTrue("reset must execute only against the explicit target: " + calls,
                calls.contains("docker exec -i ogc1054-database"));
        assertFalse("reset must not rediscover another stack: " + calls, calls.contains("docker ps "));
        assertFalse("reset must never touch a discovered fallback: " + calls, calls.contains("wrong-database"));
    }

    @Test
    public void explicitContainerFailsClosedWhenDockerIsUnavailable() throws Exception {
        TestEnvironment environment = testEnvironment(null, """
                #!/bin/sh
                printf 'python %s\n' "$*" >> "$CALL_LOG"
                exit 0
                """);

        ProcessResult loadResult = environment.runWithIsolatedPath("src/test/resources/load-test-fixtures.sh",
                Map.of("DB_CONTAINER", "ogc1054-database"), "--profile=harness", "--no-verify");
        ProcessResult resetResult = environment.runWithIsolatedPath("src/test/resources/reset-test-database.sh",
                Map.of("DB_CONTAINER", "ogc1054-database"), "--force");

        assertNotEquals("fixture load must not fall back from an explicit container", 0, loadResult.exitCode());
        assertNotEquals("fixture reset must not fall back from an explicit container", 0, resetResult.exitCode());
        assertTrue(loadResult.output(), loadResult.output().contains("requires Docker"));
        assertTrue(resetResult.output(), resetResult.output().contains("requires Docker"));
        assertFalse("fixture generation must not run after Docker validation fails",
                Files.exists(environment.callLog()));
    }

    private TestEnvironment testEnvironment(String dockerScript, String pythonScript) throws IOException {
        Path root = Files.createTempDirectory("fixture-database-targeting-");
        temporaryDirectories.add(root);
        Path binaryDirectory = Files.createDirectory(root.resolve("bin"));
        writeExecutable(binaryDirectory.resolve("dirname"), """
                #!/bin/sh
                exec /usr/bin/dirname "$@"
                """);
        if (dockerScript != null) {
            writeExecutable(binaryDirectory.resolve("docker"), dockerScript);
        }
        if (pythonScript != null) {
            writeExecutable(binaryDirectory.resolve("python3"), pythonScript);
        }
        return new TestEnvironment(binaryDirectory, root.resolve("calls.log"));
    }

    private void writeExecutable(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
        Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwx------"));
    }

    private record TestEnvironment(Path binaryDirectory, Path callLog) {
        private ProcessResult run(String script, Map<String, String> environment, String... arguments)
                throws Exception {
            List<String> command = new ArrayList<>();
            command.add("/bin/bash");
            command.add(Path.of(script).toAbsolutePath().toString());
            command.addAll(List.of(arguments));

            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
            builder.environment().put("PATH", binaryDirectory + ":" + System.getenv("PATH"));
            builder.environment().put("CALL_LOG", callLog.toString());
            builder.environment().putAll(environment);

            Process process = builder.start();
            assertTrue("fixture command timed out", process.waitFor(10, TimeUnit.SECONDS));
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ProcessResult(process.exitValue(), output);
        }

        private ProcessResult runWithIsolatedPath(String script, Map<String, String> environment, String... arguments)
                throws Exception {
            List<String> command = new ArrayList<>();
            command.add("/bin/bash");
            command.add(Path.of(script).toAbsolutePath().toString());
            command.addAll(List.of(arguments));

            ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
            builder.environment().put("PATH", binaryDirectory.toString());
            builder.environment().put("CALL_LOG", callLog.toString());
            builder.environment().putAll(environment);

            Process process = builder.start();
            assertTrue("fixture command timed out", process.waitFor(10, TimeUnit.SECONDS));
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return new ProcessResult(process.exitValue(), output);
        }
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
