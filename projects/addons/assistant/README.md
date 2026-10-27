# AI assistant add-on

An OpenELIS add-on that answers questions about the results on screen with a
language model. It uses the add-on mechanism (`docs/addons/design.md`) and
changes nothing in OpenELIS.

- **No identifiers leave OpenELIS.** The add-on has one scope, `ai.context`.
  OpenELIS gives it the de-identified context of the orders on the user's screen
  (sex, age, results, earlier results as "N days before") and removes names,
  ids, lab numbers and dates from the user's question
  (`POST /ai/deidentify`). The add-on never sees a name, an id or a lab number,
  so it cannot send one to a model.
- **Kidney staging is computed, not guessed.** KDIGO G (eGFR) and A (ACR)
  stages are computed by the service and given to the model as facts; every
  answer is checked against them (`EgfrCheck`): a wrong stage given to the
  patient's value is corrected, a wrong range next to a stage is fixed, a
  mentioned neighbouring stage ("close to the G2 (60–89) boundary") is left
  alone, and every correction adds a visible warning line.
- **Every answer ends with a disclaimer** ("Suggestion for reference only — not
  a substitute for a physician's diagnosis."). It cannot be turned off.
- **Daily question limit per user** (`dailyQuestionLimit`), counted by the
  OpenELIS user id, per day in the laboratory's `timeZone`. Only answered
  questions count.
- **Network access is declared**: `network.egress` lists the model providers it
  may reach (shown on the permission screen). The service refuses any other
  host, whatever the configuration says.

## Where it appears

| Slot | What it does |
| --- | --- |
| `top-nav-actions-slot` | a header button opening the assistant for the current screen (no patient data) |
| `result-validation-side-panel-slot` | the cases on the validation page, called "Case 1…N" |
| `result-lookup-side-panel-slot` | the patient's current order and earlier results |

## Configuration (Administration → Extensions)

| Key | Meaning |
| --- | --- |
| `language` | `en` (default) or `vi`: the rules, the context and the added lines |
| `labName`, `productName` | filled into the rules |
| `provider` | `openai-compatible` (OpenRouter, DeepSeek, OpenAI…) or `claude-runner` |
| `fallbackProvider` | tried when the first one fails |
| `endpoint`, `model`, `apiKey` | the OpenAI-compatible API (`apiKey` is a secret field) |
| `runnerUrl`, `runnerToken` | a `claude -p` runner on the host (see below) |
| `dailyQuestionLimit`, `timeZone` | the cap |

The rules and reference tables are in `src/main/resources/rules/{en,vi}.md`. To
change them without a rebuild, mount a directory with your own `en.md` /
`vi.md` and set `ASSISTANT_RULES_DIR` (read again every minute).

## Run

```bash
./pack.sh pack assistant.zip            # the package (manifest + ui/); sign it: ./pack.sh pack assistant.zip <publisher>
podman build -t openelis-addon-assistant:1.0.0 .
```

Install the package (Administration → Extensions), then run the service with
the compose snippet OpenELIS prints, adding:

```yaml
    volumes: ["assistant-data:/data"]          # the usage log: cap and audit survive restarts
    environment:
      HTTPS_PROXY: http://egress-proxy:8888     # optional: an allowlisting proxy for network.egress
```

and the reverse-proxy route for the panel's API, with buffering off so answers
stream:

```nginx
location /addons/assistant/api/ {
    proxy_pass http://assistant:8080/api/;
    proxy_set_header Cookie "";
    proxy_buffering off;
    proxy_read_timeout 180s;
}
```

Set the configuration (at least `apiKey`), review the permissions and turn the
add-on on.

## Service API

Called by the panel with the frame's OpenELIS token (`Authorization: Bearer`),
which the service passes on to OpenELIS: an expired token or a disabled add-on
stops everything.

| | |
| --- | --- |
| `GET /api/status?page=&name=` | enabled, the user's cap, suggestions, current conversation |
| `POST /api/ask` | `{page, name, question, suggestionId, conversationId, newConversation}` → server-sent events `meta`, `delta`*, `reset`, `done` \| `error`; `?stream=0` or no `Accept: text/event-stream` → one JSON |
| `POST /api/continue` | a new conversation that starts from a summary of the previous one (same subject) |
| `GET /api/conversations[/{id}]` | the user's own conversations |
| `GET /health` | OpenELIS reachable with the service key, providers, allowed hosts |

`page` is `lookup`, `validation` or `other`. Conversations are per user and per
subject (OpenELIS's pseudonym of the patient, or the page): another patient
never shares a conversation, a history or a model session.

## Claude runner

`claude-runner` posts to a small service on the host that runs `claude -p`
without tools, one session per conversation (`--session-id` / `--resume`), with
the rules as system prompt. Give its URL (its host must be in
`network.egress`, e.g. `host.containers.internal:4190` in a deployment's own
manifest) and token.

## Tests

`mvn test`: the KDIGO tables and boundaries (59.5 → G3a, 60 → G2, 44.9 → G3b),
the post-answer check in Vietnamese (including the 19 October 2026 regression,
where a correct "close to the G2 (60–89) boundary" was rewritten to "G3a
(60–89)") and in English, the context text, conversation isolation between
patients and users, the cap, the rules files and the outbound host check.
