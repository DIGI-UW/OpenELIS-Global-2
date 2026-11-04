package org.openelisglobal.addon.security;

/** Paths of the add-on mechanism that need their own security rules. */
public final class AddonSecurity {

    /** The internal add-on API. */
    public static final String API_PATH = "/rest/addon-api/**";

    /**
     * Files of add-on UI frames. Open: a sandboxed frame has an opaque origin and
     * its requests carry no session cookie. The files are the package's code, not
     * data, and are only served while the add-on is enabled.
     */
    public static final String UI_ASSET_PATH = "/rest/addons/*/ui/**";

    private AddonSecurity() {
    }
}
