# Microbiology browser performance qualification

- Commit: `18e78c75c8ff88ba4d10851bcf88e701420b2c48`
- Overall: **PASS**
- Percentiles: `nearest-rank-ceiling`

## Environment

- browser: `chromium`
- browserVersion: `145.0.7632.6`
- node: `v22.23.2`
- platform: `darwin-arm64`
- viewport: `1280x720`
- baseUrl: `https://127.0.0.1:48444`

## Data volume

- worklistCases: 200
- denseCaseIsolates: 5
- denseCaseReadings: 80

## Measurements

| Operation | Threshold (ms) | p50 (ms) | p95 (ms) | max (ms) | Result |
| --- | ---: | ---: | ---: | ---: | --- |
| worklist-initial-render | 2000.000 | 208.600 | 218.200 | 218.200 | PASS |
| dense-case-initial-render | 1000.000 | 202.000 | 220.900 | 220.900 | PASS |
| worklist-page-interaction | 300.000 | 55.800 | 58.200 | 58.200 | PASS |

The JSON evidence retains every measured sample.
