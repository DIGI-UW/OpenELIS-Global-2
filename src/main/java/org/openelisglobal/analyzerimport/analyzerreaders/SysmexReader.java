/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzerimport.analyzerreaders;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.analyzerimport.util.AnalyzerTestNameCache;
import org.openelisglobal.analyzerimport.util.MappedTestName;
import org.openelisglobal.analyzerresults.valueholder.AnalyzerResults;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.DateUtil;

@SuppressWarnings("unused")
public class SysmexReader extends AnalyzerLineInserter {

    private static final String CONTROL_ACCESSION_PREFIX = "QC-";

    private static int index = 0;
    private static final int ID_INSTRUMENT = index++;
    private static final int DATE = index++;
    private static final int TIME = index++;
    private static final int RACK = index++;
    private static final int TUBE = index++;
    private static final int ACCESSION = index++;
    private static final int ACCESSION_INFO = index++;
    private static final int METHOD = index++;
    private static final int ID_PATIENT = index++;
    private static final int ANALYSIS_INFO = index++;
    private static final int POS_NEG = index++;
    private static final int POS_DIFF = index++;
    private static final int POS_MORF = index++;
    private static final int POS_COMP = index++;
    private static final int ERR_FUNC = index++;
    private static final int ERR_RESULT = index++;
    private static final int INFO_REQUEST = index++;
    private static final int GB_ABNORMAL = index++;
    private static final int GB_SUSPICIOUS = index++;
    private static final int GR_ABNORMAL = index++;
    private static final int GR_SUSPICIOUS = index++;
    private static final int PLQ_ABNORMAL = index++;
    private static final int PLQ_SUSPICIOUS = index++;
    private static final int INFO_UNITS = index++;
    private static final int INFO_PLQ = index++;
    private static final int VALID = index++;
    private static final int RET_MESSAGES = index++;
    private static final int DELTA_MESSAGES = index++;
    private static final int SAMPLE_COMMENT = index++;
    private static final int PATIENT_NAME = index++;
    private static final int PATIENT_BIRTH_DATE = index++;
    private static final int PATIENT_GENDER = index++;
    private static final int PATIENT_COMMENT = index++;
    private static final int SERVICE = index++;
    private static final int MEDS = index++;
    private static final int TRANSMISSION_PARMS = index++;
    private static final int SEQUENECE_COUNT = index++;
    private static final int GB_10_uL = index++;
    private static final int GB_M = index++;
    private static final int GR_100000_uL = index++;
    private static final int GR_M = index++;
    private static final int HBG_g_L = index++;
    private static final int HBG_M = index++;
    private static final int HCT_10_NEG_1_PER = index++;
    private static final int HCT_M = index++;
    private static final int VGM_10_NEG_1_fL = index++;
    private static final int VGM_M = index++;
    private static final int TCMH_10_NEG_1_pg = index++;
    private static final int TCMH_M = index++;
    private static final int CCMH_g_L = index++;
    private static final int CCMH_M = index++;
    private static final int PLQ_10_3_uL = index++;
    private static final int PLQ_M = index++;
    private static final int IDR_SD_10_NEG_1_fL = index++;
    private static final int IDR_SD_M = index++;
    private static final int IDR_CV_10_NEG_1_PER = index++;
    private static final int IDR_CV_M = index++;
    private static final int IDP = index++;
    private static final int IDP_M = index++;
    private static final int VPM_10_NEG_1_fL = index++;
    private static final int VPM_M = index++;
    private static final int P_RGC_10_NEG_1_PER = index++;
    private static final int P_RGC_M = index++;
    private static final int PCT_10_NEG_2_PER = index++;
    private static final int PCT_M = index++;
    private static final int NEUT_COUNT_10_uL = index++;
    private static final int NEUT_COUNT_M = index++;
    private static final int LYMPH_COUNT_10_uL = index++;
    private static final int LYMPH_COUNT_M = index++;
    private static final int MONO_COUNT_10_uL = index++;
    private static final int MONO_COUNT_M = index++;
    private static final int EO_COUNT_10_uL = index++;
    private static final int EO_COUNT_M = index++;
    private static final int BASO_COUNT_10_uL = index++;
    private static final int BASO_COUNT_M = index++;
    private static final int NEUT_PER_10_NEG_1_PER = index++;
    private static final int NEUT_PER_M = index++;
    private static final int LYMPH_PER_10_NEG_1_PER = index++;
    private static final int LYMPH_PER_M = index++;
    private static final int MONO_PER_10_NEG_1_PER = index++;
    private static final int MONO_PER_M = index++;
    private static final int EO_PER_10_NEG_1_PER = index++;
    private static final int EO_PER_M = index++;
    private static final int BASO_PER_10_NEG_1_PER = index++;
    private static final int BASO_PER_M = index++;
    private static final int RET_COUNT_10_2_uL = index++;
    private static final int RET_COUNT_M = index++;
    private static final int RET_PER_10_NEG_2_PER = index++;
    private static final int RET_PER_M = index++;
    private static final int LFR_10_NEG_1_PER = index++;
    private static final int LFR_M = index++;
    private static final int MFR_10_NEG_1_PER = index++;
    private static final int MFR_M = index++;
    private static final int HFR_10_NEG_1_PER = index++;
    private static final int HFR_M = index++;
    private static final int IRF_10_NEG_1_PER = index++;
    private static final int IRF_M = index++;
    private static final int IG_COUNT_10_uL = index++;
    private static final int IG_COUNT_M = index++;
    private static final int IG_PER_10_NEG_1_PER = index++;
    private static final int IG_PER_M = index++;
    private static final int NEUT_COUNT_AND_10_uL = index++;
    private static final int NEUT_COUNT_AND_M = index++;
    private static final int NEUT_PER_AND_10_NEG_1_PER = index++;
    private static final int NEUT_PER_AND_M = index++;
    private static final int NRBC_PLUS_W_10_uL = index++;
    private static final int NRBC_PLUS_W_M = index++;
    private static final int LYMPH_COUNT_AND_10_uL = index++;
    private static final int LYMPH_COUNT_AND_M = index++;
    private static final int LYMPH_PER_AND_10_NEG_1_PER = index++;
    private static final int LYMPH_PER_AND_M = index++;
    private static final int OTHER_COUNT_10_uL = index++;
    private static final int OTHER_COUNT_M = index++;
    private static final int OTHER_PER_10_NEG_1_PER = index++;
    private static final int OTHER_PER_M = index++;
    private static final int GR_O_10_4_uL = index++;
    private static final int GR_O_M = index++;
    private static final int PLQ_O_10_3_uL = index++;
    private static final int PLQ_O_M = index++;
    private static final int IP_Abn_GB_Scatterg_GB_Anorm = index++;
    private static final int IP_Abn_GB_NEUTROPENIA = index++;
    private static final int IP_Abn_GB_NEUTROPHILIE = index++;
    private static final int IP_Abn_GB_LYMPHOPENIA = index++;
    private static final int IP_Abn_GB_LYMPHCYTOSIS = index++;
    private static final int IP_Abn_GB_MONOCYTOSIS = index++;
    private static final int IP_Abn_GB_EOSINOPHILIE = index++;
    private static final int IP_Abn_GB_BASOPHILIE = index++;
    private static final int IP_Abn_GB_LEUCOPENIA = index++;
    private static final int IP_Abn_GB_LEUCOCYTOSIS = index++;
    private static final int IP_Abn_GR_Dist_GR_An = index++;
    private static final int IP_Abn_GR_D_pop_GR = index++;
    private static final int IP_Abn_GR_ANISOCYTOSIS = index++;
    private static final int IP_Abn_GR_MICROCYTOSIS = index++;
    private static final int IP_Abn_GR_MACROCYTOSIS = index++;
    private static final int IP_Abn_GR_HYPOCHROMIA = index++;
    private static final int IP_Abn_GR_ANEMIA = index++;
    private static final int IP_Abn_GR_ERYTHROCYTOSIS = index++;
    private static final int IP_Abn_GR_Scatterg_RET_Anorm = index++;
    private static final int IP_Abn_GR_RETICULOCYTOSIS = index++;
    private static final int IP_Abn_PLQ_Dist_PLQ_An = index++;
    private static final int IP_Abn_PLQ_THROMBOCYTOPENIA = index++;
    private static final int IP_Abn_PLQ_THROMBOCYTOSIS = index++;
    private static final int IP_Abn_PLQ_Scatterg_PLQ_Anorm = index++;
    private static final int IP_SUS_GB_Blasts = index++;
    private static final int IP_SUS_GB_Gra_Immat = index++;
    private static final int IP_SUS_GB_Dev_Gauche = index++;
    private static final int IP_SUS_GB_Lympho_Aty = index++;
    private static final int IP_SUS_GB_Anor_Ly_Blasts = index++;
    private static final int IP_SUS_GB_NRBC = index++;
    private static final int IP_SUS_GB_Res_GR_Lyse = index++;
    private static final int IP_SUS_GR_Agglut_GR = index++;
    private static final int IP_SUS_GR_Turb_HGB_Interf = index++;
    private static final int IP_SUS_GR_IRON_DEFICIENCY = index++;
    private static final int IP_SUS_GR_Def_HGB = index++;
    private static final int IP_SUS_GR_Fragments = index++;
    private static final int IP_SUS_PLQ_Agg_PLQ = index++;
    private static final int IP_SUS_PLQ_Agg_PLQ_S = index++;
    private static final int DEFAULT_INFO = index++;
    private static final int Qflag_Blasts = index++;
    private static final int Qflag_Gra_Immat = index++;
    private static final int Qflag_Dev_Gauche = index++;
    private static final int Qflag_Lympho_Aty = index++;
    private static final int Qflag_NRBC = index++;
    private static final int Qflag_Abn_Ly_Bla = index++;
    private static final int Qflag_Res_GR_Lysis = index++;
    private static final int Qflag_Agglut_GR = index++;
    private static final int Qflag_Turb_HGB = index++;
    private static final int Qflag_IRON_DEFICIENCY = index++;
    private static final int Qflag_Def_HGB = index++;
    private static final int Qflag_Fragments = index++;
    private static final int Qflag_Agg_PLQ = index++;
    private static final int Qflag_Agg_PLQ_S = index++;
    private static final int columns = index++;

