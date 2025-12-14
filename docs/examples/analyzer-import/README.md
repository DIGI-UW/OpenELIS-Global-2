# Analyzer Import Example Files

This directory contains example CSV files that can be used to test the analyzer
result import functionality in the Immunology Notebook Workflow (Page 6 -
Analysis).

## Files

### 1. elisa_results_example.csv

ELISA plate reader results with:

- **Well**: Well coordinate (A1-E2 format)
- **Sample_ID**: Sample identifier for matching
- **OD_450**: Optical density reading at 450nm
- **Result**: Qualitative result (Positive/Negative/Equivocal)
- **Interpretation**: Sample type (Control/Calibrator/Patient Sample)
- **Run_Date**: Date of analysis
- **Dilution**: Sample dilution factor

### 2. flow_cytometry_example.csv

Flow cytometry results with:

- **Well_Position**: Well coordinate (A01-D02 format)
- **External_ID**: External sample identifier
- **CD4_Count**: Absolute CD4 count (cells/μL)
- **CD4_Percent**: CD4 percentage
- **CD8_Count**: Absolute CD8 count (cells/μL)
- **CD8_Percent**: CD8 percentage
- **CD4_CD8_Ratio**: CD4/CD8 ratio
- **Total_Lymphocytes**: Total lymphocyte count
- **Acquisition_Date**: Date of acquisition
- **Gate_Events**: Number of gated events

### 3. generic_analyzer_example.csv

Generic analyzer output with:

- **Plate_Well**: Well coordinate
- **Lab_Number**: Laboratory sample number
- **Test_Result**: Numeric result value
- **Units**: Measurement units
- **Reference_Range**: Normal range
- **Flag**: Result flag (Normal/High/Low/Critical)
- **Timestamp**: Date and time of analysis

## How to Use

1. Navigate to the Notebook Workflow → Page 6 (Analysis)
2. Click "Import Analyzer Results"
3. Upload one of these CSV files
4. Map the columns:
   - **Well Coordinate Column**: Select the column containing well positions
     (e.g., "Well", "Well_Position", "Plate_Well")
   - **External ID Column (fallback)**: Select the column containing sample IDs
     (e.g., "Sample_ID", "External_ID", "Lab_Number")
   - **Result Value Column**: Select the column containing the primary result
     (e.g., "OD_450", "CD4_Count", "Test_Result")
5. Review the preview showing matched/unmatched samples
6. Enter assay metadata:
   - **Assay Run ID**: e.g., "ELISA-2024-12-001"
   - **Operator ID**: e.g., "Tech-JSmith"
   - **Machine Parameters**: e.g.,
     `{"wavelength": "450nm", "temperature": "25C"}`
   - **Reagent Lots**: e.g., "LOT-ABC123, LOT-DEF456"
7. Click "Import" to store results

## Column Mapping

The importer supports flexible column mapping:

| Target Field    | Example Columns                              |
| --------------- | -------------------------------------------- |
| Well Coordinate | Well, Well_Position, Plate_Well, WellCoord   |
| External ID     | Sample_ID, External_ID, Lab_Number, SampleId |
| Result          | OD_450, Result, CD4_Count, Test_Result       |

## Notes

- Files must have headers in the first row
- Supported formats: CSV (.csv), Excel (.xlsx, .xls)
- Maximum file size: 10MB
- Maximum rows: 10,000
- Well coordinates are matched via the SampleRouting entity
- Unmatched rows are reported but do not cause import failure
