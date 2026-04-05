# Tier 3 Analyzer Blockers — Follow-up Tracking

**Channel:** `#ext-madagascar-e-sil`  
**Plan:** File-Based Analyzer Mock Testing Readiness

## Blocked — No Real Exports Yet

| Analyzer     | Jira    | Blocker                                                                | Follow-up                                       |
| ------------ | ------- | ---------------------------------------------------------------------- | ----------------------------------------------- |
| **HYDRASYS** | OGC-332 | Pattern C flat file, LOE 3. No real exports found in Slack/Confluence. | Request sample export via #ext-madagascar-e-sil |

## Deprioritized

| Analyzer          | Jira    | Reason                                                    |
| ----------------- | ------- | --------------------------------------------------------- |
| **Attune CytPix** | OGC-350 | Image-only exports (png/bmp/jpg). Deprioritized by Casey. |

## Unblocked (Implemented)

- QuantStudio 7 Flex / QS5 — XLS mock + profile
- Tecan Infinite F50 — profile v2.0 + Magellan CSV plate-grid mock +
  PlateGridNormalizer (BOM, comma delim)
- Multiskan FC — profile v2.0 + dual plate-grid mock (French locale) +
  PlateGridNormalizer (dual-grid, semicolons, comma decimals)
- Wondfo Finecare FS-205 — profile v1.0 + 40-col CSV mock from OGC-344 spec +
  skipRows support
- FluoroCycler XT — profile + mock (12-col Excel)
- DT-Prime — XmlAnalyzerReader + profile + mock
