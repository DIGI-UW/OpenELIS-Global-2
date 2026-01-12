/**
 * Medical Laboratory Workflow Module.
 *
 * <p>
 * This module implements the medical laboratory workflow for OpenELIS,
 * supporting the complete sample lifecycle from patient registration through
 * disposal/archiving.
 *
 * <p>
 * Key features:
 *
 * <ul>
 * <li>Quality control at sample reception (sample-type-specific criteria)
 * <li>IATA PI650 transport packaging compliance
 * <li>Levey-Jennings QC charting and Westgard rule validation
 * <li>Equipment usage tracking for accreditation support
 * </ul>
 *
 * <p>
 * This module reuses existing Patient, Sample, and LabOrder services where
 * possible, adding only genuinely new entities for medlab-specific
 * functionality.
 *
 * @see org.openelisglobal.sample.service.SampleService
 * @see org.openelisglobal.patient.service.PatientService
 */
package org.openelisglobal.medlab;
