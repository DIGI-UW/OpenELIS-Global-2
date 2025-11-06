package org.openelisglobal.common.propertyeditor;

import java.beans.PropertyEditorSupport;

public class CaseInsensitiveEnumPropertyEditor<T extends Enum<?>> extends PropertyEditorSupport {

    private final Class<T> enumClass;

    public CaseInsensitiveEnumPropertyEditor(Class<T> enumClass) {
        super();
        this.enumClass = enumClass;
    }

    @Override
    public void setAsText(final String text) throws IllegalArgumentException {
        for (T enumConstant : enumClass.getEnumConstants()) {
            if (enumConstant.name().compareToIgnoreCase(text) == 0) {
                setValue(enumConstant);
            }
        }
    }
}
