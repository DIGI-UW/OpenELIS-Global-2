/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.common.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import org.openelisglobal.internationalization.MessageUtil;

@MappedSuperclass
public abstract class BaseObject<PK extends Serializable> implements Serializable, Cloneable {

    private static final long serialVersionUID = 1L;

    @Column(name = "last_updated")
    @Version
    private Timestamp lastupdated;

    @Transient
    private Timestamp originalLastupdated;
    @Transient
    private String sysUserId;
    @Transient
    private String nameKey;

    public BaseObject() {
    }

    public abstract PK getId();

    public abstract void setId(PK id);

    // used for audittrail
    @JsonIgnore
    public String getStringId() {
        if (getId() == null) {
            return null;
        } else if (getId().getClass().equals(String.class)) {
            return (String) getId();
        } else if (getId().getClass().equals(Integer.class)) {
            return Integer.toString((Integer) getId());
        } else if (getId().getClass().equals(Long.class)) {
            return Long.toString((Long) getId());
        } else {
            throw new UnsupportedOperationException(
                    "object must override getStringId() as it's id is not a recognizeable type");
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Returns the lastupdated.
     *
     * @return Date
     */
    public Timestamp getLastupdated() {
        return lastupdated;
    }

    /**
     * Sets the lastupdated.
     *
     * @param lastupdated The lastupdated to set
     */
    public void setLastupdated(Timestamp lastupdated) {
        setOriginalLastupdated(lastupdated);
        this.lastupdated = lastupdated;
    }

    /** Convienence method to set the last updated fields. */
    public void setLastupdatedFields() {

        Timestamp ts = new Timestamp(System.currentTimeMillis());

        if (ts.getNanos() == 0) {
            ts.setNanos(1);
            // unsure why we need this, but this is an unnecessary way to get non 0
            // try {
            // // a little past 0 millisecs
            // Thread.sleep(100);
            // } catch (InterruptedException e) {
            // // bugzilla 2154
            // LogEvent.logError(e);
            // }
            //
            // ts = new Timestamp(System.currentTimeMillis());
        }
        setLastupdated(ts);
        // setLastupdatedBy( getSessionContext().getUsername() );
    }

    /**
     * Returns the originalLastupdated.
     *
     * @return Timestamp
     */
    public Timestamp getOriginalLastupdated() {
        return originalLastupdated;
    }

    /**
     * Sets the originalLastupdated.
     *
     * @param originalLastupdated The originalLastupdated to set
     */
    private void setOriginalLastupdated(Timestamp originalLastupdated) {
        if (this.originalLastupdated == null) {
            this.originalLastupdated = originalLastupdated;
        }
    }

    public void resetOriginalLastupdated() {
        originalLastupdated = null;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public String getNameKey() {
        return nameKey;
    }

    /*
     * Gets a localized version of a name. The key is from a database column so not
     * all tables will have them. If the localized name is not found then the
     * derived class will be asked to supply the name via the protected method
     * getDefaultLocaledName()
     */
    @JsonIgnore
    public String getLocalizedName() {
        if (nameKey != null) {
            String localizedName = MessageUtil.getContextualMessage(nameKey.trim());

            if (localizedName == null || localizedName.equals(nameKey.trim())) {
                return getDefaultLocalizedName();
                // return "bo:gln:143:name:" + nameKey;
            } else {
                return localizedName;
            }
        } else {
            return getDefaultLocalizedName();
            // return "bo:gln:149:name:" + nameKey;
        }
    }

    /*
     * Override if there is a name key column in the database for this table/object
     */
    protected String getDefaultLocalizedName() {
        return "unknown";
    }

    /**
     * A useful routine for sorting any (DB) entity object in Global OpenELIS by
     * it's localized name. If it starts with a number, sort by that ("2" comes
     * after "1", not after "10". Try not to do this too often, since your are
     * looking at strings from the resource file, but if you need a list for a UI or
     * a report in local Alphabetical order, this is just the thing to do.
     *
     * @param list
     */
    public static void sortByLocalizedName(List<? extends BaseObject> list) {
        Collections.sort(list, new ComparatorByLocalizedName());
    }

    private static class ComparatorByLocalizedName implements Comparator<BaseObject> {
        // if the string starts with a number, sort by that, otherwise sort
        // using the string.
        @Override
        public int compare(BaseObject o0, BaseObject o1) {
            String ln0 = o0 == null ? null : o0.getLocalizedName();
            String ln1 = o1 == null ? null : o1.getLocalizedName();
            if (ln0 == null) {
                return -1;
            }
            if (ln1 == null) {
                return 1;
            }
            Scanner s0 = new Scanner(ln0);
            Scanner s1 = new Scanner(ln1);
            if (s0.hasNextInt() && s1.hasNextInt()) {
                Integer n0 = s0.nextInt();
                Integer n1 = s1.nextInt();
                return n0.compareTo(n1);
            }
            return ln0.compareTo(ln1);
        }
    }
}
