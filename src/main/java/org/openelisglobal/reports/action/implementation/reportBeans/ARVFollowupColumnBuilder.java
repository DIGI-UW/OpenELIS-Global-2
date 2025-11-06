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
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.DICT_RAW;
import static org.openelisglobal.reports.action.implementation.reportBeans.CSVColumnBuilder.Strategy.NONE;

import org.openelisglobal.reports.action.implementation.Report.DateRange;

public class ARVFollowupColumnBuilder extends ARVColumnBuilder {

    public ARVFollowupColumnBuilder(DateRange dateRange, String projectStr) {
        super(dateRange, projectStr);
    }

    @Override
    protected void defineAllReportColumns() {
        defineBasicColumns();

        add("educationLevel", "SESNIVET", BLANK);
        add("maritalStatus", "SESETCV1", BLANK);
        add("nationality", "NATIONAL", BLANK); // nationality "other" is NOT stored in some other observation, but in
        // the same observ, so we have to let actual strings pass through.
        add("legalResidence", "RESIDE", BLANK);
        add("hivStatus", "STATVIH", DICT_RAW);
        add("hivStatus", "VIH", new ARVFollowupInitalHIVStrategy());
        add("nameOfDoctor", "NOMMED", NONE);
        add("anyPriorDiseases", "HISATCD", BLANK);
        add("anyCurrentDiseases", "IODERSVIS");
        add("arvProphylaxisBenefit", "HISPRARV", BLANK);
        add("arvProphylaxis", "HISPRTYP", BLANK);
        add("currentARVTreatment", "HISTXARV", BLANK);
        add("priorARVTreatment", "HISTRARV", BLANK);
        add("cd4Count", "NBCD4", NONE);
        add("cd4Percent", "PRECCD4", NONE);
        add("priorCd4Date", "DATECD4", NONE);
        add("antiTbTreatment", "ATCDTB");
        add("interruptedARVTreatment", "INTARV");
        add("priorARVTreatment", "ATCDARV");
        add("arvTreatmentAnyAdverseEffects", "EIARV");
        add("arvTreatmentChange", "CHANGETTT");
        add("arvTreatmentNew", "ARVPRESC");
        add("arvTreatmentRegime", "ARVREG");
        add("cotrimoxazoleTreatAnyAdvEff", "EFTINDE");
        add("anySecondaryTreatment", "AUTRPRO");
        add("secondaryTreatment", "PREAUTRE");
        add("clinicVisits", "CONTACTS", NONE);
        add("priorARVTreatmentINNs1", "DERXARV1", NONE);
        add("priorARVTreatmentINNs2", "DERXARV2", NONE);
        add("priorARVTreatmentINNs3", "DERXARV3", NONE);
        add("priorARVTreatmentINNs4", "DERXARV4", NONE);
        add("futureARVTreatmentINNs1", "COMBIARV1", NONE);
        add("futureARVTreatmentINNs2", "COMBIARV2", NONE);
        add("futureARVTreatmentINNs3", "COMBIARV3", NONE);
        add("futureARVTreatmentINNs4", "COMBIARV4", NONE);
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

        add("arvTreatmentAdvEffType1", "EIARV1", NONE);
        add("arvTreatmentAdvEffGrd1", "GEIARV1", NONE);
        add("arvTreatmentAdvEffType2", "EIARV2", NONE);
        add("arvTreatmentAdvEffGrd2", "GEIARV2", NONE);
        add("arvTreatmentAdvEffType3", "EIARV3", NONE);
        add("arvTreatmentAdvEffGrd3", "GEIARV3", NONE);
        add("arvTreatmentAdvEffType4", "EIARV4", NONE);
        add("arvTreatmentAdvEffGrd4", "GEIARV4", NONE);

        add("cotrimoxazoleTreatment", "COTRIMO");
        add("cotrimoxazoleTreatAdvEffType1", "EICOTR1", NONE);
        add("cotrimoxazoleTreatAdvEffGrd1", "GEICOTR1", NONE);
        add("cotrimoxazoleTreatAdvEffType2", "EICOTR2", NONE);
        add("cotrimoxazoleTreatAdvEffGrd2", "GEICOTR2", NONE);
        add("cotrimoxazoleTreatAdvEffType3", "EICOTR3", NONE);
        add("cotrimoxazoleTreatAdvEffGrd3", "GEICOTR3", NONE);
        add("cotrimoxazoleTreatAdvEffType4", "EICOTR4", NONE);
        add("cotrimoxazoleTreatAdvEffGrd4", "GEICOTR4", NONE);
        add("PTBPul", "PTBPUL", BLANK);
        add("PTBExpul", "PTBEXPUL", BLANK);
        add("PCrblToxo", "PTOXOCER", BLANK);
        add("PCryptoMen", "PMENCRY", BLANK);
        add("PGenPrurigo", "PPRURIGEN", BLANK);
        add("PIST", "PIST", BLANK);
        add("PCervCancer", "PCANCOL", BLANK);
        add("POpharCand", "PCANDOROP", BLANK);
        add("PKaposiSarc", "PSARCOMK", BLANK);
        add("PShingles", "PZONA", BLANK);
        // add("priorDisease", "PAUTRIO", NONE);
        add("priorDiseaseOther", "PAUTRIO", BLANK);

        add("aidsStage", "STAVOL", BLANK);
        add("patientWeight", "PHYOIDS", NONE);
        add("karnofskyScore", "PHYKARN", NONE);
        add("currentOITreatment", "TRAITIO", BLANK);
        addAllResultsColumns();
    }
}
