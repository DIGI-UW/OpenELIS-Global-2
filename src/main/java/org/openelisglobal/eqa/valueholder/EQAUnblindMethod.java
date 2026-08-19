package org.openelisglobal.eqa.valueholder;

/**
 * How an in-house panel was unblinded (FR-V2.4-10). Recorded separately from
 * the acting user, because the scheduled path also writes a real user id and
 * the audit has to distinguish the two.
 */
public enum EQAUnblindMethod {
    SCHEDULED, MANUAL
}
