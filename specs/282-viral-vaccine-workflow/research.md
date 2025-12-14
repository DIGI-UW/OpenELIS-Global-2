# Research: Viral and Vaccine Laboratory Workflow

**Feature**: `282-viral-vaccine-workflow`  
**Date**: 2025-12-14  
**Status**: Complete

## Purpose

Research technical decisions, patterns, and best practices for implementing viral and vaccine laboratory workflows in OpenELIS Global 2.

## Research Areas

### 1. Cold Chain Monitoring Implementation

**Question**: How to implement continuous temperature monitoring with real-time alerts for -80°C freezers and liquid nitrogen dewars?

**Decision**: Use database-driven temperature logging with scheduled background jobs for alert generation.

**Rationale**:
- **Audit Trail**: All temperature readings stored in database for regulatory compliance
- **Alert Logic**: Background job (Spring @Scheduled) queries recent readings, detects excursions (temp out of range for >duration threshold), creates TemperatureExcursion records
- **Notification**: Alert service sends emails/SMS to cold chain coordinators when excursions detected
- **Performance**: Hourly background job (configurable) processes readings, avoids real-time overhead
- **Data Retention**: Temperature logs retained per regulatory requirements (typically 5+ years)

**Alternatives Considered**:
- **Real-time event processing** (e.g., Kafka, RabbitMQ): Rejected due to added complexity and infrastructure requirements
- **External monitoring service**: Rejected to keep all data in OpenELIS database for unified audit trail

**Implementation Approach**:
- `TemperatureLog` table: Stores continuous readings (storage_unit_id, timestamp, temperature, within_range)
- `TemperatureExcursion` table: Stores alarm events (storage_unit_id, start_time, end_time, max_temp, affected_samples, corrective_action)
- `TemperatureMonitoringService`: Background job (@Scheduled) that:
  1. Queries recent TemperatureLog entries (last hour)
  2. Detects out-of-range readings
  3. Groups consecutive excursions
  4. Creates TemperatureExcursion records
  5. Triggers AlertService for notifications
- Index on `TemperatureLog(storage_unit_id, timestamp)` for fast querying

### 2. PCR Multi-Target Result Storage

**Question**: How to store PCR results with multiple target genes (e.g., COVID-19: N, ORF1ab, S genes) in a normalized schema?

**Decision**: Use `PCRResult` table with one row per sample-target combination.

**Rationale**:
- **Flexibility**: Different assays have different numbers of targets (Influenza A/B = 2, COVID-19 = 3, HIV = 1)
- **Normalized Design**: Avoids NULL columns or dynamic columns
- **Query Efficiency**: Easy to query all targets for a sample or all samples for a specific target
- **Result Interpretation**: Each target can have independent Ct value and interpretation (Detected/Not Detected)

**Alternatives Considered**:
- **JSON column** for targets: Rejected due to difficulty querying and indexing
- **Fixed columns** (Ct1, Ct2, Ct3): Rejected due to lack of flexibility and semantic meaning

**Schema Design**:
```sql
PCRRun: run_id, run_date, instrument_id, assay_type, kit_lot, technician_id
PCRResult: result_id, run_id, sample_id, target_gene, ct_value, interpretation (Detected/Not Detected/Indeterminate)
```

**Example Data**:
- COVID-19 sample: 3 rows (N gene Ct=18.5/Detected, ORF1ab Ct=19.2/Detected, S gene Ct=20.1/Detected)
- Influenza sample: 2 rows (Influenza A Ct=22.3/Detected, Influenza B Ct>40/Not Detected)

### 3. ELISA Standard Curve Calculation

**Question**: How to implement 4-parameter logistic regression for ELISA standard curve fitting?

**Decision**: Use Apache Commons Math library for curve fitting.

**Rationale**:
- **Mature Library**: Apache Commons Math 3.6.1 provides robust non-linear regression
- **4PL Formula**: Supports 4-parameter logistic: `y = d + (a - d) / (1 + (x/c)^b)`
  - `a` = minimum asymptote (background)
  - `b` = Hill's slope
  - `c` = inflection point (EC50)
  - `d` = maximum asymptote (plateau)
- **R² Calculation**: Library provides coefficient of determination for fit quality
- **Performance**: Calculation completes in <2s for typical 8-point curve

**Alternatives Considered**:
- **Manual implementation**: Rejected due to complexity of non-linear optimization
- **External Python service**: Rejected to keep all logic in Java backend
- **Linear interpolation**: Rejected as inaccurate for antibody quantification

