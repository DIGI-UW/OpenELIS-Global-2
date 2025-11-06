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
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.menu.daoimpl;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.menu.dao.MenuDAO;
import org.openelisglobal.menu.valueholder.Menu;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class MenuDAOImpl extends BaseDAOImpl<Menu, String> implements MenuDAO {

    public MenuDAOImpl() {
        super(Menu.class);
    }

    //
    // @Override
    // public List<Menu> getAllMenus() throws LIMSRuntimeException {
    //
    // try {
    // String sql = "from Menu";
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // List<Menu> menus = query.list();
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    //
    // return menus;
    // } catch (HibernateException e) {
    // LogEvent.logError("MenuDAOImpl", "getAllMenus()", e.toString());
    // throw new LIMSRuntimeException("Error in Menu getAllMenus()", e);
    // }
    // }

    //
    // @Override
    // public List<Menu> getAllActiveMenus() throws LIMSRuntimeException {
    //
    // try {
    // String sql = "from Menu m where m.isActive = true";
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // List<Menu> menus = query.list();
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    //
    // return menus;
    // } catch (HibernateException e) {
    // LogEvent.logError("MenuDAOImpl", "getAllActiveMenus()", e.toString());
    // throw new LIMSRuntimeException("Error in Menu getAllActiveMenus()", e);
    // }
    // }

    // @Override
    // public Menu getMenuByElementId(String elementId) throws LIMSRuntimeException
    // {
    // String sql = "From Menu m where m.elementId = :elementId";
    // try {
    // Query query = entityManager.unwrap(Session.class).createQuery(sql);
    // query.setString("elementId", elementId);
    // Menu menu = (Menu) query.uniqueResult();
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // return menu;
    // } catch (HibernateException e) {
    // LogEvent.logError("MenuDAOImpl", "getMenuByElementId()", e.toString());
    // throw new LIMSRuntimeException("Error in Menu getMenuByElementId()", e);
    // }
    //
    // }

    // @Override
    // public void updateData(Menu menu) throws LIMSRuntimeException {
    // try {
    // entityManager.unwrap(Session.class).merge(menu);
    // // entityManager.unwrap(Session.class).flush(); // CSL remove old
    // // entityManager.unwrap(Session.class).clear(); // CSL remove old
    // // entityManager.unwrap(Session.class).evict // CSL remove old(menu);
    // // entityManager.unwrap(Session.class).refresh // CSL remove old(menu);
    // } catch (RuntimeException e) {
    //
    // LogEvent.logError("Menu.DAOImpl", "updateData()", e.toString());
    // throw new LIMSRuntimeException("Error in Menu updateData()", e);
    // }
    // }

}
