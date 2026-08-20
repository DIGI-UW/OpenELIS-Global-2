package org.openelisglobal.textmacro.service;

public class TextMacroRequestException extends IllegalArgumentException {

    private final String code;

    public TextMacroRequestException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
