# Shipped reference data labels (OGC-1507)

OpenELIS ships reference lists that users pick from: NCE categories and types,
and dictionary entries such as result options and sample acceptance and QA
checklist items. Each value OpenELIS ships has a display key in
`frontend/src/languages/en.json`, so its label is translated in Transifex like
the rest of the interface.

## How a label is chosen

For a value whose display key is in `en.json`, in the user's language:

1. the lab's own label for that language (an override saved in Admin >
   Translation Management);
2. the key's translation from the language file, when Transifex has one;
3. the translation the shipped configuration file carried, while Transifex has
   none yet;
4. the key's English.

A value whose key is not in `en.json` is the site's own (created in the app or
loaded from a country configuration file) and keeps its stored labels.

Translation Management shows, for each language, whether a label is **Shipped**,
a **Site override** or a **Site value**. An override can be returned to the
shipped translation with **Reset to shipped translation**; it cannot be blank.
The Dictionary list shows the same origin per entry.

## Keys

`<module>.<list>.<value>`, lowerCamelCase segments:
`dictionary.testResults.positive`, `nce.category.general`. The 22 NCE types in
`nce-types.csv` keep the existing keys already translated in Transifex
(`nce.type.sample_collection_error`, ...). A key is never reused for another
meaning.

`scripts/ci/check-shipped-display-keys.py` fails the build when a shipped
configuration file names a key that is not in `en.json`. With `--list` it prints
every key the shipped files use; an unused-key scan of `en.json` must count
those as used, because no source file names them.

## Files in this folder

- `translations-fr.json`, `translations-es.json`, `translations-id.json`: the
  translations the shipped configuration files carry for keys Transifex has not
  translated yet, keyed by display key, for a Transifex maintainer to import.
  Once they are in Transifex, the `localization:fr/es/id` columns can be removed
  from the shipped files.
- `developer-notes.csv`: a note per key saying which list it belongs to, for the
  translators. The `en.json` resource is key-value JSON, which carries no notes,
  so they are added in Transifex from this file.
- `audit-dictionary-labels.sql`: a read-only audit of the dictionary entries an
  instance holds that OpenELIS does not ship (see below).

## Upgrading an instance

At start, OpenELIS links each existing NCE category and type whose key is not in
`en.json` to a shipped key by its English name, then by a list of older
spellings, and writes a line per list to the application log (`linked by name`,
`linked by alias`, `already linked`, `unmatched`). Unmatched rows are left as
they are and stay the site's own. Nothing is deleted or merged, so every NCE
keeps its category and type.

Stored labels record who wrote them. Labels the 3.5.x upgrades copied from a
value's own name are recorded as OpenELIS's; every other existing label is kept
as the lab's own, so an earlier edit stays in force.

## Auditing an instance's dictionary

```bash
python3 scripts/ci/check-shipped-display-keys.py --list > docs/i18n/shipped-reference-data/shipped-display-keys.txt
cd docs/i18n/shipped-reference-data
psql -U clinlims -d clinlims -f audit-dictionary-labels.sql
```

The first table counts, per list, the entries OpenELIS ships, whether tests
answer from the list, and the signs of entries that need a decision: French in
the English slot, numbers or MIC values, long free text. The second lists the
site's entries with a suggestion each:

- **retire**: in a list tests answer from, but no test offers it and no result
  holds it. Deactivate it in Admin > Dictionary.
- **move to the French slot**: the English label holds French text. Enter the
  English label, and the French one in Translation Management.
- **keep untranslated**: a number, titer or MIC value.
- **review**: free text kept as an answer option, often an old comment.
- **translate in the localization editor**: a site value without a French label.

The audit changes nothing. Retiring and relabelling stay the lab's decision.
