package org.openelisglobal.notification.service.recipient;

import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.notification.service.NotificationContext;
import org.openelisglobal.notification.valueholder.NotificationRecipientType;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.valueholder.Provider;
import org.openelisglobal.referral.valueholder.ReferralSubcontract;
import org.openelisglobal.samplehuman.service.SampleHumanService;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class NotificationRecipientResolver {

    @Autowired
    private SampleHumanService sampleHumanService;

    @Autowired
    private SystemUserService systemUserService;

    public Recipient resolve(NotificationRecipientType type, NotificationContext ctx) {
        if (type == null || ctx == null) {
            return null;
        }
        try {
            return switch (type) {
            case ORDERING_PROVIDER -> resolveOrderingProvider(ctx);
            case SUBMITTING_USER -> resolveSubmittingUser(ctx);
            case COC_CONTACT -> resolveCocContact(ctx);
            };
        } catch (RuntimeException e) {
            LogEvent.logError(this.getClass().getSimpleName(), "resolve",
                    "failed to resolve recipient " + type + " for event " + ctx.getEventCode());
            LogEvent.logError(e);
            return null;
        }
    }

    private Recipient resolveOrderingProvider(NotificationContext ctx) {
        if (ctx.getSample() == null) {
            return null;
        }
        Provider provider = sampleHumanService.getProviderForSample(ctx.getSample());
        if (provider == null || provider.getPerson() == null) {
            return null;
        }
        Person person = provider.getPerson();
        return new Recipient(displayName(person), person.getEmail(), primaryPhone(person));
    }

    private Recipient resolveCocContact(NotificationContext ctx) {
        if (ctx.getReferral() == null || ctx.getReferral().getSubcontract() == null) {
            return null;
        }
        ReferralSubcontract sub = ctx.getReferral().getSubcontract();
        String name = sub.getCocContactName();
        return new Recipient(name == null ? "" : name, sub.getCocContactEmail(), sub.getCocContactPhone());
    }

    private Recipient resolveSubmittingUser(NotificationContext ctx) {
        String userId = ctx.getSubmittingUserId();
        if (GenericValidator.isBlankOrNull(userId)) {
            return null;
        }
        SystemUser user = systemUserService.getUserById(userId);
        if (user == null) {
            return null;
        }
        // SystemUser doesn't carry direct contact info today — name resolves, contact
        // channels
        // may be null. The dispatcher logs and skips for any channel the recipient
        // can't receive.
        String display = composeName(user.getFirstName(), user.getLastName());
        return new Recipient(display, null, null);
    }

    private String displayName(Person person) {
        return composeName(person.getFirstName(), person.getLastName());
    }

    private String composeName(String first, String last) {
        StringBuilder sb = new StringBuilder();
        if (!GenericValidator.isBlankOrNull(first)) {
            sb.append(first.trim());
        }
        if (!GenericValidator.isBlankOrNull(last)) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(last.trim());
        }
        return sb.toString();
    }

    private String primaryPhone(Person person) {
        if (!GenericValidator.isBlankOrNull(person.getPrimaryPhone())) {
            return person.getPrimaryPhone();
        }
        if (!GenericValidator.isBlankOrNull(person.getCellPhone())) {
            return person.getCellPhone();
        }
        if (!GenericValidator.isBlankOrNull(person.getWorkPhone())) {
            return person.getWorkPhone();
        }
        return null;
    }
}
