package org.openelisglobal.addon.dao;

/** Server-side keys of the add-on mechanism, encrypted at rest. */
public interface AddonSecretDAO {

    /** The stored value, or {@code candidate} stored now if there was none. */
    String getOrCreate(String name, String candidate);
}
