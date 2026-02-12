package org.openelisglobal.analyzer.service;

/**
 * Options for copy mappings operation
 * 
 */
public class CopyOptions {
    private boolean overwriteExisting = true;
    private boolean skipIncompatible = true; // Default: skip incompatible types

    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

    public boolean isSkipIncompatible() {
        return skipIncompatible;
    }

    public void setSkipIncompatible(boolean skipIncompatible) {
        this.skipIncompatible = skipIncompatible;
    }
}
