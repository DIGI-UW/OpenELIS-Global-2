# Laboratory Sample Processing Workflow: Reception to Storage

## Overview

This workflow describes the complete process of receiving a laboratory order,
entering sample data, generating labels, aliquoting samples, and assigning
storage locations using the OpenELIS sample storage management system.

---

## Actors

- **Reception Clerk**: Handles initial order entry and label generation
- **Laboratory Technician**: Manages sample processing, aliquoting, and storage
  assignment

## Materials & Equipment

- Pre-printed paper order form (from ordering facility)
- Sample tubes (original and aliquot tubes)
- Barcode label printer
- Barcode scanner
- Storage racks (labeled and unlabeled)
- Freezer storage system (organized by Freezer → Shelf → Rack → Position)

---

## Phase 1: Sample Reception and Order Entry

**CLARIFICATION with SCANNING**

### 1.1 Initial Reception (Reception Clerk)

**Location**: Reception desk **Duration**: ~5-10 minutes per order

**Steps**:

1. **Receive physical materials**

   - Paper order form arrives from ordering facility
   - Physical samples arrive (2 tubes in this case)
   - Verify sample integrity (proper sealing, labeling, temperature control if
     applicable)

2. **Open Order Entry screen**

   - Navigate to OpenELIS Order Entry module
   - Create new order

3. **Enter order details**

   - **Patient information**: Name, ID, demographics (if not already in system)
   - **Ordering facility**: Reference lab, clinic, or hospital
   - **Ordering provider**: Physician or clinician requesting tests
   - **Order date/time**: When order was placed
   - **Priority level**: Routine, urgent, STAT
   - **Clinical notes**: Relevant patient history or special handling
     instructions

4. **Enter sample item information** (for each of 2 samples)

   - **Sample type**: Blood, serum, urine, etc.
   - **Location**: Location where the sample will be put.
   - **Collection date/time**: When sample was collected
   - **Collection method**(check if currently implemented): Venipuncture, finger
     stick, etc.
   - **Volume**: Approximate volume in tube
   - **Unit of Measure**: Quantity + Unit of measure makes up the volume.
   - **Container type**: EDTA tube, serum separator, etc.

5. **Select tests** (for each sample)

   - Navigate test catalog
   - Add ordered tests or panels (e.g., CBC for Sample 1, Chemistry panel for
     Sample 2)
   - Select panels or tests that need to be refered out aka logged and moved
     down the road.

6. **Intentionally skip location assignment**

   - **Rationale**: Samples will be handed directly to lab tech for immediate
     processing
   - Clerk does not assign storage locations at this stage
   - Samples remain "in transit" status

7. **Save order**
   - System generates **Lab Number**: `LAB123456`
   - System auto-generates **Accession Numbers** for each sample:
     - Sample 1: `LAB123456.1`
     - Sample 2: `LAB123456.2`

### 1.2 Label Generation and Application (Reception Clerk)

**Duration**: ~2-3 minutes

**Steps**:

1. **Print labels** (triggered at end of order entry)

   - System prints 4 barcode labels total:
     - **2 paperwork labels**: `LAB123456` (for paper order form)
     - **2 sample labels**: `LAB123456.1` and `LAB123456.2` (for sample tubes)

2. **Apply paperwork labels**

   - Affix both `LAB123456` labels to the paper order form
     - One on front page (primary identification)
     - One on requisition section (for internal tracking)

3. **Apply sample labels**

   - Affix `LAB123456.1` barcode to first sample tube
     - Position label vertically on tube body (not cap)
     - Ensure barcode is readable and not obscured by tube contents
   - Affix `LAB123456.2` barcode to second sample tube
     - Same positioning guidelines

4. **Hand off to laboratory**
   - Place labeled samples and paperwork in specimen transport tray
   - Deliver to laboratory technician workstation
   - Samples status: "Received, pending processing"

---

## Phase 2: Sample Aliquoting and Preparation

### 2.1 Sample Aliquoting (Laboratory Technician)

**Location**: Laboratory bench (Sample Preparation area) **Duration**: ~5-7
minutes

**Rationale**: Sample `LAB123456.2` requires aliquoting because:

