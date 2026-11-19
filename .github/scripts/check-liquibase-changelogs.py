#!/usr/bin/env python3
"""
CI guard validating Liquibase changelog rules (Issue #4517):
1. Changelogs in 3.5.x.x/changes/ must follow UTC timestamped naming: YYYYMMDDTHHMM-<ticket>-<slug>.xml.
2. Each changelog in changes/ must declare logicalFilePath equal to its file stem.
3. Each changeset id in changes/ must equal the file stem (or begin with file stem for multi-changeset files).
4. Changeset IDs across the entire liquibase/ tree must be unique.
5. Previously applied changelogs on the base branch are immutable and must not be edited.
6. 3.5.x.x/base.xml must not be edited unless an explicit override is granted.
"""

import argparse
import os
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET


CHANGES_DIR_REL = "src/main/resources/liquibase/3.5.x.x/changes"
BASE_XML_REL = "src/main/resources/liquibase/3.5.x.x/base.xml"
LIQUIBASE_ROOT_REL = "src/main/resources/liquibase"

# Pattern: YYYYMMDDTHHMM-<ticket>-<slug>.xml
# e.g., 20261002T2230-OGC-1416-remove-pre-bridge-analyzer-storage.xml
CHANGELOG_NAME_REGEX = re.compile(
    r"^[0-9]{8}T[0-9]{4}-[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*-[a-z0-9-]+\.xml$"
)

XML_NS = {"lb": "http://www.liquibase.org/xml/ns/dbchangelog"}


def validate_filename(filename: str) -> bool:
    """Return True if filename matches YYYYMMDDTHHMM-<ticket>-<slug>.xml."""
    return bool(CHANGELOG_NAME_REGEX.match(filename))


def validate_changelog_content(path: Path) -> list[str]:
    """Validate logicalFilePath and changeset id(s) for a file in changes/."""
    errors = []
    file_stem = path.stem
    try:
        tree = ET.parse(path)
        root = tree.getroot()
    except Exception as exc:
        return [f"{path}: failed to parse XML: {exc}"]

    # Extract tag without namespace
    root_tag = root.tag.split("}")[-1] if "}" in root.tag else root.tag
    if root_tag != "databaseChangeLog":
        errors.append(f"{path}: root element must be <databaseChangeLog>, found <{root_tag}>")
        return errors

    logical_path = root.attrib.get("logicalFilePath")
    if not logical_path:
        errors.append(f"{path}: <databaseChangeLog> missing required logicalFilePath attribute")
    elif logical_path != file_stem:
        errors.append(
            f"{path}: logicalFilePath '{logical_path}' must match file stem '{file_stem}'"
        )

    # Find changesets (with or without namespace)
    changesets = root.findall("lb:changeSet", XML_NS) or root.findall("changeSet")
    if not changesets:
        errors.append(f"{path}: no <changeSet> elements found")
        return errors

    for idx, cs in enumerate(changesets):
        cs_id = cs.attrib.get("id")
        author = cs.attrib.get("author")
        if not cs_id:
            errors.append(f"{path}: changeset #{idx+1} missing required 'id' attribute")
        elif len(changesets) == 1 and cs_id != file_stem:
            errors.append(
                f"{path}: changeset id '{cs_id}' must match file stem '{file_stem}'"
            )
        elif len(changesets) > 1 and not (cs_id == file_stem or cs_id.startswith(f"{file_stem}-")):
            errors.append(
                f"{path}: changeset id '{cs_id}' must start with file stem '{file_stem}'"
            )

        if not author:
            errors.append(f"{path}: changeset '{cs_id or idx+1}' missing required 'author' attribute")

    return errors


def check_unique_changeset_ids(liquibase_root: Path, changes_dir: Path) -> list[str]:
    """Check that all changeset IDs in changes/ are unique and do not collide with any changeset across the tree."""
    errors = []
    # Collect all existing IDs outside changes/
    existing_ids: dict[str, str] = {}
    changes_ids: dict[str, str] = {}

    for xml_path in sorted(liquibase_root.rglob("*.xml")):
        if xml_path.name in ("base.xml", "base-changelog.xml"):
            continue
        try:
            tree = ET.parse(xml_path)
            root = tree.getroot()
        except Exception:
            continue

        root_tag = root.tag.split("}")[-1] if "}" in root.tag else root.tag
        if root_tag != "databaseChangeLog":
            continue

        changesets = root.findall("lb:changeSet", XML_NS) or root.findall("changeSet")
        rel_path = str(xml_path.relative_to(liquibase_root))
        is_in_changes = changes_dir.exists() and xml_path.is_relative_to(changes_dir)

        for cs in changesets:
            cs_id = cs.attrib.get("id")
            if not cs_id:
                continue

            if is_in_changes:
                if cs_id in changes_ids:
                    errors.append(
                        f"Duplicate changeset id '{cs_id}' in {rel_path} (already defined in {changes_ids[cs_id]})"
                    )
                elif cs_id in existing_ids:
                    errors.append(
                        f"Changeset id '{cs_id}' in {rel_path} collides with existing changeset in {existing_ids[cs_id]}"
                    )
                else:
                    changes_ids[cs_id] = rel_path
            else:
                if cs_id in changes_ids:
                    errors.append(
                        f"Changeset id '{cs_id}' in {changes_ids[cs_id]} collides with existing changeset in {rel_path}"
                    )
                else:
                    existing_ids[cs_id] = rel_path

    return errors


