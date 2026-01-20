package org.openelisglobal.test.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.context.ApplicationEvent;

@Getter
public class TestCreatedEvent extends ApplicationEvent {

    private final Test test;
    private final OffsetDateTime createdAt;
    private final boolean isUpdate; // NEW: Distinguish create vs update

    // Default constructor for create operations
    public TestCreatedEvent(Object source, Test test) {
        this(source, test, false);
    }

    // Full constructor for create or update operations
    public TestCreatedEvent(Object source, Test test, boolean isUpdate) {
        super(source);
        this.test = test;
        this.createdAt = OffsetDateTime.now();
        this.isUpdate = isUpdate;
    }
}