- Multiple tests require separate aliquots to prevent freeze-thaw cycles
- Sufficient volume exists to create sub-samples
- Original sample preservation for potential re-testing

**Steps**:

1. **Review order and test requirements**

   - Check paperwork for Sample `LAB123456.2`
   - Determine aliquot requirements based on test panel
   - Decision: Create 2 aliquots with residual volume in original tube

2. **Prepare aliquot tubes**

   - Retrieve 2 new sample tubes (same type as original)
   - Label tubes manually with temporary marker:
     - Aliquot 1: `LAB123456.2-1`
     - Aliquot 2: `LAB123456.2-2`

3. **Perform aliquoting**

   - Using sterile pipette, transfer appropriate volume to each aliquot tube:
     - Aliquot 1 (`LAB123456.2-1`): Transfer 1.0 mL
     - Aliquot 2 (`LAB123456.2-2`): Transfer 1.0 mL
   - Original tube (`LAB123456.2`): Retain remaining volume (~0.5 mL residual)

4. **Generate aliquot labels in OpenELIS**

   - Navigate to Sample Management → Aliquot Creation
   - Scan parent sample barcode: `LAB123456.2`
   - System prompts: "Number of aliquots?"
   - Enter: `2`
   - System auto-generates accession numbers:
     - `LAB123456.2-1`
     - `LAB123456.2-2`
   - Print aliquot labels

5. **Apply barcode labels to aliquots**
   - Remove temporary marker labels
   - Affix printed barcode labels:
     - `LAB123456.2-1` → Aliquot tube 1
     - `LAB123456.2-2` → Aliquot tube 2

**Result**: 4 samples now ready for storage:

- `LAB123456.1` (original, no aliquots)
- `LAB123456.2` (original with residual volume)
- `LAB123456.2-1` (aliquot 1)
- `LAB123456.2-2` (aliquot 2)

---

## Phase 3: Storage Location Assignment

### 3.1 Assign Storage for Sample LAB123456.1 (Pre-existing Rack)

**Location**: Laboratory bench → Freezer A **Duration**: ~2-3 minutes

**Scenario**: Assigning sample to an already-labeled, system-registered rack

**Steps**:

1. **Open Storage Assignment screen**

   - Navigate to: **Sample Menu → Storage Assignment**
   - Screen displays: Barcode input fields for Sample and Location

2. **Scan sample barcode**

   - Scan `LAB123456.1`
   - System displays:
     - Sample ID: `LAB123456.1`
     - Sample type: [Type]
     - Current location: "Unassigned"

3. **Retrieve target storage rack**

   - Walk to **Freezer A → Shelf 3 → Rack R-A3-05** (example location)
   - Rack has pre-printed barcode label: `RACK-A3-05`
   - **Observation**: Label already present (rack is registered in system)

4. **Scan rack barcode**

   - Scan `RACK-A3-05`
   - System auto-fills location fields:
     - **Room**: Laboratory Cold Storage Room
     - **Device**: Freezer A
     - **Shelf**: Shelf 3
     - **Rack**: R-A3-05
     - **Position**: [Dropdown shows available positions: A1-H12]

5. **Select position**

   - Review rack visually to find next available position
   - Select from dropdown: `Position B4`

6. **Save assignment**

   - Click "Save" button
   - System confirms: "Sample `LAB123456.1` assigned to Freezer A / Shelf 3 /
     Rack R-A3-05 / Position B4"
   - System updates sample status: "In Storage"

7. **Physical placement**
   - Place sample tube `LAB123456.1` into Rack `R-A3-05`, Position `B4`
   - Return rack to Freezer A, Shelf 3

---

### 3.2 Assign Storage for LAB123456.2 Family (New Rack Creation)

**Location**: Laboratory bench → Freezer B **Duration**: ~8-10 minutes

**Scenario**: Assigning 3 related samples to a new, unlabeled rack that doesn't
exist in the system

#### 3.2.1 Identify Storage Need

**Steps**:

1. **Scan first sample of group**

   - Scan `LAB123456.2` (parent sample with residual volume)
   - System displays sample details, awaits location scan

2. **Retrieve unlabeled rack from storage area**
   - Walk to **Freezer B → Shelf 2**
   - Retrieve empty rack from shelf
   - **Problem identified**: Rack has no barcode label
   - **Consequence**: Cannot scan to auto-fill location

