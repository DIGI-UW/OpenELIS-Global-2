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
package org.openelisglobal.dataexchange.aggregatereporting.valueholder;

import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

public class ReportExternalExport extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private Timestamp eventDate;
    private Timestamp sentDate;
    private Timestamp collectionDate;
    private String typeId;
    private boolean send;
    private String data;
    private String bookkeepingData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public Timestamp getSentDate() {
        return sentDate;
    }

    public void setSentDate(Timestamp sentDate) {
        this.sentDate = sentDate;
    }

    public Timestamp getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(Timestamp collectionDate) {
        this.collectionDate = collectionDate;
    }

    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    public boolean isSend() {
        return send;
    }

    public void setSend(boolean sent) {
        this.send = sent;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getBookkeepingData() {
        return bookkeepingData;
    }

    public void setBookkeepingData(String bookkeepingData) {
        this.bookkeepingData = bookkeepingData;
    }

    public int getDataSize() {
        return data.length();
    }
}
