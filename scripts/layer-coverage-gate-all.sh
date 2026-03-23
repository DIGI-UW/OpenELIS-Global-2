#!/usr/bin/env bash

set -euo pipefail

CSV_PATH="${1:-target/site/jacoco/jacoco.csv}"

if [[ ! -f "$CSV_PATH" ]]; then
  echo "ERROR: JaCoCo CSV not found at $CSV_PATH"
  echo "Run Maven tests first so JaCoCo generates reports."
  exit 2
fi

awk -F, '
  function pct(cov, total) {
    if (total == 0) {
      return 0
    }
    return (100 * cov) / total
  }

  function add_to_layer(layer, missed, covered) {
    layer_miss[layer] += missed
    layer_cov[layer] += covered
    all_miss += missed
    all_cov += covered
  }

  NR > 1 {
    pkg = $2
    missed = $4
    covered = $5

    if (index(pkg, ".controller") > 0) {
      add_to_layer("controller", missed, covered)
    } else if (index(pkg, ".service") > 0) {
      add_to_layer("service", missed, covered)
    } else if (index(pkg, ".dao") > 0) {
      add_to_layer("dao", missed, covered)
    } else if (index(pkg, ".valueholder") > 0) {
      add_to_layer("valueholder", missed, covered)
    } else if (index(pkg, ".form") > 0) {
      add_to_layer("form", missed, covered)
    } else {
      add_to_layer("other", missed, covered)
    }
  }

  END {
    all_total = all_miss + all_cov
    if (all_total == 0) {
      print "ERROR: Unable to compute coverage from " ARGV[1]
      exit 3
    }

    all_pct = pct(all_cov, all_total)

    print "Backend layers coverage report (instruction coverage)"
    print "-----------------------------------------"

    n = split("controller service dao valueholder form other", ordered, " ")
    for (i = 1; i <= n; i++) {
      layer = ordered[i]
      total = layer_miss[layer] + layer_cov[layer]
      if (total > 0) {
        layer_pct = pct(layer_cov[layer], total)
        printf("%-12s : %6.2f%% (covered=%d,total=%d)\n", layer, layer_pct, layer_cov[layer], total)
      }
    }

    printf("%-12s : %6.2f%% (covered=%d,total=%d)\n", "all", all_pct, all_cov, all_total)
  }
' "$CSV_PATH"
