/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */
package org.openelisglobal.image.daoimpl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.image.dao.ImageDAO;
import org.openelisglobal.image.valueholder.Image;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class ImageDAOImpl extends BaseDAOImpl<Image, String> implements ImageDAO {

    public ImageDAOImpl() {
        super(Image.class);
    }

    @Override
    @Transactional(readOnly = true)
    public Image getImageByDescription(String imageDescription) {

        try {
            String sql = "from Image i where i.description = :imageDescription";

            Query<Image> query = entityManager.unwrap(Session.class).createQuery(sql, Image.class);
            query.setParameter("imageDescription", imageDescription);
            return query.uniqueResult();
        } catch (RuntimeException e) {
            LogEvent.logError(e);
            throw new LIMSRuntimeException("Error in Image getImageByDescription()", e);
        }
    }
}
