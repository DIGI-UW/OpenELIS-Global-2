# REST And URL Contract: OGC-788 M1

This file records the first engineering contract. Product acceptance remains in
`spec.md`.

## Runtime Lookup

`GET /rest/text-macros?context={context}&q={query}&limit={limit}`

- Requires an authenticated user.
- `context` is required and must be recognized.
- `q` is optional. A blank query returns the first active entries for the
  context; a dot/code or phrase fragment matches canonical code and expansion
  text case-insensitively.
- `limit` defaults to 20 and is capped at 50.
- Sort order is canonical code ascending for deterministic keyboard behavior.

Response:

```json
{
  "items": [
    {
      "id": "generated-id",
      "code": ".ng24",
      "expansionText": "No growth at 24 hours",
      "contexts": ["MICROBIOLOGY_CULTURE_ACTIVITY"]
    }
  ]
}
```

Runtime responses omit audit and package-management fields.

## Administrative List

`GET /rest/text-macros/admin?q={query}&context={context}&status={status}&sort={sort}&page={page}&pageSize={pageSize}`

- Requires the existing administrator role.
- `status`: `active`, `inactive`, or `all`.
- M1 sorts: `code:asc`, `code:desc`, `updated:desc`, `updated:asc`.
- `page` is one-based. `pageSize` accepts 10, 20, 50, or 100.
- Response includes `items`, `page`, `pageSize`, and `total`.

## Administrative Writes

- `POST /rest/text-macros/admin`
- `PUT /rest/text-macros/admin/{id}`

Request:

```json
{
  "code": ".ng24",
  "expansionText": "No growth at 24 hours",
  "contexts": ["MICROBIOLOGY_CULTURE_ACTIVITY"],
  "active": true
}
```

The request has no ID on create, actor, timestamp, provenance, source key, or
source version. M1 writes are local and the server derives attribution from the
authenticated request.

Expected errors use a stable `code` and human-safe message key:

- `400 INVALID_MACRO_CODE`
- `400 MACRO_TEXT_REQUIRED`
- `400 MACRO_CONTEXT_REQUIRED`
- `400 INVALID_MACRO_CONTEXT`
- `404 MACRO_NOT_FOUND`
- `409 MACRO_CODE_EXISTS`
- `403` for unauthorized administration

## Canonical Administration URL

`/admin/MacroLibrary?q=&context=all&status=active&sort=code:asc&page=1&pageSize=20&edit=`

- Default values may be omitted after canonicalization only if a reload produces
  identical state.
- `edit=new` opens create state.
- `edit={id}` opens the exact edit state.
- Closing create/edit removes only `edit` and preserves list state.
- Successful save returns to the preserved list state and refreshes visible
  data.

## Controlled Field Contract

The reusable runtime field receives:

- `id`, `labelText`, `value`, and `onChange` compatible with a controlled Carbon
  text area;
- one recognized `context`;
- optional existing Carbon text-area props.

It emits plain expanded text through `onChange`. It does not submit macro IDs,
change the parent form contract, or save data independently.
