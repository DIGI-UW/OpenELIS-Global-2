package org.openelisglobal.inventory.valueholder;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.localization.valueholder.Localization;

/**
 * OGC-658 Part A: admin-managed, localized replacement for the hardcoded
 * {@code InventoryEnums.ItemType} enum. {@code code} is the stable identifier
 * stored on {@code inventory_item.item_type}; the display name is translated
 * per-locale via the shared {@link Localization} mechanism (same pattern as
 * {@code Dictionary.localizedDictionaryName}).
 */
@Getter
@Setter
@Entity
@Table(name = "inventory_item_type")
@Access(AccessType.FIELD)
public class InventoryItemType extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "inventory_item_type_generator")
    @SequenceGenerator(name = "inventory_item_type_generator", sequenceName = "inventory_item_type_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    @NotNull
    @Size(min = 1, max = 50)
    private String code;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "name_localization_id", nullable = false)
    private Localization nameLocalization;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_seeded", nullable = false)
    private Boolean isSeeded = false;

    /** Localized display name for the current request locale. */
    public String getLabel() {
        return nameLocalization != null ? nameLocalization.getLocalizedValue() : code;
    }
}
