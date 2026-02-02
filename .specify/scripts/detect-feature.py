#!/usr/bin/env python3
"""
Detect the current feature directory from git branch name.
Handles various branch naming patterns flexibly.
"""
import os
import sys
import subprocess
import re

def get_current_branch():
    """Get the current git branch name."""
    try:
        result = subprocess.run(
            ['git', 'rev-parse', '--abbrev-ref', 'HEAD'],
            capture_output=True,
            text=True,
            check=True
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError:
        return None

def extract_identifiers(branch_name):
    """
    Extract potential feature identifiers from branch name.
    Returns list of identifier patterns to search for.

    Examples:
        feat/OGC-070-catalyst-assistant -> ['OGC-070', 'catalyst-assistant']
        feat/011-some-feature -> ['011', 'some-feature']
        feat/issue-332-something -> ['332', 'something']
    """
    # Remove common prefixes
    name = re.sub(r'^(feat|feature|fix|bugfix|hotfix)/', '', branch_name)

    # Split on various separators but keep meaningful chunks
    parts = re.split(r'[-_/]', name)

    identifiers = []

    # Look for ticket patterns (e.g., OGC-070)
    ticket_match = re.search(r'[A-Z]+-\d+', name)
    if ticket_match:
        identifiers.append(ticket_match.group(0))

    # Look for standalone numbers (e.g., 011, 332)
    for part in parts:
        if part.isdigit():
            identifiers.append(part)

    # Add the full name (without prefix) as fallback
    identifiers.append(name)

    return identifiers

def find_matching_feature(identifiers, specs_dir='specs'):
    """
    Find the best matching feature directory in specs/.
    Returns absolute path to feature directory or None.
    """
    if not os.path.isdir(specs_dir):
        return None

    # Get all feature directories
    features = []
    for item in os.listdir(specs_dir):
        item_path = os.path.join(specs_dir, item)
        if os.path.isdir(item_path):
            features.append(item)

    if not features:
        return None

    # Score each feature directory by how many identifiers match
    best_match = None
    best_score = 0

    for feature in features:
        score = 0
        for identifier in identifiers:
            if identifier in feature:
                score += 1

        if score > best_score:
            best_score = score
            best_match = feature

    # If we found a match, return it
    if best_match:
        return os.path.abspath(os.path.join(specs_dir, best_match))

    # Fallback: most recently modified directory
    most_recent = max(
        features,
        key=lambda f: os.path.getmtime(os.path.join(specs_dir, f))
    )
    return os.path.abspath(os.path.join(specs_dir, most_recent))

def list_available_docs(feature_dir):
    """List all markdown docs in the feature directory."""
    docs = []
    for root, dirs, files in os.walk(feature_dir):
        for file in files:
            if file.endswith('.md'):
                docs.append(os.path.abspath(os.path.join(root, file)))
    return sorted(docs)

def main():
    # Get current branch
    branch = get_current_branch()
    if not branch:
        print('ERROR: Not in a git repository', file=sys.stderr)
        sys.exit(1)

    # Extract identifiers from branch name
    identifiers = extract_identifiers(branch)

    # Find matching feature directory
    feature_dir = find_matching_feature(identifiers)
    if not feature_dir:
        print('ERROR: No feature directories found in specs/', file=sys.stderr)
        sys.exit(1)

    # Output results
    print(f'FEATURE_DIR={feature_dir}')

    docs = list_available_docs(feature_dir)
    if docs:
        print('AVAILABLE_DOCS=' + ','.join(docs))
    else:
        print('AVAILABLE_DOCS=')

if __name__ == '__main__':
    main()
