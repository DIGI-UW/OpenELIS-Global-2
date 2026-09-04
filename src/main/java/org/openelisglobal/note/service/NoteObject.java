package org.openelisglobal.note.service;

import org.openelisglobal.common.service.CrossDomainService;
import org.openelisglobal.note.service.NoteServiceImpl.BoundTo;

@CrossDomainService(callers = "domain marker interface — implemented by many domain entities")
public interface NoteObject {

    String getTableId();

    String getObjectId();

    BoundTo getBoundTo();
}
