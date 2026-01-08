# Questionnaire Configuration

This directory contains FHIR Questionnaire JSON files that are automatically
loaded into the FHIR server during system initialization.

## Structure

Place questionnaire JSON files in this directory. Each file should contain a
valid FHIR R4 Questionnaire resource.

- `example-questionnaire.json` – generic starter questionnaire.
- `generic-sample-logbook-questionnaire.json` – captures logbook-specific
  metadata (e.g., serial number, project) used by the generic sample import.

## Example

See `example-questionnaire.json` for a sample questionnaire structure.

## Checksum Tracking

The system automatically tracks file checksums to avoid reinitializing unchanged
questionnaires. Checksums are stored in
`/var/lib/openelis-global/configuration/questionnaires-checksums.properties`.

## File Format

Each questionnaire file should be a valid FHIR R4 Questionnaire resource in JSON
format. The questionnaire will be assigned a UUID if it doesn't have an ID.

## Docker Volume Mapping

Files in `./configuration/questionnaires/` are automatically mapped to
`/var/lib/openelis-global/configuration/questionnaires/` in the container.

## Configuration

Enable/disable automatic loading via the property:

```
org.openelisglobal.configuration.autocreate=true
```
