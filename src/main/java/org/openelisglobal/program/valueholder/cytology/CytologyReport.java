package org.openelisglobal.program.valueholder.cytology;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

@Entity
@Table(name = "cytology_report")
public class CytologyReport extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cytology_report_generator")
    @SequenceGenerator(name = "cytology_report_generator", sequenceName = "cytology_report_seq", allocationSize = 1)
    private Integer id;

    @Type(type = "org.hibernate.type.BinaryType")
    private byte[] image;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "report_type")
    @Enumerated(EnumType.STRING)
    @NotNull
    private CytologyReportType reportType;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public CytologyReportType getReportType() {
        return reportType;
    }

    public void setReportType(CytologyReportType reportType) {
        this.reportType = reportType;
    }

    public enum CytologyReportType {
        CERVICAL_OR_VAGINAL_CYTOLOGY("Report For Cervical Or Vaginal Cytology"), PAP_SMEAR("Reporting Of Pap Smear");

        private String display;

        CytologyReportType(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }
}
