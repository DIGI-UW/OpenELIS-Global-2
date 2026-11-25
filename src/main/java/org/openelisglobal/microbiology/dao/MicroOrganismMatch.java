package org.openelisglobal.microbiology.dao;

import org.openelisglobal.microbiology.valueholder.MicroOrganism;

/**
 * An organism a search found. {@code formerName} is the older WHONET name that
 * matched, when neither the organism's name nor its code did.
 */
public record MicroOrganismMatch(MicroOrganism organism, String formerName) {
}
