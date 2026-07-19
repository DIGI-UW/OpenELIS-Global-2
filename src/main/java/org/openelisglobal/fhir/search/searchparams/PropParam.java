package org.openelisglobal.fhir.search.searchparams;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Associates a FHIR search parameter with the corresponding OpenELIS entity
 * property.
 *
 * @param <T> HAPI FHIR search parameter type
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class PropParam<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Entity property path.
     *
     * Examples:
     * 
     * <pre>
     * person.firstName
     * person.lastName
     * person.city
     * fhirUuid
     * lastUpdated
     * </pre>
     */
    private String propertyName;

    /**
     * Original HAPI FHIR search parameter.
     */
    private T param;
}