def check_git_diffs(repo_root: Path, base_ref: str, allow_base_xml_override: bool) -> list[str]:
    """Check git diff against base branch for immutable files and base.xml guard."""
    errors = []
    # Test if git repo and base_ref exists
    try:
        subprocess.run(
            ["git", "-C", str(repo_root), "rev-parse", "--verify", base_ref],
            check=True,
            capture_output=True,
        )
    except Exception:
        # Base ref not present (e.g. shallow clone or standalone test)
        return []

    try:
        merge_base = subprocess.check_output(
            ["git", "-C", str(repo_root), "merge-base", "HEAD", base_ref],
            text=True,
        ).strip()
    except Exception:
        merge_base = base_ref

    # 1. Check if base.xml was modified
    if not allow_base_xml_override:
        diff_base_xml = subprocess.check_output(
            ["git", "-C", str(repo_root), "diff", "-U0", merge_base, "HEAD", "--", BASE_XML_REL],
            text=True,
        ).splitlines()

        if diff_base_xml:
            added_lines = [
                line[1:].strip()
                for line in diff_base_xml
                if line.startswith("+") and not line.startswith("+++")
            ]
            # Check if any new <include file=...> was appended
            has_forbidden_include = any(
                line.startswith("<include ") or "file=" in line
                for line in added_lines
            )
            # Check if changes are anything other than the includeAll changes/ bootstrap and comments
            is_bootstrap_transition = all(
                line.startswith("<!--")
                or line.endswith("-->")
                or line == ""
                or '<includeAll path="changes/"' in line
                for line in added_lines
            )

            if has_forbidden_include or not is_bootstrap_transition:
                errors.append(
                    f"{BASE_XML_REL} was modified. New schema changes must be added as "
                    f"timestamped files in {CHANGES_DIR_REL}/ instead of editing base.xml "
                    f"(override with OE_ALLOW_BASE_XML_OVERRIDE=true or --allow-base-xml-override)."
                )

    # 2. Check for modifications to existing changelog files (immutability)
    diff_files = subprocess.check_output(
        ["git", "-C", str(repo_root), "diff", "--name-status", merge_base, "HEAD", "--", LIQUIBASE_ROOT_REL],
        text=True,
    ).splitlines()

    for line in diff_files:
        parts = line.strip().split(maxsplit=1)
        if len(parts) != 2:
            continue
        status, rel_path = parts[0], parts[1]
        # Ignore new files in changes/ or base.xml (handled separately)
        if rel_path == BASE_XML_REL or rel_path.startswith(CHANGES_DIR_REL):
            continue
        # If existing changelog was modified (M) or deleted (D)
        if status.startswith("M") or status.startswith("D"):
            errors.append(
                f"Existing changelog {rel_path} was modified ({status}). "
                f"Applied changesets are immutable. Create a new changeset in {CHANGES_DIR_REL}/ instead."
            )

    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate Liquibase changelogs against repo conventions.")
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[2]), help="Path to repo root")
    parser.add_argument("--base-ref", default=os.getenv("GITHUB_BASE_REF", "origin/develop"), help="Base git ref")
    parser.add_argument(
        "--allow-base-xml-override",
        action="store_true",
        default=os.getenv("OE_ALLOW_BASE_XML_OVERRIDE", "false").lower() in ("true", "1"),
        help="Allow modifications to base.xml",
    )
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    changes_dir = repo_root / CHANGES_DIR_REL
    liquibase_root = repo_root / LIQUIBASE_ROOT_REL

    errors = []

    # 1. Validate files in changes/
    if changes_dir.exists():
        for path in sorted(changes_dir.iterdir()):
            if path.name.startswith("."):
                continue  # skip .gitkeep, .DS_Store, etc.
            if not validate_filename(path.name):
                errors.append(
                    f"{path.name}: does not match required pattern YYYYMMDDTHHMM-<ticket>-<slug>.xml "
                    f"(e.g., 20261002T2230-OGC-1416-remove-pre-bridge-analyzer-storage.xml)"
                )
            errors.extend(validate_changelog_content(path))

    # 2. Check for duplicate changeset IDs
    errors.extend(check_unique_changeset_ids(liquibase_root, changes_dir))

    # 3. Check git diffs (if git is active)
    errors.extend(check_git_diffs(repo_root, args.base_ref, args.allow_base_xml_override))

    if errors:
        print("❌ Liquibase changelog validation failed:", file=sys.stderr)
        for err in errors:
            print(f"  • {err}", file=sys.stderr)
        return 1

    print("✅ All Liquibase changelogs and conventions validated successfully.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
