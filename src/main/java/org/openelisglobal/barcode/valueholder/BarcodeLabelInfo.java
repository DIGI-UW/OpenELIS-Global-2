package org.openelisglobal.barcode.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Class for persisting bar code label information in the database
 *
 * @author Caleb
 */
@Setter
@Getter
@DynamicUpdate
@Entity
@Table(name = "barcode_label_info")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class BarcodeLabelInfo extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    // PK
    @Id
    @GeneratedValue(generator = "barcode_label_info_seq_gen")
    @GenericGenerator(name = "barcode_label_info_seq_gen", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @Parameter(name = "sequence_name", value = "barcode_label_info_seq") })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0, nullable = false)
    private String id;
    // Other values to persist

    @Column(name = "numprinted", nullable = false)
    private int numPrinted;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "type", nullable = true)
    private String type;

    public BarcodeLabelInfo() {
        super();
        numPrinted = 0;
    }

    public BarcodeLabelInfo(String code) {
        super();
        this.code = code;
        numPrinted = 0;
        type = parseCodeForType(code);
    }

    public void incrementNumPrinted() {
        ++numPrinted;
    }

    /**
     * Determines the type of label based on the given code
     *
     * @return The type of label this code belongs to
     */
    private final String parseCodeForType(String code) {
        if (code.contains("-")) {
            return "aliquot";
        } else if (code.contains(".")) {
            return "specimen";
        } else {
            return "order";
        }
    }

    /**
     * Get the id (PK of the object in the database)
     *
     * @return PK
     */
    @Override
    public String getId() {
        return id;
    }

    /**
     * Set the id (PK of the object in the database)
     *
     * @param id PK
     */
    @Override
    public void setId(String id) {
        this.id = id;
    }

}
