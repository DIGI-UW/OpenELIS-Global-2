package org.openelisglobal.biorepository.controller.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Single lifecycle event for a biorepository sample (storage, transfer, retrieval
 * flow). Populated from persisted domain data only.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BioSampleLifecycleEventDTO {

    private String eventType;
    /** ISO-8601 instant string (UTC). */
    private String occurredAt;
    private String actor;
    private String sourceLocation;
    private String destinationLocation;
    private String statusOrNotes;

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(String occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getSourceLocation() {
        return sourceLocation;
    }

    public void setSourceLocation(String sourceLocation) {
        this.sourceLocation = sourceLocation;
    }

    public String getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(String destinationLocation) {
        this.destinationLocation = destinationLocation;
    }

    public String getStatusOrNotes() {
        return statusOrNotes;
    }

    public void setStatusOrNotes(String statusOrNotes) {
        this.statusOrNotes = statusOrNotes;
    }
}
