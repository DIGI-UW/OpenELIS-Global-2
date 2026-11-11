package org.openelisglobal.fhir.service;

import java.text.ParseException;
import java.util.List;
import org.hl7.fhir.r4.model.Address;
import org.hl7.fhir.r4.model.Annotation;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.dataexchange.fhir.service.TempIdGenerator;
import org.openelisglobal.note.valueholder.Note;
import org.openelisglobal.person.valueholder.Person;

/**
 * Building blocks shared by every FHIR transform: references, identifiers, ids
 * and the person-level name/telecom helpers.
 */
public interface FhirCommonTransformService {

    List<ContactPoint> transformToTelecom(Person person);

    DateType transformToDateElement(String strDate) throws ParseException;

    <T extends BaseObject<?>> T getItemByFhirId(String fhirUuid, BaseObjectService<T, ?> service);

    Annotation transformNoteToAnnotation(Note note);

    boolean setTempIdIfMissing(Resource resource, TempIdGenerator tempIdGenerator);

    Reference createReferenceFor(Resource resource);

    Reference createReferenceFor(ResourceType resourceType, String id);

    String getIdFromLocation(String location);

    Identifier createIdentifier(String system, String value);

    Identifier createFacilityIdentifier();

    void addHumanNameToPerson(HumanName humanName, Person person);

    void addTelecomToPerson(List<ContactPoint> telecoms, Person person);

    /** Copies a FHIR address onto the person's address columns. */
    void addAddressToPerson(Address address, Person person);

    /** Renders the person's address columns as a FHIR address. */
    Address transformToAddress(Person person);
}
