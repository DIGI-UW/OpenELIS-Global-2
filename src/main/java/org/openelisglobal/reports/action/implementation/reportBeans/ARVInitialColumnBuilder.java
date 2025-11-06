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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.reports.action.implementation.reportBeans;

import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.BLANK;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DICT;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DICT_PLUS;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DICT_RAW;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;

import org.openelisglobal.reports.action.implementation.Report.DateRange;

public class ARVInitialColumnBuilder extends ARVColumnBuilder {

    public ARVInitialColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();

        add("educationLevel", "SESNIVET");
        add("maritalStatus", "SESETCV1");
        add("nationality", "NATIONAL", DICT_PLUS); // nationality "other" is NOT stored in some other observation, but
        // in the same observ, so we have to let actual strings pass through.
        add("legalResidence", "RESIDE", NONE);
        add("hivStatus", "VIH", DICT_RAW);
        add("nameOfDoctor", "NOMMED", NONE);
        add("anyPriorDiseases", "HISATCD");
        add("anyCurrentDiseases", "IODERSVIS");
        add("arvProphylaxisBenefit", "HISPRARV");
        add("arvProphylaxis", "HISPRTYP");
        add("currentARVTreatment", "HISTXARV");
        add("priorARVTreatment", "HISTRARV");
        add("cd4Count", "NBCD4", BLANK);
        add("cd4Percent", "PRECCD4", BLANK);
        add("priorCd4Date", "DATECD4", BLANK);
        add("antiTbTreatment", "ATCDTB", BLANK);
        add("interruptedARVTreatment", "ATCDARV", BLANK);
        add("arvTreatmentAnyAdverseEffects", "EIARV", BLANK);
        add("arvTreatmentChange", "CHANGETTT", BLANK);
        add("arvTreatmentNew", "ARVPRESC", BLANK);
        add("arvTreatmentRegime", "ARVREG", BLANK);
        add("cotrimoxazoleTreatAnyAdvEff", "EFTINDE", BLANK);
        add("anySecondaryTreatment", "AUTRPRO", BLANK);
        add("secondaryTreatment", "PREAUTRE", BLANK);
        add("clinicVisits", "CONTACTS", BLANK);
        add("priorARVTreatmentINNs1", "DERXARV1", NONE);
        add("priorARVTreatmentINNs2", "DERXARV2", NONE);
        add("priorARVTreatmentINNs3", "DERXARV3", NONE);
        add("priorARVTreatmentINNs4", "DERXARV4", NONE);
        add("futureARVTreatmentINNs1", "COMBIARV1", BLANK);
        add("futureARVTreatmentINNs2", "COMBIARV2", BLANK);
        add("futureARVTreatmentINNs3", "COMBIARV3", BLANK);
        add("futureARVTreatmentINNs4", "COMBIARV4", BLANK);
        add("CTBPul", "CTBPUL");
        add("CTBExpul", "CTBEXPUL");
        add("CCrblToxo", "CTOXOCER");
        add("CCryptoMen", "CMENCRY");
        add("CGenPrurigo", "CPRURIGEN");
        add("CIST", "CIST");
        add("CCervCancer", "CCANCOL");
        add("COpharCand", "CCANDOROP");
        add("CKaposiSarc", "CSARCOMK");
        add("CShingles", "CZONA");
        // add("currentDisease", "CAUTRIO", NONE);
        add("currentDiseaseOther", "CAUTRIO", NONE);

        add("arvTreatmentAdvEffType1", "EIARV1", BLANK);
        add("arvTreatmentAdvEffGrd1", "GEIARV1", BLANK);
        add("arvTreatmentAdvEffType2", "EIARV2", BLANK);
        add("arvTreatmentAdvEffGrd2", "GEIARV2", BLANK);
        add("arvTreatmentAdvEffType3", "EIARV3", BLANK);
        add("arvTreatmentAdvEffGrd3", "GEIARV3", BLANK);
        add("arvTreatmentAdvEffType4", "EIARV4", BLANK);
        add("arvTreatmentAdvEffGrd4", "GEIARV4", BLANK);

        add("cotrimoxazoleTreatment", "COTRIMO");
        add("cotrimoxazoleTreatAdvEffType1", "EICOTR1", BLANK);
        add("cotrimoxazoleTreatAdvEffGrd1", "GEICOTR1", BLANK);
        add("cotrimoxazoleTreatAdvEffType2", "EICOTR2", BLANK);
        add("cotrimoxazoleTreatAdvEffGrd2", "GEICOTR2", BLANK);
        add("cotrimoxazoleTreatAdvEffType3", "EICOTR3", BLANK);
        add("cotrimoxazoleTreatAdvEffGrd3", "GEICOTR3", BLANK);
        add("cotrimoxazoleTreatAdvEffType4", "EICOTR4", BLANK);
        add("cotrimoxazoleTreatAdvEffGrd4", "GEICOTR4", BLANK);
        add("PTBPul", "PTBPUL");
        add("PTBExpul", "PTBEXPUL");
        add("PCrblToxo", "PTOXOCER");
        add("PCryptoMen", "PMENCRY");
        add("PGenPrurigo", "PPRURIGEN");
        add("PDiarrheaC", "PDIARCHR");
        add("PIST", "PIST");
        add("PCervCancer", "PCANCOL");
        add("POpharCand", "PCANDOROP");
        add("PKaposiSarc", "PSARCOMK");
        add("PShingles", "PZONA");
        // add("priorDisease", "PAUTRIO", NONE);
        add("priorDiseaseOther", "PAUTRIO", NONE);

        add("aidsStage", "STAVOL");
        add("patientWeight", "PHYOIDS", NONE);
        add("karnofskyScore", "PHYKARN", NONE);
        add("currentOITreatment", "TRAITIO", DICT);
        addAllResultsColumns();
    }
}
