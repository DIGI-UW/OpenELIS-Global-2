# Configuration Directory

This directory contains domain-specific configuration files that are
automatically loaded into the system during initialization.

## Structure

Each domain has its own subdirectory:

- `questionnaires/` - FHIR Questionnaire JSON files
- `roles/` - Dictionary configuration files (example)
- `[other-domains]/` - Additional domain configurations

## How It Works

1. Each domain has a handler that implements `DomainConfigurationHandler`
2. Configuration files are loaded from both:
   - Classpath: `src/main/resources/configuration/[domain]/*.[ext]`
   - Filesystem: `/var/lib/openelis-global/configuration/[domain]/*.[ext]`
     (mapped from `./configuration/[domain]` in Docker)
3. Checksums are tracked to avoid reinitializing unchanged files
4. Checksums are stored in
   `/var/lib/openelis-global/configuration/[domain]-checksums.properties`

## Docker Volume Mapping

The `configuration` directory is mapped to the container at
`/var/lib/openelis-global/configuration` via:

```yaml
volumes:
  - ./configuration:/var/lib/openelis-global/configuration
```

This allows you to:

- Add configuration files locally in `./configuration/[domain]/`
- Files will be automatically available in the container
- Changes persist across container restarts

## Configuration Properties

- `org.openelisglobal.configuration.dir` - Base configuration directory
  (default: `/var/lib/openelis-global/configuration`)
- `org.openelisglobal.configuration.autocreate` - Enable/disable
  auto-initialization (default: `true`)

## Adding New Domains

To add support for a new domain:

1. Create a handler class implementing `DomainConfigurationHandler`:

```java
@Component
public class MyDomainHandler implements DomainConfigurationHandler {
    @Override
    public String getDomainName() {
        return "mydomain";
    }

    @Override
    public String getFileExtension() {
        return "json";
    }

    @Override
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        // Process your configuration file
    }
}
```

2. Create the directory structure:

   - `configuration/mydomain/` - for filesystem files
   - `src/main/resources/configuration/mydomain/` - for classpath files

3. The system will automatically discover and use your handler via Spring's
   component scanning.
