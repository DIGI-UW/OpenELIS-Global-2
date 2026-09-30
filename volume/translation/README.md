# Deployment translation overrides

Message files placed here override the bundled UI translations, so a deployment
can reword any string in the interface without editing source or rebuilding the
frontend image.

Nothing here has any effect until overriding is switched on. There are two ways,
and either is enough:

- **From the UI** — Admin → Site Information Menu
  (`/MasterListsPage/SiteInformationMenu`), the `overrideDefaultTranslation`
  row. Select it, press Modify, choose `true`, Save. This takes effect
  immediately: no restart, no redeploy.
- **From the properties file** — `OVERRIDE_DEFAULT_TRANSLATION=true` in
  `../properties/SystemConfiguration.properties`, which overrides whatever the
  UI holds and needs a backend restart.

Left alone the setting is `false`: the UI uses the bundled bundles exactly as
shipped and does not even request these files.

## File names

One JSON file per locale, named exactly as under `frontend/src/languages/` — the
underscored form, not the BCP-47 tag:

```
en.json      fr.json      mg.json
en_GB.json   fr_MG.json   am_ET.json   si_LK.json
```

This is the form Transifex emits, so a deployment can point its own Transifex
project straight at this directory with no renaming step.

Ship only the locales you customize. A locale with no file here uses its bundled
bundle.

## Content

The same flat shape as the bundled files — message id to string:

```json
{
  "login.title": "Sign in",
  "label.button.save": "Keep"
}
```

Overriding is **per key**. A file with two entries rewords those two strings and
leaves every other message, including the English fallback layered under each
locale, exactly as shipped. You never copy a whole bundle to change one line.

A regional locale also picks up its base language's file: a user running `fr_MG`
gets `fr.json` layered under `fr_MG.json`, so rewording `fr.json` alone still
reaches them.

## How it reaches the UI

The frontend serves this directory at `/translation/<locale>.json` and layers
what it finds over its bundled messages. The files are fetched at runtime rather
than compiled in, which is what lets a deployment change them without a rebuild.

**Development** — already wired. `dev.docker-compose.yml` mounts this directory
into the frontend container:

```yaml
- ./volume/translation:/app/public/translation
```

**Production** — mount your own directory into the frontend service:

```yaml
frontend.openelis.org:
  image: itechuw/openelis-global-2-frontend:develop
  volumes:
    - ./configs/translation:/usr/share/nginx/html/translation:ro
```

### When a change takes effect

|                        | Editing a file that is already there | Adding a locale file that was not there |
| ---------------------- | ------------------------------------ | --------------------------------------- |
| **Production** (nginx) | browser reload                       | browser reload                          |
| **Development** (Vite) | browser reload                       | **restart the frontend container**      |

Neither ever needs a rebuild. nginx reads from disk on every request, so both
cases are immediate. The Vite dev server takes its list of statically served
files when it starts, so a file that appears afterwards is not served until it
restarts — you will get the app's HTML instead of your JSON. Once the file
exists, editing it needs only a reload:

```
docker compose -f dev.docker-compose.yml restart frontend.openelis.org
```

Adding a locale is a rare, deliberate act, so this only bites the first time you
introduce one.

## Turning it off again

If you switched it on from the UI, switch it back the same way — select the row,
Modify, `false`, Save.

If you set it in `SystemConfiguration.properties`, note that it behaves like
every other entry there: the effective value is remembered in
`TotalSystemConfiguration.properties`, so **commenting the line out does not
unset it**. Set it explicitly instead:

```
OVERRIDE_DEFAULT_TRANSLATION=false
```

and restart the backend. While that line is present it wins over the UI, so
clear it if you want the toggle to be the thing in charge.

## If something is wrong with a file

Every failure falls back to the bundled messages rather than breaking the
screen: a missing file, an empty one, invalid JSON, or a value that is not a
string (that one entry is dropped, the rest of the file still applies). Check
the browser console for `[translation]` messages.

Locale files you drop here are ignored by git (see `.gitignore`) so a
deployment's translations cannot be committed into the core repository by
accident.
