package org.openelisglobal.addon.packaging;

import java.util.List;

/**
 * A package was refused: unreadable or unsafe zip, or an invalid manifest. (A
 * signature never causes a refusal; see AddonSignature.check.) {@code code} is
 * stable and meant for the client; {@code problems} lists manifest errors in
 * plain English.
 */
public class AddonPackageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final List<String> problems;

    public AddonPackageException(String code, String message) {
        this(code, message, List.of());
    }

    public AddonPackageException(String code, String message, List<String> problems) {
        super(message);
        this.code = code;
        this.problems = List.copyOf(problems);
    }

    public String getCode() {
        return code;
    }

    public List<String> getProblems() {
        return problems;
    }
}
