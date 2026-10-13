package org.openelisglobal.fhir.service;

import java.io.InputStream;
import org.fhir.ucum.UcumEssenceService;
import org.fhir.ucum.UcumException;
import org.openelisglobal.common.log.LogEvent;

/**
 * Whether a unit name is a valid UCUM code. Unit-of-measure names are typed by
 * the lab ("10^3/µl", "tubes"), so a FHIR Quantity carries the UCUM system and
 * code only for the names UCUM accepts; the others go as text.
 */
public final class UcumUnits {

    public static final String SYSTEM = "http://unitsofmeasure.org";

    private UcumUnits() {
    }

    public static boolean isValid(String unit) {
        UcumEssenceService ucum = Holder.UCUM;
        return ucum != null && unit != null && !unit.isBlank() && ucum.validate(unit) == null;
    }

    private static final class Holder {
        private static final UcumEssenceService UCUM = load();

        private static UcumEssenceService load() {
            try (InputStream essence = UcumUnits.class.getResourceAsStream("/ucum-essence.xml")) {
                return essence == null ? null : new UcumEssenceService(essence);
            } catch (UcumException | java.io.IOException e) {
                LogEvent.logError(UcumUnits.class.getSimpleName(), "load",
                        "UCUM definitions could not be read, units go as text: " + e.getMessage());
                return null;
            }
        }
    }
}
