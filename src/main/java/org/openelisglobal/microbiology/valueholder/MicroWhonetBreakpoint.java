package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/** One row of WHONET's breakpoint table, held as WHONET publishes it. */
@Entity
@Table(name = "micro_whonet_breakpoint", schema = "clinlims")
@Getter
@Setter
public class MicroWhonetBreakpoint extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "guidelines", nullable = false)
    private String guidelines;

    @Column(name = "year", nullable = false)
    private String year;

    @Column(name = "test_method", nullable = false)
    private String testMethod;

    @Column(name = "potency")
    private String potency;

    @Column(name = "organism_code", nullable = false)
    private String organismCode;

    @Column(name = "organism_code_type")
    private String organismCodeType;

    @Column(name = "breakpoint_type", nullable = false)
    private String breakpointType;

    @Column(name = "host")
    private String host;

    @Column(name = "site_of_infection")
    private String siteOfInfection;

    @Column(name = "reference_table")
    private String referenceTable;

    @Column(name = "reference_sequence")
    private String referenceSequence;

    @Column(name = "whonet_abx_code")
    private String whonetAbxCode;

    @Column(name = "whonet_test")
    private String whonetTest;

    @Column(name = "r")
    private String r;

    @Column(name = "i")
    private String i;

    @Column(name = "sdd")
    private String sdd;

    @Column(name = "s")
    private String s;

    @Column(name = "ecv_ecoff")
    private String ecvEcoff;

    @Column(name = "ecv_ecoff_tentative")
    private String ecvEcoffTentative;

    @Column(name = "date_entered")
    private String dateEntered;

    @Column(name = "date_modified")
    private String dateModified;

    @Column(name = "comments")
    private String comments;
}
