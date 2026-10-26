package org.openelisglobal.microbiology.service;

import java.lang.annotation.*;

/**
 * The service checks rights under all case locks itself, in its own
 * transaction.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ManagesCaseLocks {
}
