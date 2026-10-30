"""Unit tests for Liquibase changelog conventions and CI guard (Issue #4517)."""

import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location(
    "check_liquibase_changelogs",
    Path(__file__).with_name("check-liquibase-changelogs.py"),
)
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


class LiquibaseChangelogValidationTest(unittest.TestCase):

    def test_filename_pattern_accepts_valid_utc_timestamp_and_ticket(self):
        valid_names = [
            "20261002T2230-OGC-1416-remove-pre-bridge-analyzer-storage.xml",
            "20261003T1830-4517-liquibase-timestamp-changelogs.xml",
            "20261003T0915-OE-123-add-indexing.xml",
            "20261231T2359-GH-9999-year-end-cleanup.xml",
        ]
        for name in valid_names:
            with self.subTest(name=name):
                self.assertTrue(validator.validate_filename(name), f"Should accept: {name}")

    def test_filename_pattern_rejects_invalid_names(self):
        invalid_names = [
            "119-retroci-dbs-sample-type-config.xml",  # legacy sequential number
            "20261002T2230.xml",  # missing ticket and slug
            "20261002-OGC-1416-missing-time.xml",  # missing time component
            "20261002T2230-OGC-1416-upper-CASE.xml",  # uppercase slug
            "20261002T2230-OGC-1416-remove-storage.sql",  # non-xml
            ".gitkeep",
        ]
        for name in invalid_names:
            with self.subTest(name=name):
                self.assertFalse(validator.validate_filename(name), f"Should reject: {name}")

    def test_valid_changelog_content_passes(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            file_stem = "20261002T2230-OGC-1416-remove-pre-bridge-analyzer-storage"
            xml_path = Path(temp_dir) / f"{file_stem}.xml"
            xml_path.write_text(f"""<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd"
    logicalFilePath="{file_stem}">
    <changeSet id="{file_stem}" author="tester">
        <comment>Sample changeset</comment>
    </changeSet>
</databaseChangeLog>
""")
            errors = validator.validate_changelog_content(xml_path)
            self.assertEqual([], errors)

    def test_mismatched_logical_file_path_fails(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            file_stem = "20261002T2230-OGC-1416-remove-pre-bridge-analyzer-storage"
            xml_path = Path(temp_dir) / f"{file_stem}.xml"
            xml_path.write_text(f"""<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    logicalFilePath="wrong-logical-path">
    <changeSet id="{file_stem}" author="tester"/>
</databaseChangeLog>
""")
            errors = validator.validate_changelog_content(xml_path)
            self.assertTrue(any("logicalFilePath 'wrong-logical-path' must match file stem" in err for err in errors))

    def test_missing_logical_file_path_fails(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            file_stem = "20261002T2230-OGC-1416-remove-pre-bridge-analyzer-storage"
            xml_path = Path(temp_dir) / f"{file_stem}.xml"
            xml_path.write_text(f"""<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog">
    <changeSet id="{file_stem}" author="tester"/>
</databaseChangeLog>
""")
            errors = validator.validate_changelog_content(xml_path)
            self.assertTrue(any("missing required logicalFilePath" in err for err in errors))

    def test_mismatched_changeset_id_fails(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            file_stem = "20261002T2230-OGC-1416-remove-pre-bridge-analyzer-storage"
            xml_path = Path(temp_dir) / f"{file_stem}.xml"
            xml_path.write_text(f"""<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    logicalFilePath="{file_stem}">
    <changeSet id="different-id" author="tester"/>
</databaseChangeLog>
""")
            errors = validator.validate_changelog_content(xml_path)
            self.assertTrue(any("changeset id 'different-id' must match file stem" in err for err in errors))

    def test_unique_changeset_ids_across_directory(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            dir_path = Path(temp_dir)
            changes_path = dir_path / "changes"
            changes_path.mkdir()
            (dir_path / "existing.xml").write_text("""<databaseChangeLog>
    <changeSet id="existing-cs" author="user1"/>
</databaseChangeLog>""")
            (changes_path / "a.xml").write_text("""<databaseChangeLog>
    <changeSet id="existing-cs" author="user2"/>
</databaseChangeLog>""")
            errors = validator.check_unique_changeset_ids(dir_path, changes_path)
            self.assertTrue(any("collides with existing changeset" in err for err in errors))

    def test_git_diff_check_runs_cleanly_on_current_repo(self):
        errors = validator.check_git_diffs(ROOT, "origin/develop", allow_base_xml_override=False)
        self.assertEqual([], errors)


if __name__ == "__main__":
    unittest.main()