**Implementation Approach**:
```java
@Service
public class ELISAService {
    public Map<String, Object> calculateStandardCurve(List<Calibrator> calibrators) {
        // Extract OD and concentration data
        double[] od = calibrators.stream().mapToDouble(Calibrator::getOD).toArray();
        double[] conc = calibrators.stream().mapToDouble(Calibrator::getConcentration).toArray();
        
        // Use Apache Commons Math for 4PL fitting
        ParametricUnivariateFunction fourPL = new FourParameterLogistic();
        SimpleCurveFitter fitter = SimpleCurveFitter.create(fourPL, initialGuess);
        WeightedObservedPoints obs = new WeightedObservedPoints();
        for (int i = 0; i < od.length; i++) {
            obs.add(od[i], conc[i]);
        }
        double[] params = fitter.fit(obs.toList());
        
        // Calculate R²
        double rSquared = calculateRSquared(od, conc, params);
        
        return Map.of("parameters", params, "rSquared", rSquared);
    }
}
```

### 4. Cryogenic Storage Position Hierarchy

**Question**: How to model hierarchical storage (Dewar → Canister → Rack → Box → Position) for liquid nitrogen and -80°C freezers?

**Decision**: Use single `StoragePosition` table with self-referencing parent_id FK + storage_unit_id FK.

**Rationale**:
- **Flexible Hierarchy**: Supports variable depth (LN₂ dewars: 5 levels, -80°C freezers: 4 levels)
- **Path Queries**: Can query full path from root to leaf using recursive CTE or application-level recursion
- **Occupancy Tracking**: `sample_id` FK at leaf positions only (boxes contain positions, not samples)
- **Grid Display**: Frontend queries all positions for a box, renders as 9×9 or 10×10 grid

**Alternatives Considered**:
- **Separate tables per level** (Dewar, Canister, Rack, Box, Position): Rejected due to schema complexity
- **Nested set model**: Rejected due to update complexity when positions added/removed
- **Closure table**: Considered but overkill for this use case

**Schema Design**:
```sql
StorageUnit: unit_id, unit_type (DEWAR/FREEZER), name, capacity, location, temperature, alarm_system
StoragePosition: position_id, storage_unit_id, parent_id, level (CANISTER/SHELF/RACK/BOX/POSITION), 
                 name (C2/S3/R2/BOX-12/F7), occupancy_status (EMPTY/OCCUPIED/RESERVED), sample_id
```

**Example Path**:
- LN₂ dewar: LN2-01 (unit) → C2 (canister) → R3 (rack) → BOX-A (box) → D5 (position)
- -80°C freezer: ULT-01 (unit) → S3 (shelf) → R2 (rack) → BOX-12 (box) → F7 (position)

**Query Pattern**:
```java
// Get full path from position to root
public List<StoragePosition> getPositionPath(String positionId) {
    List<StoragePosition> path = new ArrayList<>();
    StoragePosition current = positionDAO.get(positionId);
    while (current != null) {
        path.add(0, current);  // Prepend to reverse order
        current = current.getParentId() != null ? positionDAO.get(current.getParentId()) : null;
    }
    return path;
}
```

### 5. Biosafety Level Enforcement

**Question**: How to enforce biosafety checklist completion before allowing processing of BSL-3 samples?

**Decision**: Use `BiosafetyClearance` entity with required checklist items + controller-level validation.

**Rationale**:
- **Safety First**: System blocks processing if biosafety checklist incomplete
- **Audit Trail**: All checklist completions logged with user ID + timestamp
- **Configurable Levels**: Biosafety levels and required PPE configured per lab (not hardcoded)
- **Incremental Saving**: Users can save incomplete checklists as draft, but cannot proceed to processing

**Implementation Approach**:
```java
@Service
public class BiosafetyClearanceService {
    public void validateBiosafetyClearance(String sampleId) throws BiosafetyClearanceException {
        ViralSample sample = viralSampleDAO.get(sampleId);
        BiosafetyClearance clearance = biosafetyClearanceDAO.findBySampleId(sampleId);
        
        if (clearance == null || !clearance.isChecklistComplete()) {
            throw new BiosafetyClearanceException(
                "Biosafety checklist incomplete for BSL-" + sample.getBiosafety
Level() + " sample"
            );
        }
        
        // Log access for audit trail
        auditService.logBiosafetyClearanceAccess(sampleId, getCurrentUser());
    }
}
```

**Controller Usage**:
```java
@PostMapping("/sample/{id}/process")
public ResponseEntity<?> processSample(@PathVariable String id) {
    biosafetyClearanceService.validateBiosafetyClearance(id);  // Throws exception if incomplete
    // Proceed with processing...
}
```

### 6. Viral Culture Passage Tracking

**Question**: How to track viral culture passages (P0 → P1 → P2...) with lineage integrity?

**Decision**: Use `ViralCulture` table with self-referencing parent_culture_id FK.

**Rationale**:
- **Lineage Preservation**: Each passage links to parent, forming a chain
- **Passage Number**: Stored explicitly (passage_number: 0, 1, 2, ...) for quick filtering
- **CPE Observations**: Each passage has its own observation log (separate rows or JSON field)
- **Viral Titer**: Recorded per passage for quality tracking

**Schema Design**:
```sql
ViralCulture: culture_id, specimen_id, parent_culture_id, passage_number, 
              cell_line, culture_medium, inoculation_date, harvest_date,
              cpe_status (NONE/MILD/MODERATE/SEVERE), viral_titer, titer_method
```

