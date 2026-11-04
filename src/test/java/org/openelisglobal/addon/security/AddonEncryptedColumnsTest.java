package org.openelisglobal.addon.security;

import static org.junit.Assert.assertEquals;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;
import org.openelisglobal.addon.valueholder.Addon;
import org.openelisglobal.addon.valueholder.AddonAsset;
import org.openelisglobal.addon.valueholder.AddonSecret;
import org.openelisglobal.security.converter.EncryptionConverter;

/**
 * Add-on secrets are written by {@link EncryptionConverter}, so they use the
 * same general text encryptor as the other database secrets. Re-keying those
 * secrets works from a fixed list of columns: a new encrypted add-on column has
 * to be added there too (docs/addons/design.md, "Secrets at rest").
 */
public class AddonEncryptedColumnsTest {

    private static final Set<String> ENCRYPTED = Set.of("clinlims.addon.event_secret", "clinlims.addon.secret_config",
            "clinlims.addon_secret.value");

    @Test
    public void addonSecretsAreTheKnownEncryptedColumns() {
        Set<String> found = new TreeSet<>();
        for (Class<?> entity : List.of(Addon.class, AddonAsset.class, AddonSecret.class)) {
            Table table = entity.getAnnotation(Table.class);
            for (Field field : entity.getDeclaredFields()) {
                Convert convert = field.getAnnotation(Convert.class);
                if (convert != null && convert.converter() == EncryptionConverter.class) {
                    found.add(table.schema() + "." + table.name() + "." + field.getAnnotation(Column.class).name());
                }
            }
        }
        assertEquals(
                "an encrypted add-on column was added or removed: update the re-key list and docs/addons/design.md",
                new TreeSet<>(ENCRYPTED), found);
    }
}
