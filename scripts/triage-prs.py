#!/usr/bin/env python3
import subprocess
import json
import sys

def run_command(cmd):
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        print(f"Error executing: {' '.join(cmd)}")
        print(result.stderr)
        sys.exit(1)
    return result.stdout

def triage_all():
    print("Fetching up to 100 open PRs...")
    json_output = run_command(["gh", "pr", "list", "--json", "number,title,labels,statusCheckRollup,author", "-L", "100"])
    
    prs = json.loads(json_output)
    print(f"\n--- Triage Telemetry: {len(prs)} PRs fetched ---")
    
    unlabeled = [pr for pr in prs if not pr.get("labels")]
    print(f"Unlabeled PRs: {len(unlabeled)}")
    
    with open("triage-telemetry.json", "w") as f:
        json.dump({"total": len(prs), "unlabeled": unlabeled, "all_prs": prs}, f, indent=2)
    
    print("Telemetry saved to `triage-telemetry.json`. Proceed to Step 2: Formulate Triage Snapshot.")

def triage_single(pr_num):
    print(f"Fetching data for PR #{pr_num}...")
    json_output = run_command(["gh", "pr", "view", str(pr_num), "--json", "number,title,labels,statusCheckRollup,author,files"])
    
    pr = json.loads(json_output)
    with open(f"triage-telemetry-{pr_num}.json", "w") as f:
        json.dump(pr, f, indent=2)
    print(f"Telemetry saved to `triage-telemetry-{pr_num}.json`. Proceed to Step 3: Deep Triage & Audit.")

if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] != "--all":
        triage_single(sys.argv[1])
    else:
        triage_all()
