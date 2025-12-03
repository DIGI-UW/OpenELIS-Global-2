package org.openelisglobal.test.event;

import java.time.OffsetDateTime;
import lombok.Getter;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.context.ApplicationEvent;

@Getter
public class TestCreatedEvent extends ApplicationEvent {

    private final Test test;
    private final OffsetDateTime createdAt;

    public TestCreatedEvent(Object source, Test test) {
        super(source);
        this.test = test;
        this.createdAt = OffsetDateTime.now();
    }
}
