package org.openelisglobal.siteinformation.service;

import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.siteinformation.valueholder.SiteInformation;

/**
 * The text an administrator sees describing a site-information setting: the
 * message for its instruction key, else the message for its description key,
 * else the stored description. A key with no message falls through to the next
 * source instead of being shown raw.
 */
public final class SiteInformationInstructions {

    private SiteInformationInstructions() {
    }

    public static String describe(SiteInformation siteInformation) {
        String instruction = MessageUtil.getMessageIfPresent(siteInformation.getInstructionKey());
        if (instruction == null) {
            instruction = MessageUtil.getMessageIfPresent(siteInformation.getDescriptionKey());
        }
        return instruction == null ? siteInformation.getDescription() : instruction;
    }
}
