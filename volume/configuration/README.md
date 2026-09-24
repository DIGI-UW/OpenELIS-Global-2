# Configuration Directory

This directory contains domain-specific configuration files that are
automatically loaded into the system during initialization.

## Structure

Each domain has its own subdirectory:

- `questionnaires/` - FHIR Questionnaire JSON files
- `dictionaries/` - Dictionary entries CSV files
- `roles/` - Role configuration files (example)
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
  (default: `/var/lib/openelis-global/configuration/backend`)
- `org.openelisglobal.configuration.autocreate` - Enable/disable
  auto-initialization (default: `true`)

## Test Catalog Domains

The catalog is authored as CSV, one domain per subdirectory. They load in this
order, so a file may refer to anything an earlier domain created:

| Order | Domain               | One row is                                | Key columns                                                                                                                                                          |
| ----- | -------------------- | ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 100   | `test-sections/`     | a lab unit                                | `testSectionName` (max 20 chars)                                                                                                                                     |
| 100   | `sample-types/`      | a specimen                                | `description` (max 40), `localAbbreviation` (max 10)                                                                                                                 |
| 200   | `tests/`             | a test, linked to every specimen it lists | `testName`, `testSection`, `sampleType` (`\|`-separated), `loinc`, `localCode`, `reportingName`, `domain`, `amr`, `isReportable`, `notifyResults`, `localization:xx` |
| 210   | `result-components/` | one result of a test                      | `testName`, `code`, `label`, `resultType`, `unitOfMeasure`, `significantDigits`, `isPrimary`                                                                         |
| 300   | `panels/`            | a panel and its members                   | `panelName`, `sampleTypes`, `tests`                                                                                                                                  |
| 310   | `test-results/`      | a select-list option                      | `testName`, `resultType`, `resultValue`                                                                                                                              |
| 320   | `result-limits/`     | a numeric reference range                 | `testName`, `sampleType`, `componentCode`, `gender`, `minAge`, `maxAge`, `lowNormal`, `highNormal`, `lowCritical`, `highCritical`                                    |
| 320   | `terminology/`       | a standard code for a test                | `testName`, `source` (LOINC/SNOMED/CIEL/OCL), `code`, `relationship`, `displayName`                                                                                  |
| 320   | `sample-handling/`   | a test's storage and disposal rules       | `testName`, `storageCondition`, `storageDuration`, `disposalMethod`                                                                                                  |
| 330   | `reflex-rules/`      | one condition and action of a rule        | `ruleName`, `overall`, `conditionTest`, `relation`, `value`, `reflexTest`                                                                                            |

Every catalog row is loaded in its own transaction: a row the database rejects
is skipped with its line number and the reason, and the rest of the file still
loads. Each file ends with a line you can grep for:

```
SUMMARY file=tests-cphl.csv domain=tests created=21 updated=3 skipped=1
```

A name a file uses for a lab unit, specimen or test that the catalog does not
know is not silently dropped: it waits in **Admin → Import Catalog (CSV) → Needs
your decision**, where it can be pointed at the right record. Choosing "Remember
this name" keeps the spelling, so later imports resolve it by themselves.

Files can also be uploaded from that page. Preview evaluates each row against
the catalog as it stands and keeps nothing, so on a first load a row that needs
a lab unit, specimen or test another file of the same upload creates is listed
as skipped with that reason; Apply writes the files into this directory and
loads them in the order above, which resolves them. Start-up and the page stay
one mechanism.

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
