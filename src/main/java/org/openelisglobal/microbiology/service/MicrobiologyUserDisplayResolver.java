package org.openelisglobal.microbiology.service;

import java.util.Map;
import org.openelisglobal.systemuser.service.SystemUserService;
import org.openelisglobal.systemuser.valueholder.SystemUser;

final class MicrobiologyUserDisplayResolver {

    private MicrobiologyUserDisplayResolver() {
    }

    static String resolve(SystemUserService systemUserService, String userId, Map<String, String> userDisplayById) {
        if (userId == null || userId.isBlank()) {
            return null;
        }
        if (userDisplayById.containsKey(userId)) {
            return userDisplayById.get(userId);
        }
        SystemUser user = systemUserService.getUserById(userId);
        String display = userId;
        if (user != null) {
            String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
            String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
            String fullName = (firstName + " " + lastName).trim();
            display = fullName.isEmpty() ? userId : fullName;
        }
        userDisplayById.put(userId, display);
        return display;
    }
}
