# NCE deep links from EQA (OGC-611)

The EQA oversight surfaces link into the main NCE register rather than
re-implementing it. This is the URL contract they use.

## The link

```
/NceDashboard?source=eqa
```

`/NceDashboard` is the register list — the same page the QA Overview tiles
deep-link to with `?status=` and `?severity=`. Every filter seeds from the query
string once, on mount, and the user can clear or change it afterwards; nothing
about a deep link is sticky.

| Parameter  | Values                                                | Effect                                       |
| ---------- | ----------------------------------------------------- | -------------------------------------------- |
| `source`   | `eqa`                                                 | only NCEs whose trigger source starts `EQA_` |
| `status`   | `Pending`, `Under Investigation`, `CAPA`, `Completed` | exact status match                           |
| `severity` | `CRITICAL`, `MAJOR`, `MINOR`, `LOW`                   | exact severity match                         |

Parameters combine, so `?source=eqa&status=Pending` is the open-EQA-NCE view.

## Why a prefix, not a single value

An EQA-triggered NCE carries one of two trigger sources on `nc_event`:

| `trigger_source_type`     | Raised by                                                 | `trigger_source_id`   |
| ------------------------- | --------------------------------------------------------- | --------------------- |
| `EQA_UNACCEPTABLE`        | the tiered rules, automatically, on an unacceptable score | participant result id |
| `EQA_FOLLOWUP_ESCALATION` | a supervisor escalating a Follow-Up Queue row             | follow-up row id      |

Both are EQA in origin and belong in the same filtered view, so the filter
matches the shared `EQA_` prefix rather than either literal. The prefix is
`EqaScoreNceService.TRIGGER_SOURCE_PREFIX` on the server side; adding a third
EQA trigger source needs no change to the register or to this contract, as long
as it keeps the prefix.

## Who links here

- **Lab EQA Performance → Coverage**, the "EQA-triggered NCEs (12 mo)" tile
  (FR-V2.3-07). Its count comes from the same prefix, so the number on the tile
  and the rows behind the link are the same set.
- The Follow-Up Queue reports the NCE number it just raised, but does not link;
  the escalation response carries the number and the register is one click away
  through the tile.
