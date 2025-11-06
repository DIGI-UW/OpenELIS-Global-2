package org.openelisglobal.note.service;

import org.openelisglobal.note.service.NoteServiceImpl.BoundTo;

public interface NoteObject {

    String getTableId();

    String getObjectId();

    BoundTo getBoundTo();
}
