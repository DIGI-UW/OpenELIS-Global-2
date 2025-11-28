package org.openelisglobal.notebook.dao;

import org.openelisglobal.common.daoimpl.BaseDAOImpl;
import org.openelisglobal.notebook.valueholder.NoteBookSample;
import org.springframework.stereotype.Component;

@Component
public class NoteBookSampleDAOImpl extends BaseDAOImpl<NoteBookSample, Integer> implements NoteBookSampleDAO {

    public NoteBookSampleDAOImpl() {
        super(NoteBookSample.class);
    }
}
