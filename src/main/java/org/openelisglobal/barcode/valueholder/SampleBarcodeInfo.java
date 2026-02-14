package org.openelisglobal.barcode.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * Class for persisting bar code label information in the database
 *
 * @author Caleb
 */

@Entity
@Table(name = "sample_barcode_info")
public class SampleBarcodeInfo extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_barcode_info_generator")
    @SequenceGenerator(name = "sample_barcode_info_generator", sequenceName = "sample_barcode_info_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Valid
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sample_id", referencedColumnName = "id")
    private Sample sample;

    // number of labels to print for this order
    @Column(name = "print_order_num")
    private Integer printOrderNum;

    public SampleBarcodeInfo() {
        super();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Integer getPrintOrderNum() {
        return printOrderNum;
    }

    public void setPrintOrderNum(Integer printOrderNum) {
        this.printOrderNum = printOrderNum;
    }

}
