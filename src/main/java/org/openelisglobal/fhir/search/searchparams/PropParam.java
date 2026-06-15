package org.openelisglobal.fhir.search.searchparams;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PropParam<T> {

    private String propertyName;

    private T param;
}
