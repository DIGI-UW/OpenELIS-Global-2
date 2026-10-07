package org.openelisglobal.microbiology.form;

import java.util.List;

public record MicroCaseCancellationCommand(List<String> caseIds, String reason) {
}
