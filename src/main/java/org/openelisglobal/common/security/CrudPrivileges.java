package org.openelisglobal.common.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares, on a service <em>interface</em>, the privileges that gate the CRUD
 * it inherits from {@code BaseObjectService}. Read by {@link CrudGate} when
 * {@code BaseObjectService}'s own {@code @PreAuthorize} evaluates.
 *
 * <p>
 * Why an annotation on the interface and not gates on redeclared methods: a
 * method-level {@code @PreAuthorize} that a descendant interface puts on a
 * redeclared CRUD method is resolved against the <em>most specific method</em>,
 * which for an inherited implementation is declared in
 * {@code BaseObjectServiceImpl} — a class whose type hierarchy does not include
 * the descendant interface. Such a gate is not enforced in production; only a
 * JDK-proxy test stub, whose class does implement the interface, makes it
 * appear to work ({@code BaseObjectServiceCrudGateTest} pins this). A
 * type-level annotation is found on the real impl and on every stub alike.
 *
 * <p>
 * An empty value leaves that side as it was before this mechanism existed:
 * ungated. That is a transitional state tracked by
 * {@code InheritedCrudGateCoverageTest}; new services should declare both.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CrudPrivileges {

    /** {@code PRIV_*} authority required for inherited reads; empty = open. */
    String read() default "";

    /** {@code PRIV_*} authority required for inherited writes; empty = open. */
    String write() default "";
}