**Example Lineage**:
```
P0 (VCU-2025-001): specimen_id=SWAB-001, parent_culture_id=NULL, passage_number=0
P1 (VCU-2025-001-P1): specimen_id=NULL, parent_culture_id=VCU-2025-001, passage_number=1
P2 (VCU-2025-001-P2): specimen_id=NULL, parent_culture_id=VCU-2025-001-P1, passage_number=2
```

**Query Pattern**:
```java
// Get full passage history
public List<ViralCulture> getPassageHistory(String cultureId) {
    List<ViralCulture> history = new ArrayList<>();
    ViralCulture current = viralCultureDAO.get(cultureId);
    
    // Traverse up to P0
    while (current != null) {
        history.add(0, current);
        current = current.getParentCultureId() != null ? 
                  viralCultureDAO.get(current.getParentCultureId()) : null;
    }
    
    return history;
}
```

### 7. Vaccine Lot Release Decision Logic

**Question**: How to implement vaccine lot release decision based on multiple test results (sterility, potency, endotoxin)?

**Decision**: Use `VaccineLot` entity with separate test result fields + derived release_status field.

**Rationale**:
- **All Tests Required**: Lot cannot be released unless all required tests pass
- **Audit Trail**: Each test result recorded with date, operator, and result value
- **Quality Manager Approval**: Final release requires QM digital signature even if all tests pass
- **Quarantine Flag**: Failed lots automatically quarantined and cannot be released

**Schema Design**:
```sql
VaccineLot: lot_id, lot_number, manufacturer, vaccine_type, expiry_date,
            sterility_result (PASS/FAIL), sterility_date, sterility_operator,
            potency_result (PASS/FAIL), potency_value, potency_date, potency_operator,
            endotoxin_result (PASS/FAIL), endotoxin_value, endotoxin_date, endotoxin_operator,
            release_status (PENDING/APPROVED/REJECTED), release_date, release_approved_by
```

**Release Logic**:
```java
@Service
public class VaccineLotService {
    public boolean canRelease(String lotId) {
        VaccineLot lot = vaccineLotDAO.get(lotId);
        
        return lot.getSterilityResult() == TestResult.PASS &&
               lot.getPotencyResult() == TestResult.PASS &&
               lot.getEndotoxinResult() == TestResult.PASS &&
               lot.getReleaseApprovedBy() != null;  // QM approval required
    }
    
    @Transactional
    public void approveLotRelease(String lotId, String qualityManagerId) throws ValidationException {
        VaccineLot lot = vaccineLotDAO.get(lotId);
        
        if (!canRelease(lotId)) {
            throw new ValidationException("Cannot release lot - test results incomplete or failed");
        }
        
        lot.setReleaseStatus(ReleaseStatus.APPROVED);
        lot.setReleaseDate(new Date());
        lot.setReleaseApprovedBy(qualityManagerId);
        vaccineLotDAO.update(lot);
        
        auditService.logLotRelease(lotId, qualityManagerId);
    }
}
```

### 8. Integration with Existing OpenELIS Services

**Question**: How to integrate viral workflow with existing Patient, Sample, and Storage services?

**Decision**: Extend existing services, reuse interfaces, add viral-specific attributes.

**Rationale**:
- **Code Reuse**: Leverage existing PatientService for patient registration, SampleService for sample creation, SampleStorageService for storage hierarchies
- **Minimal Duplication**: Avoid recreating common functionality (patient demographics, sample tracking, storage assignment)
- **Consistent APIs**: Viral samples appear in existing sample lists, storage reports include viral samples

**Integration Points**:
- **PatientService**: Reuse for patient/participant registration (no changes needed)
- **SampleService**: Extend with viral-specific attributes (biosafety level, cold chain status) via `ViralSample` entity that references `Sample`
- **SampleStorageService**: Reuse for cryogenic storage hierarchies (dewars/freezers modeled as `StorageLocation` extensions)
- **NotebookService (OGC-51)**: Extend notebook pages to support viral workflow pages (registration, QC, testing)

**Relationship Pattern**:
```java
@Entity
@Table(name = "viral_sample")
public class ViralSample extends BaseObject<String> {
    @ManyToOne
    @JoinColumn(name = "sample_id", referencedColumnName = "id")
    private Sample sample;  // Reference to core Sample entity
    
    @Column(name = "biosafety_level")
    private String biosafet yLevel;  // BSL-1/BSL-2/BSL-3/BSL-4
    
    @Column(name = "cold_chain_status")
    private String coldChainStatus;  // COMPLIANT/EXCURSION/BREAK
    
    // Viral-specific attributes...
}
```

## Summary

All research questions resolved. Key technical decisions documented with rationales. Ready to proceed to Phase 1 (data model + contracts).

**Next Steps**:
1. Generate `data-model.md` with entity relationships
2. Create API contracts in `contracts/` directory
3. Generate `quickstart.md` for developer onboarding
4. Run agent context update script