    private static final String DELIMITER = ",";

    private static final String DATE_PATTERN = "dd/MM/yyyy HH:mm:ss";
    private static String[] testNameIndex = new String[columns];
    private static String[] unitsIndex = new String[columns];
    private static Boolean[] readOnlyIndex = new Boolean[columns];
    private static int[] scaleIndex = new int[columns];
    private static int[] orderedTestIndexs = new int[18];

    {
        testNameIndex[GB_10_uL] = "GB_10_uL";
        testNameIndex[GR_100000_uL] = "GR_100000_uL";
        testNameIndex[NEUT_PER_10_NEG_1_PER] = "NEUT_PER_10_NEG_1_PER";
        testNameIndex[HBG_g_L] = "HBG_g_L";
        testNameIndex[LYMPH_PER_10_NEG_1_PER] = "LYMPH_PER_10_NEG_1_PER";
        testNameIndex[HCT_10_NEG_1_PER] = "HCT_10_NEG_1_PER";
        testNameIndex[MONO_PER_10_NEG_1_PER] = "MONO_PER_10_NEG_1_PER";
        testNameIndex[VGM_10_NEG_1_fL] = "VGM_10_NEG_1_fL";
        testNameIndex[EO_PER_10_NEG_1_PER] = "EO_PER_10_NEG_1_PER";
        testNameIndex[TCMH_10_NEG_1_pg] = "TCMH_10_NEG_1_pg";
        testNameIndex[BASO_PER_10_NEG_1_PER] = "BASO_PER_10_NEG_1_PER";
        testNameIndex[CCMH_g_L] = "CCMH_g_L";
        testNameIndex[PLQ_10_3_uL] = "PLQ_10_3_uL";

        /*
         * testNameIndex[NEUT_COUNT_10_uL] = "NE#"; testNameIndex[MONO_COUNT_10_uL] =
         * "MO#"; testNameIndex[BASO_COUNT_10_uL] = "BA#";
         * testNameIndex[LYMPH_COUNT_10_uL] = "LY#"; testNameIndex[EO_COUNT_10_uL] =
         * "EO#";
         */

        unitsIndex[GB_10_uL] = "10^3/ul";
        unitsIndex[GR_100000_uL] = "10^6/ul";
        unitsIndex[NEUT_PER_10_NEG_1_PER] = "%";
        unitsIndex[HBG_g_L] = "g/dl";
        unitsIndex[LYMPH_PER_10_NEG_1_PER] = "%";
        unitsIndex[HCT_10_NEG_1_PER] = "%";
        unitsIndex[MONO_PER_10_NEG_1_PER] = "%";
        unitsIndex[VGM_10_NEG_1_fL] = "fl";
        unitsIndex[EO_PER_10_NEG_1_PER] = "%";
        unitsIndex[TCMH_10_NEG_1_pg] = "pg";
        unitsIndex[BASO_PER_10_NEG_1_PER] = "%";
        unitsIndex[CCMH_g_L] = "g/l";
        unitsIndex[PLQ_10_3_uL] = "10^3/ul";

        /*
         * unitsIndex[NEUT_COUNT_10_uL] = " "; unitsIndex[MONO_COUNT_10_uL] = " ";
         * unitsIndex[BASO_COUNT_10_uL] = " "; unitsIndex[LYMPH_COUNT_10_uL] = " ";
         * unitsIndex[EO_COUNT_10_uL] = " ";
         */
        for (int i = 0; i < readOnlyIndex.length; i++) {
            readOnlyIndex[i] = Boolean.FALSE;
        }

        /*
         * readOnlyIndex[NEUT_COUNT_10_uL] = Boolean.TRUE;
         * readOnlyIndex[MONO_COUNT_10_uL] = Boolean.TRUE;
         * readOnlyIndex[BASO_COUNT_10_uL] = Boolean.TRUE;
         * readOnlyIndex[LYMPH_COUNT_10_uL] = Boolean.TRUE;
         * readOnlyIndex[EO_COUNT_10_uL] = Boolean.TRUE;
         */
        scaleIndex[GB_10_uL] = 100;
        scaleIndex[GR_100000_uL] = 100;
        scaleIndex[NEUT_PER_10_NEG_1_PER] = 10;
        scaleIndex[HBG_g_L] = 10;
        scaleIndex[LYMPH_PER_10_NEG_1_PER] = 10;
        scaleIndex[HCT_10_NEG_1_PER] = 10;
        scaleIndex[MONO_PER_10_NEG_1_PER] = 10;
        scaleIndex[VGM_10_NEG_1_fL] = 10;
        scaleIndex[EO_PER_10_NEG_1_PER] = 10;
        scaleIndex[TCMH_10_NEG_1_pg] = 10;
        scaleIndex[BASO_PER_10_NEG_1_PER] = 10;
        scaleIndex[CCMH_g_L] = 10;
        scaleIndex[PLQ_10_3_uL] = 1;
        /*
         * scaleIndex[NEUT_COUNT_10_uL] = 100; scaleIndex[MONO_COUNT_10_uL] = 100;
         * scaleIndex[BASO_COUNT_10_uL] = 10; scaleIndex[LYMPH_COUNT_10_uL] = 100;
         * scaleIndex[EO_COUNT_10_uL] = 100;
         */

        orderedTestIndexs[0] = GB_10_uL;
        orderedTestIndexs[6] = GR_100000_uL;
        orderedTestIndexs[1] = NEUT_PER_10_NEG_1_PER;
        orderedTestIndexs[7] = HBG_g_L;
        orderedTestIndexs[2] = LYMPH_PER_10_NEG_1_PER;
        orderedTestIndexs[8] = HCT_10_NEG_1_PER;
        orderedTestIndexs[3] = MONO_PER_10_NEG_1_PER;
        orderedTestIndexs[9] = VGM_10_NEG_1_fL;
        orderedTestIndexs[4] = EO_PER_10_NEG_1_PER;
        orderedTestIndexs[10] = TCMH_10_NEG_1_pg;
        orderedTestIndexs[5] = BASO_PER_10_NEG_1_PER;
        orderedTestIndexs[11] = CCMH_g_L;
        orderedTestIndexs[12] = PLQ_10_3_uL;

        orderedTestIndexs[13] = NEUT_COUNT_10_uL;
        orderedTestIndexs[15] = MONO_COUNT_10_uL;
        orderedTestIndexs[17] = BASO_COUNT_10_uL;
        orderedTestIndexs[14] = LYMPH_COUNT_10_uL;
        orderedTestIndexs[16] = EO_COUNT_10_uL;

        /*
         * GB_10_uL WBC 100 GR_100000_uL RBC 100 NEUT_PER_10_NEG_1_PER NE% 10 HBG_g_L
         * HGB 10 LYMPH_PER_10_NEG_1_PER LY% 10 HCT_10_NEG_1_PER HCT 10
         * MONO_PER_10_NEG_1_PER MO% 10 VGM_10_NEG_1_fL MVC 10 EO_PER_10_NEG_1_PER EO%
         * 10 TCMH_10_NEG_1_pg MCH 10 BASO_PER_10_NEG_1_PER BA% 10 CCMH_g_L MCHC 10
         * PLQ_10_3_uL PLT 1
         *
         * NEUT_COUNT_10_uL NE# 100 MONO_COUNT_10_uL MO# 100 BASO_COUNT_10_uL BA# 10
         * LYMPH_COUNT_10_uL LY# 100 EO_COUNT_10_uL EO# 100
         */
    }

