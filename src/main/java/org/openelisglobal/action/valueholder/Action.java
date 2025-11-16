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
package org.openelisglobal.action.valueholder;

import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.BaseObject;

public class Action extends BaseObject<String> {

    private String code;

    private String id;

    private String description;

    private String type;

    // (concatenate action code name/desc)
    private String actionDisplayValue;

    public Action() {
        super();
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getActionDisplayValue() {
        if (!StringUtil.isNullorNill(this.code)) {
            actionDisplayValue = code + "-" + description;
        } else {
            actionDisplayValue = description;
        }
        return actionDisplayValue;
    }
}
