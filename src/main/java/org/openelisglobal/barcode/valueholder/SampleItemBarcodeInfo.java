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
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Class for persisting bar code label information in the database
 *
 * @author Caleb
 */

@Entity
@Table(name = "sample_item_barcode_info")
public class SampleItemBarcodeInfo extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_item_barcode_info_generator")
    @SequenceGenerator(name = "sample_item_barcode_info_generator", sequenceName = "sample_item_barcode_info_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Valid
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sample_item_id", referencedColumnName = "id")
    private SampleItem sampleItem;

    // number of specimen labels to print for this sampleItem
    @Column(name = "print_specimen_num")
    private Integer printSpecimenNum;

    // number of block labels to print for this sampleItem
    @Column(name = "print_block_num")
    private Integer printBlockNum;

    // number of slide labels to print for this sampleItem
    @Column(name = "print_slide_num")
    private Integer printSlideNum;

    // number of freezer labels to print for this sampleItem
    @Column(name = "print_freezer_num")
    private Integer printFreezerNum;

    @Column(name = "printed_specimen_count")
    private Integer printedSpecimenCount;

    @Column(name = "printed_block_count")
    private Integer printedBlockCount;

    @Column(name = "printed_slide_count")
    private Integer printedSlideCount;

    @Column(name = "printed_freezer_count")
    private Integer printedFreezerCount;

    public SampleItemBarcodeInfo() {
        super();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public Integer getPrintSpecimenNum() {
        return printSpecimenNum;
    }

    public void setPrintSpecimenNum(Integer printSpecimenNum) {
        this.printSpecimenNum = printSpecimenNum;
    }

    public Integer getPrintBlockNum() {
        return printBlockNum;
    }

    public void setPrintBlockNum(Integer printBlockNum) {
        this.printBlockNum = printBlockNum;
    }

    public Integer getPrintSlideNum() {
        return printSlideNum;
    }

    public void setPrintSlideNum(Integer printSlideNum) {
        this.printSlideNum = printSlideNum;
    }

    public Integer getPrintFreezerNum() {
        return printFreezerNum;
    }

    public void setPrintFreezerNum(Integer printFreezerNum) {
        this.printFreezerNum = printFreezerNum;
    }

    public Integer getPrintedSpecimenCount() {
        return printedSpecimenCount;
    }

    public void setPrintedSpecimenCount(Integer printedSpecimenCount) {
        this.printedSpecimenCount = printedSpecimenCount;
    }

    public Integer getPrintedBlockCount() {
        return printedBlockCount;
    }

    public void setPrintedBlockCount(Integer printedBlockCount) {
        this.printedBlockCount = printedBlockCount;
    }

    public Integer getPrintedSlideCount() {
        return printedSlideCount;
    }

    public void setPrintedSlideCount(Integer printedSlideCount) {
        this.printedSlideCount = printedSlideCount;
    }

    public Integer getPrintedFreezerCount() {
        return printedFreezerCount;
    }

    public void setPrintedFreezerCount(Integer printedFreezerCount) {
        this.printedFreezerCount = printedFreezerCount;
    }
}