    @Override
    public boolean insert(List<String> lines, String currentUserId) {

        boolean successful = true;

        List<AnalyzerResults> results = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {
            addAnalyzerResultFromLine(results, lines.get(i));
        }

        if (results.size() > 0) {
            try {
                persistResults(results, currentUserId);
            } catch (LIMSRuntimeException e) {
                successful = false;
            }
        }

        return successful;
    }

    private void addAnalyzerResultFromLine(List<AnalyzerResults> results, String line) {
        String[] fields = line.split(DELIMITER);

        AnalyzerReaderUtil readerUtil = new AnalyzerReaderUtil();
        String analyzerAccessionNumber = fields[ACCESSION];
        Timestamp timestamp = DateUtil.convertStringDateToTimestampWithPattern(fields[DATE] + " " + fields[TIME],
                DATE_PATTERN);

        List<AnalyzerResults> readOnlyResults = new ArrayList<>();

        // the reason for the indirection is to get the order of tests correct
        for (int i = 0; i < orderedTestIndexs.length; i++) {
            int testIndex = orderedTestIndexs[i];

            if (!GenericValidator.isBlankOrNull(testNameIndex[testIndex])) {
                MappedTestName mappedName = AnalyzerTestNameCache.getInstance()
                        .getMappedTest(AnalyzerTestNameCache.SYSMEX_XT2000_NAME, testNameIndex[testIndex]);

                if (mappedName == null) {
                    mappedName = AnalyzerTestNameCache.getInstance()
                            .getEmptyMappedTestName(AnalyzerTestNameCache.SYSMEX_XT2000_NAME, testNameIndex[testIndex]);
                }

                AnalyzerResults analyzerResults = new AnalyzerResults();

                analyzerResults.setAnalyzerId(mappedName.getAnalyzerId());

                double result = Double.NaN;

                try {
                    result = Double.parseDouble(fields[testIndex]) / scaleIndex[testIndex];
                } catch (NumberFormatException e) {
                    LogEvent.logError(e.getMessage(), e);
                    // no-op -- defaults to NAN
                }

                analyzerResults.setResult(String.valueOf(result));
                analyzerResults.setUnits(unitsIndex[testIndex]);
                analyzerResults.setCompleteDate(timestamp);
                analyzerResults.setTestId(mappedName.getTestId());
                analyzerResults.setAccessionNumber(analyzerAccessionNumber);
                analyzerResults.setTestName(mappedName.getOpenElisTestName());
                analyzerResults.setReadOnly(readOnlyIndex[testIndex]);

                if (analyzerAccessionNumber != null) {
                    analyzerResults.setIsControl(analyzerAccessionNumber.startsWith(CONTROL_ACCESSION_PREFIX));
                } else {
                    analyzerResults.setIsControl(false);
                }

                if (analyzerResults.isReadOnly()) {
                    readOnlyResults.add(analyzerResults);
                } else {
                    results.add(analyzerResults);
                }

                AnalyzerResults resultFromDB = readerUtil.createAnalyzerResultFromDB(analyzerResults);
                if (resultFromDB != null) {
                    results.add(resultFromDB);
                }
            }
        }

        results.addAll(readOnlyResults);
    }

    @Override
    public String getError() {
        return "Sysmex analyzer unable to write to database";
    }
}
