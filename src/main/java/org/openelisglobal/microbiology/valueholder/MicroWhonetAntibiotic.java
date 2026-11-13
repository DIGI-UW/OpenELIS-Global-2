package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * One row of WHONET's antibiotic table, held as WHONET publishes it. A code has
 * one row per disk potency WHONET lists for it.
 */
@Entity
@Table(name = "micro_whonet_antibiotic", schema = "clinlims")
@Getter
@Setter
public class MicroWhonetAntibiotic extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "whonet_abx_code", nullable = false)
    private String whonetAbxCode;

    @Column(name = "who_code")
    private String whoCode;

    @Column(name = "din_code")
    private String dinCode;

    @Column(name = "jac_code")
    private String jacCode;

    @Column(name = "eucast_code")
    private String eucastCode;

    @Column(name = "user_code")
    private String userCode;

    @Column(name = "antibiotic", nullable = false)
    private String antibiotic;

    @Column(name = "guidelines")
    private String guidelines;

    @Column(name = "clsi")
    private String clsi;

    @Column(name = "eucast")
    private String eucast;

    @Column(name = "sfm")
    private String sfm;

    @Column(name = "srga")
    private String srga;

    @Column(name = "bsac")
    private String bsac;

    @Column(name = "din")
    private String din;

    @Column(name = "neo")
    private String neo;

    @Column(name = "afa")
    private String afa;

    @Column(name = "abx_number")
    private String abxNumber;

    @Column(name = "potency")
    private String potency;

    @Column(name = "atc_code")
    private String atcCode;

    @Column(name = "antibiotic_class")
    private String antibioticClass;

    @Column(name = "subclass")
    private String subclass;

    @Column(name = "prof_class")
    private String profClass;

    @Column(name = "who_aware")
    private String whoAware;

    @Column(name = "cia_category")
    private String ciaCategory;

    @Column(name = "clsi_order")
    private String clsiOrder;

    @Column(name = "eucast_order")
    private String eucastOrder;

    @Column(name = "human")
    private String human;

    @Column(name = "veterinary")
    private String veterinary;

    @Column(name = "animal_gp")
    private String animalGp;

    @Column(name = "loinc_comp")
    private String loincComp;

    @Column(name = "loinc_gen")
    private String loincGen;

    @Column(name = "loinc_disk")
    private String loincDisk;

    @Column(name = "loinc_mic")
    private String loincMic;

    @Column(name = "loinc_etest")
    private String loincEtest;

    @Column(name = "loinc_slow")
    private String loincSlow;

    @Column(name = "loinc_afb")
    private String loincAfb;

    @Column(name = "loinc_sbt")
    private String loincSbt;

    @Column(name = "loinc_mlc")
    private String loincMlc;

    @Column(name = "date_entered")
    private String dateEntered;

    @Column(name = "date_modified")
    private String dateModified;

    @Column(name = "comments")
    private String comments;
}
