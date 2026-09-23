package org.openelisglobal.inventory.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * A directory entry for one tag: the little state a free-form string cannot
 * carry on its own.
 *
 * <p>
 * This is deliberately not the managed item-type entity the redesign removed.
 * An item's tags are the strings on the item, no behaviour hangs off a row
 * here, and a tag with no row at all is an ordinary tag in good standing. A row
 * appears for exactly two reasons: the tag has been deactivated, so it should
 * stop being suggested; or it was created here before anything carried it.
 *
 * <p>
 * Deactivating is not deleting. Items already carrying the tag keep it and go
 * on showing it, which is what lets a lab retire {@code gloves} in favour of
 * {@code glove} without rewriting what it recorded last year.
 */
@Getter
@Setter
@Entity
@Access(AccessType.FIELD)
@Table(name = "inventory_tag")
public class InventoryTag extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_tag_generator")
    @SequenceGenerator(name = "inventory_tag_generator", sequenceName = "inventory_tag_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    /** Spelled exactly as it is stored on the items carrying it. */
    @Column(name = "name", nullable = false, length = 255)
    @NotNull
    @Size(min = 1, max = 255)
    private String name;

    @Column(name = "is_active", length = 1, nullable = false)
    private String isActive = "Y";

    @JsonIgnore
    public boolean isActive() {
        return "Y".equals(isActive);
    }
}
