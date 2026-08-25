package org.openelisglobal.analyzermigration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class AnalyzerMigrationCli {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final GatewayFactory gatewayFactory;

    @FunctionalInterface
    interface GatewayFactory {
        Gateways create(String bridgeUrl, String openElisUrl, String sessionCookie);
    }

    record Gateways(AnalyzerMigrationRun.BridgeGateway bridge, AnalyzerMigrationRun.OpenElisGateway openElis) {
    }

    public AnalyzerMigrationCli() {
        this((bridgeUrl, openElisUrl, sessionCookie) -> new Gateways(new HttpBridgeGateway(bridgeUrl),
                new HttpOpenElisGateway(openElisUrl, sessionCookie)));
    }

    AnalyzerMigrationCli(GatewayFactory gatewayFactory) {
        this.gatewayFactory = gatewayFactory;
    }

    public static void main(String[] args) {
        int exitCode = new AnalyzerMigrationCli().run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    int run(String[] args, PrintStream output, PrintStream errors) {
        try {
            if (args.length == 0) {
                throw new IllegalArgumentException("Mode is required: plan, apply, or verify");
            }
            String mode = args[0].toLowerCase(java.util.Locale.ROOT);
            Map<String, String> options = options(args);
            ObjectNode source = readObject(requiredPath(options, "--source"));
            Path outputPath = requiredPath(options, "--output");
            String runId = required(options, "--run-id");
            Instant startedAt = Instant.now();
            ObjectNode manifest = switch (mode) {
            case "plan" -> new AnalyzerMigrationRun(new AnalyzerMigrationPlanner()).plan(runId, startedAt,
                    Instant.now(), source, readObject(requiredPath(options, "--selections")),
                    readProfiles(requiredPath(options, "--profiles")));
            case "apply" -> {
                Gateways gateways = gateways(options);
                yield new AnalyzerMigrationRun(new AnalyzerMigrationPlanner(), gateways.bridge(), gateways.openElis())
                        .apply(runId, startedAt, Instant.now(), source,
                                readObject(requiredPath(options, "--selections")),
                                readProfiles(requiredPath(options, "--profiles")));
            }
            case "verify" -> {
                Gateways gateways = gateways(options);
                yield new AnalyzerMigrationRun(new AnalyzerMigrationPlanner(), gateways.bridge(), gateways.openElis())
                        .verify(runId, startedAt, Instant.now(), source,
                                readObject(requiredPath(options, "--apply-manifest")));
            }
            default -> throw new IllegalArgumentException("Unsupported mode: " + mode);
            };
            Path parent = outputPath.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            JSON.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), manifest);
            output.println(outputPath.toAbsolutePath().normalize());
            return 0;
        } catch (Exception exception) {
            errors.println(exception.getMessage());
            return 2;
        }
    }

    private Gateways gateways(Map<String, String> options) throws Exception {
        String cookie = Files.readString(requiredPath(options, "--openelis-cookie-file")).trim();
        if (cookie.isBlank()) {
            throw new IllegalArgumentException("OpenELIS cookie file is empty");
        }
        return gatewayFactory.create(required(options, "--bridge-url"), required(options, "--openelis-url"), cookie);
    }

    private static Map<String, String> options(String[] args) {
        if ((args.length - 1) % 2 != 0) {
            throw new IllegalArgumentException("Every option requires one value");
        }
        Map<String, String> options = new LinkedHashMap<>();
        for (int index = 1; index < args.length; index += 2) {
            String previous = options.put(args[index], args[index + 1]);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate option: " + args[index]);
            }
        }
        return options;
    }

    private static ObjectNode readObject(Path path) throws Exception {
        JsonNode document = JSON.readTree(path.toFile());
        if (!(document instanceof ObjectNode object)) {
            throw new IllegalArgumentException("Expected a JSON object: " + path);
        }
        return object;
    }

    private static ArrayNode readProfiles(Path directory) throws Exception {
        if (!Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Profile directory does not exist: " + directory);
        }
        ArrayNode profiles = JSON.createArrayNode();
        try (Stream<Path> paths = Files.list(directory)) {
            for (Path path : paths.filter(file -> file.getFileName().toString().endsWith(".json")).sorted().toList()) {
                JsonNode document = JSON.readTree(path.toFile());
                if (!(document instanceof ObjectNode)) {
                    throw new IllegalArgumentException("Expected a profile JSON object: " + path);
                }
                profiles.add(document);
            }
        }
        return profiles;
    }

    private static Path requiredPath(Map<String, String> options, String name) {
        return Path.of(required(options, name));
    }

    private static String required(Map<String, String> options, String name) {
        String value = options.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required option is missing: " + name);
        }
        return value;
    }
}
