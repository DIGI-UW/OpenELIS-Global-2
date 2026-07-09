package org.openelisglobal.barcode.labeltype;

import java.util.ArrayList;
import org.openelisglobal.barcode.LabelField;
import org.openelisglobal.internationalization.MessageUtil;

/**
 * Stores Formatting for a Blank Order label
 *
 * @author Caleb
 */
public class BlankLabel extends Label {

    public BlankLabel(String code) {
        aboveFields = new ArrayList<LabelField>();
        LabelField field;

        field = new LabelField(MessageUtil.getMessage("barcode.label.info.patientName"), "", 12);
        field.setDisplayFieldName(true);
        field.setUnderline(true);
        aboveFields.add(field);
        field = new LabelField(MessageUtil.getMessage("barcode.label.info.patientdob"), "", 8);
        field.setDisplayFieldName(true);
        field.setUnderline(true);
        aboveFields.add(field);
        field = new LabelField(MessageUtil.getMessage("barcode.label.info.patientId"), "", 10);
        field.setDisplayFieldName(true);
        field.setUnderline(true);
        aboveFields.add(field);
        field = new LabelField(MessageUtil.getMessage("barcode.label.info.site"), "", 10);
        field.setDisplayFieldName(true);
        field.setUnderline(true);
        aboveFields.add(field);

        setCode(code);
    }

    @Override
    public int getNumTextRowsBefore() {
        Iterable<LabelField> fields = getAboveFields();
        return getNumRows(fields);
    }

    @Override
    public int getNumTextRowsAfter() {
        return 0;
    }

    @Override
    public int getMaxNumLabels() {
        return 10;
    }
}