#### 3.2.2 Create New Rack in System

**Steps**:

1. **Initiate rack creation workflow**

   - In Storage Assignment screen, click: **"Add New Storage Location"**
   - System navigates to: **Storage Management → Create New Location**

2. **Define rack hierarchy**

   - **Select Location Type**: "Rack" (from dropdown)
   - **Parent Location** (navigate hierarchy):
     - **Room**: Laboratory Cold Storage Room
     - **Device**: Freezer B (select from device list)
     - **Shelf**: Shelf 2 (select from shelf list)
   - System displays: "Create rack under Freezer B / Shelf 2"

3. **Enter rack details**

   - **Rack Name**: `R-B2-12` (following naming convention:
     [Device]-[Shelf]-[Sequential])
   - **Rack Dimensions**: 8 rows × 12 columns (standard 96-position rack)
   - **Temperature Range**: -20°C (inherited from Freezer B settings)
   - **Notes**: [Optional] "Newly commissioned 2025-01-15"

4. **Save new rack**

   - Click "Save"
   - System generates:
     - **Rack ID**: `R-B2-12`
     - **System UUID**: Auto-generated FHIR-compliant UUID
     - **Status**: "Active"

5. **Realize label needed for scanning workflow**
   - Recognize: Need physical barcode label for efficient future assignments
   - Decision: Generate and print label now

#### 3.2.3 Generate and Apply Rack Label

**Steps**:

1. **Navigate to Label Management**

   - From rack creation confirmation screen, click: **"Manage Labels"**
   - Or navigate: **Storage Management → Rack R-B2-12 → Label Management**

