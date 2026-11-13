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
 */
package org.openelisglobal.samplenewborn.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.common.valueholder.BaseObject;

@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "SAMPLE_NEWBORN")
@AttributeOverride(name = "lastupdated", column = @Column(name = "LASTUPDATED"))
public class SampleNewborn extends BaseObject<String> {

    @Id
    @Column(name = "ID", precision = 10, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String id;

    @Column(name = "WEIGHT", precision = 5, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String weight;

    @Column(name = "MULTI_BIRTH", length = 1)
    private String multiBirth;

    @Column(name = "BIRTH_ORDER", precision = 2, scale = 0)
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    private String birthOrder;

    @Column(name = "GESTATIONAL_WEEK", precision = 5, scale = 2)
    private double gestationalWeek;

    @Column(name = "DATE_FIRST_FEEDING", length = 7)
    private Timestamp dateFirstFeeding;

    @Transient
    private String dateFirstFeedingForDisplay;

    @Column(name = "BREAST", length = 1)
    private String breast;

    @Column(name = "TPN", length = 1)
    private String tpn;

    @Column(name = "FORMULA", length = 1)
    private String formula;

    @Column(name = "MILK", length = 1)
    private String milk;

    @Column(name = "SOY", length = 1)
    private String soy;

    @Column(name = "JAUNDICE", length = 1)
    private String jaundice;

    @Column(name = "ANTIBIOTICS", length = 1)
    private String antibiotic;

    @Column(name = "TRANSFUSED", length = 1)
    private String transfused;

    @Column(name = "DATE_TRANSFUSION", length = 7)
    private Timestamp dateTransfution;

    @Transient
    private String dateTransfutionForDisplay;

    @Column(name = "MEDICAL_RECORD_NUMBER", length = 18)
    private String medicalRecordNumber;

    @Column(name = "NICU", length = 1)
    private String nicu;

    @Column(name = "BIRTH_DEFECT", length = 1)
    private String birthDefect;

    @Column(name = "PREGNANCY_COMPLICATION", length = 1)
    private String pregnancyComplication;

    @Column(name = "DECEASED_SIBLING", length = 1)
    private String deceasedSibling;

    @Column(name = "CAUSE_OF_DEATH", length = 50)
    private String causeOfDeath;

    @Column(name = "FAMILY_HISTORY", length = 1)
    private String familyHistory;

    @Column(name = "OTHER", length = 100)
    private String other;

    @Column(name = "Y_NUMBER", length = 18)
    private String yNumber;

    @Column(name = "YELLOW_CARD", length = 1)
    private String yellowCard;

    public SampleNewborn() {
        super();
    }

    public void setDateFirstFeeding(Timestamp dateFirstFeeding) {
        this.dateFirstFeeding = dateFirstFeeding;
        this.dateFirstFeedingForDisplay = DateUtil.convertTimestampToStringDate(dateFirstFeeding);
    }

    public void setDateTransfution(Timestamp dateTransfution) {
        this.dateTransfution = dateTransfution;
        this.dateTransfutionForDisplay = DateUtil.convertTimestampToStringDate(dateTransfution);
    }
}
