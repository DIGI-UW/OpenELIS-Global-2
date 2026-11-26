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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.userrole.daoimpl;

import java.util.ArrayList;
import java.util.List;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.userrole.dao.UserLabUnitRolesDAO;
import org.openelisglobal.userrole.valueholder.UserLabUnitRoles;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class UserLabUnitRolesDAOImpl extends BaseDAOImpl<UserLabUnitRoles, Integer> implements UserLabUnitRolesDAO {

    private static final String ALL_LAB_UNITS = "AllLabUnits";

    public UserLabUnitRolesDAOImpl() {
        super(UserLabUnitRoles.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IdValuePair> getActiveUsersWithRoleOnLabUnit(String roleId, String labUnitId) {
        List<IdValuePair> users = new ArrayList<>();
        try {
            String sql = "select distinct cast(su.id as varchar), su.first_name, su.last_name " //
                    + "from system_user su " //
                    + "join lab_unit_roles lur on lur.system_user_id = su.id " //
                    + "join lab_unit_role_map m on m.lab_unit_role_map_id = lur.lab_unit_role_map_id " //
                    + "join lab_roles lr on lr.lab_unit_role_map_id = m.lab_unit_role_map_id " //
                    + "where lr.role = :roleId and (m.lab_unit = :labUnitId or m.lab_unit = :allLabUnits) " //
                    + "and su.is_active = 'Y'";
            // Not createNativeQuery(sql, Object[].class): Hibernate's Session
            // overload treats the Class argument as an entity to map results onto,
            // not a scalar/array result type, and throws "Unknown entity:
            // [Ljava.lang.Object;" for Object[].class (caught empirically by the
            // DB-backed UserLabUnitRolesDAOImplIntegrationTest). The untyped
            // overload already returns one Object[] per row for a multi-column
            // select, exactly like UserRoleDAOImpl's native queries elsewhere.
            NativeQuery query = entityManager.unwrap(Session.class).createNativeQuery(sql);
            query.setParameter("roleId", roleId);
            query.setParameter("labUnitId", labUnitId);
            query.setParameter("allLabUnits", ALL_LAB_UNITS);
            for (Object rowObject : query.list()) {
                Object[] row = (Object[]) rowObject;
                String id = (String) row[0];
                String firstName = (String) row[1];
                String lastName = (String) row[2];
                users.add(new IdValuePair(id, lastName + "," + firstName));
            }
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in UserLabUnitRolesDAOImpl getActiveUsersWithRoleOnLabUnit()", e);
        }
        return users;
    }
}