2. **Set short code** (for human-readable identification)

   - System shows default: `R-B2-12`
   - Optionally customize short code (e.g., `FB2-12` for "Freezer B, Shelf 2,
     Rack 12")
   - Keep default: `R-B2-12`

3. **Print rack label**

   - Click "Print Label"
   - System generates barcode label containing:
     - **Barcode**: Encodes rack ID `R-B2-12`
     - **Human-readable text**: "Rack R-B2-12 | Freezer B | Shelf 2"
     - **QR code**: (Optional) Contains full location hierarchy

4. **Affix label to physical rack**
   - Apply printed label to upper-right corner of rack frame
   - Ensure barcode is clearly visible and scannable
   - Position avoids interference with sample tubes

#### 3.2.4 Assign First Sample to New Rack (LAB123456.2)

**Steps**:

1. **Return to Storage Assignment screen**

   - Navigate: **Sample Menu → Storage Assignment**
   - (May already be open from earlier workflow)

2. **Scan sample**

   - Scan `LAB123456.2`
   - System displays sample details

3. **Scan newly-labeled rack**

   - Scan `R-B2-12` (now has barcode)
   - System auto-fills:
     - **Room**: Laboratory Cold Storage Room
     - **Device**: Freezer B
     - **Shelf**: Shelf 2
     - **Rack**: R-B2-12
     - **Position**: [Dropdown shows all 96 positions: A1-H12]

4. **Select position**

   - Choose: `Position A1` (first position in new rack)

5. **Save assignment**

   - Click "Save"
   - System confirms: "Sample `LAB123456.2` assigned to Freezer B / Shelf 2 /
     Rack R-B2-12 / Position A1"

6. **Place sample physically**
   - Insert tube `LAB123456.2` into Rack `R-B2-12`, Position `A1`
   - Do not return rack to freezer yet (more samples to assign)

#### 3.2.5 Assign Second Sample (LAB123456.2-1) - Using Dropdown

**Steps**:

1. **Scan sample**

   - Scan `LAB123456.2-1` (first aliquot)
   - System displays sample details

2. **Realize rack label not scanned**

   - **Problem**: Technician forgets to scan rack barcode
   - **Alternative approach**: Use dropdown menus to navigate hierarchy

3. **Manually select location via dropdowns**

   - **Room**: Select "Laboratory Cold Storage Room"
   - **Device**: Select "Freezer B"
   - **Shelf**: Select "Shelf 2"
   - **Rack**: Select "R-B2-12" (appears in dropdown after previous assignment)
   - **Position**: Select "A2" (next position adjacent to parent sample)

4. **Save assignment**

   - Click "Save"
   - System confirms: "Sample `LAB123456.2-1` assigned to Freezer B / Shelf 2 /
     Rack R-B2-12 / Position A2"

5. **Place sample physically**
   - Insert tube `LAB123456.2-1` into Rack `R-B2-12`, Position `A2`
   - Rack still on bench (one more sample to assign)

#### 3.2.6 Assign Third Sample (LAB123456.2-2) - Optimized Scanning

**Steps**:

1. **Scan sample**

   - Scan `LAB123456.2-2` (second aliquot)
   - System displays sample details

2. **Scan rack barcode** (efficient method)

   - Scan `R-B2-12` barcode on rack
   - System auto-fills location hierarchy
   - Position dropdown displays available positions

3. **Select position**

   - Choose: `Position A3` (next to previous aliquot)

4. **Save assignment**

   - Click "Save"
   - System confirms: "Sample `LAB123456.2-2` assigned to Freezer B / Shelf 2 /
     Rack R-B2-12 / Position A3"

5. **Place sample physically**

   - Insert tube `LAB123456.2-2` into Rack `R-B2-12`, Position `A3`

6. **Final rack placement**
   - **Visual verification**: Confirm all 3 samples visible in rack:
     - Position A1: `LAB123456.2`
     - Position A2: `LAB123456.2-1`
     - Position A3: `LAB123456.2-2`
   - Return Rack `R-B2-12` to **Freezer B, Shelf 2**
   - Close freezer door

---

## Phase 4: Workflow Completion and Verification

### 4.1 Final Status Check

**Steps**:

1. **Review sample status in system**

   - Navigate: **Sample Menu → Sample Search**
   - Search: `LAB123456`
   - Verify all samples show "In Storage" status:
     - `LAB123456.1` → Freezer A / Shelf 3 / Rack R-A3-05 / Position B4
     - `LAB123456.2` → Freezer B / Shelf 2 / Rack R-B2-12 / Position A1
     - `LAB123456.2-1` → Freezer B / Shelf 2 / Rack R-B2-12 / Position A2
     - `LAB123456.2-2` → Freezer B / Shelf 2 / Rack R-B2-12 / Position A3

2. **Update order status**

   - Order `LAB123456` status: "Samples in storage, ready for testing"

3. **Close workflow**
   - Log out of Storage Assignment module
   - Return to normal laboratory operations

---

## Key Workflow Features Demonstrated

### 1. **Flexible Label Timing**

- Reception clerk prints labels immediately after order entry
- Aliquot labels generated later during sample processing
- Rack labels generated on-demand when new locations created

### 2. **Barcode Scanning Efficiency**

- **Optimal**: Scan sample → Scan location → Auto-fill hierarchy
- **Fallback**: Manual dropdown navigation when barcode unavailable
- **Hybrid**: Mix scanning and dropdown selection as needed

### 3. **On-the-Fly Location Creation**

- Technician can create new racks mid-workflow without administrative access
- Hierarchical navigation ensures proper parent-child relationships
- System enforces data integrity (can't create rack without parent shelf)

### 4. **Label Management Integration**

- Technician generates rack label immediately after creation
- Prevents future workflow delays from unlabeled locations
- Supports both human-readable text and machine-scannable barcodes

### 5. **Aliquot Family Grouping**

- Parent sample and aliquots stored together (Rack R-B2-12, Positions A1-A3)
- Logical organization aids future sample retrieval
- System maintains parent-child relationships in database

---

## Workflow Metrics

| Metric                                 | Value                                                                        |
| -------------------------------------- | ---------------------------------------------------------------------------- |
| **Total workflow duration**            | ~25-30 minutes                                                               |
| **Reception phase**                    | ~7-13 minutes                                                                |
| **Aliquoting phase**                   | ~5-7 minutes                                                                 |
| **Storage assignment (existing rack)** | ~2-3 minutes                                                                 |
| **Storage assignment (new rack)**      | ~8-10 minutes                                                                |
| **Samples processed**                  | 4 (1 original + 1 original with aliquots + 2 aliquots)                       |
| **Locations accessed**                 | 2 freezers, 2 shelves, 2 racks                                               |
| **Labels printed**                     | 8 total (2 order + 2 original samples + 2 aliquots + 1 rack + 1 manual temp) |
| **Barcode scans**                      | 7 total (4 samples + 2 racks + 1 for label verification)                     |

---

## Error Handling and Decision Points

### Common Issues and Resolutions

| Issue                              | Resolution                                                      |
| ---------------------------------- | --------------------------------------------------------------- |
| **Rack has no barcode label**      | Use dropdown navigation OR create/print label immediately       |
| **Forgot to print aliquot labels** | Generate labels retroactively via Sample Management             |
| **Selected wrong position**        | System allows reassignment if sample not yet tested             |
| **Freezer full**                   | System shows rack capacity warnings, prompts alternate location |
| **Barcode won't scan**             | Manual entry via dropdown or re-print label                     |
| **Sample already assigned**        | System warns "Sample already has location, overwrite?"          |

---

## Best Practices Illustrated

1. ✅ **Clerk skips location assignment** when samples handed directly to tech
   (avoids double-entry)
2. ✅ **Aliquot family stored together** (same rack, adjacent positions) for
   easy retrieval
3. ✅ **Generate rack labels immediately** after creating new locations
   (prevents future delays)
4. ✅ **Use barcode scanning** whenever possible for speed and accuracy
5. ✅ **Visual verification** before returning racks to freezers (catch
   misplacements early)
6. ✅ **Short codes for human readability** (e.g., `R-B2-12` = "Rack B2,
   position 12")

---

## System Interactions Summary

```
┌─────────────────────────────────────────────────────────────┐
│                     OpenELIS Modules Used                    │
├─────────────────────────────────────────────────────────────┤
│ 1. Order Entry          → Create LAB123456                  │
│ 2. Label Generation     → Print barcodes for samples/racks  │
│ 3. Sample Management    → Create aliquots                   │
│ 4. Storage Assignment   → Assign samples to locations       │
│ 5. Storage Management   → Create new rack R-B2-12          │
│ 6. Label Management     → Print rack barcode label         │
│ 7. Sample Search        → Verify final storage locations    │
└─────────────────────────────────────────────────────────────┘
```

---

## Original Workflow Description

This detailed workflow was derived from the following user story:

> Sample comes in the door, arrives at reception, the clerk enters order
> details, enters sample and test info for the 2 samples, doesn't log the
> location, as they are going to pass it off to a lab tech, saves the order with
> lab number LAB123456, prints the labels at the end of order entry, 2 go on the
> paperwork, one is generated for each sample, the paperwork labels are applied
> to the paper order that came in, each sample gets a barcode label, the
> barcodes on the samples read LAB123456.1, and LAB123456.2.
>
> Sample LAB123456.2 is aloquated into 2 sub samples, LAB123456.2-1 and
> LAB123456.2-2, labels are created for each and affixed to new tubes with the
> split sample (in this case there is some left also in the original). The lab
> tech opens up the "Sample" menu with your work, scans the barcode of the first
> sample item LAB123456.1, and then retrieves a rack from a shelf in a specific
> freezer, oh good, it's already got a label on it, so they scan the label from
> the location, and it fills the location info. Tech saves.
>
> LAB123456.2, LAB123456.2-1 and LAB123456.2 need to go in a different freezer,
> this one is a new rack, and doesn't exist in the system, the tech follows the
> on screen path to add a new rack in that freezer and shelf, and when they
> save, they assign the sample item LAB123456.2 to that rack and position, and
> then close the window. Next they scan LAB123456.2-1 and go to assign the
> sample item to the rack, realize they didn't print the label, so they enter it
> in with the drop down menu. To make their work easier, they go into the rack
> -> label management, set the short code, and print the label, they affix it to
> the rack, then scan LAB123456.2-2, then scan the rack to easily move the
> sample, and return the rack to the proper freezer shelf.

---

This enriched workflow provides comprehensive detail suitable for:

- **Training materials** for new laboratory staff
- **Standard Operating Procedures (SOPs)**
- **System design requirements** for OpenELIS storage module
- **User acceptance testing scenarios**
- **Process optimization analysis**
