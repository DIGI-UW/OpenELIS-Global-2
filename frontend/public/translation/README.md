# Deployment translation overrides

Message files placed here override the bundled UI translations, so a deployment
can reword any string in the interface without editing source or rebuilding the
frontend image.

Nothing in this directory has any effect until
`OVERRIDE_DEFAULT_TRANSLATION=true` is set in
`volume/properties/SystemConfiguration.properties`. With it unset or false the UI
uses the bundled bundles exactly as shipped.

## File names

One JSON file per locale, named exactly as under `frontend/src/languages/` — the
underscored form, not the BCP-47 tag:

```
en.json      fr.json      mg.json
en_GB.json   fr_MG.json   am_ET.json   si_LK.json
```

This is the form Transifex emits, so a deployment can point its own Transifex
project straight at this directory with no renaming step.

Ship only the locales you customize. A locale with no file here simply uses its
bundled bundle.

## Content

The same flat shape as the bundled files — message id to string:

```json
{
  "sidenav.label.dashboard": "Home",
  "patient.label.firstName": "Given name"
}
```

Overriding is **per key**. A file with two entries rewords those two strings and
leaves every other message, including the English fallback layered under each
locale, exactly as shipped. You never have to copy a whole bundle to change one
line.

A regional locale also picks up its base language's file: a user running `fr_MG`
gets `fr.json` layered under `fr_MG.json`, so rewording `fr.json` alone still
reaches them.

## Deploying

The files are served at `/translation/<locale>.json` in both frontend images.

**Production** (nginx image, e.g. `itechuw/openelis-global-2-frontend`) — mount
your directory over this one:

```yaml
frontend.openelis.org:
  image: itechuw/openelis-global-2-frontend:develop
  volumes:
    - ./configs/translation:/usr/share/nginx/html/translation:ro
```

Note that the mount replaces this directory wholesale, including this README and
the example below. That is harmless: a locale whose file is absent falls back to
its bundled bundle.

**Development** (Vite image, or `npm start` on the host) — nothing to configure.
`dev.docker-compose.yml` already mounts `./frontend/public`, and Vite serves this
directory at the web root, so a file dropped here is live immediately.

In both cases a change to a file takes effect on the next **browser reload**. No
container restart, no rebuild. Note that Vite does not hot-reload files in
`public/` — they sit outside the module graph — so a reload is needed in
development too.

## Turning it off again

`OVERRIDE_DEFAULT_TRANSLATION` behaves like every other entry in
`SystemConfiguration.properties`: the effective value is remembered in
`TotalSystemConfiguration.properties`, so **commenting the line out does not
unset it**. To switch overriding off, set it explicitly:

```
OVERRIDE_DEFAULT_TRANSLATION=false
```

and restart the backend. With the flag off the UI does not even request these
files.

## Local testing

Files you drop here for testing are ignored by git (see `.gitignore` in this
directory) so a deployment's translations cannot be committed into the core
repository by accident. `README.md` and `example.en.json` are tracked.
