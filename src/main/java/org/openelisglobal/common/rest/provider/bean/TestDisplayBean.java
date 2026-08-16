package org.openelisglobal.common.rest.provider.bean;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.util.IdValuePair;

/**
 * A test as offered to the rule builders (reflex conditions, calculation
 * operands).
 *
 * <p>
 * {@code resultType} is the test's own type and is kept for callers that still
 * read it, but it is no longer the answer to "can this test be used where a
 * numeric result is required". A test does not have one result type any more:
 * its components do, and they can differ — COVID-19 PCR reports a coded
 * interpretation alongside two numeric Ct values. Asking the test returns the
 * primary component's type, which hid every such test from the numeric search.
 *
 * <p>
 * So the bean carries the components and the distinct types across them.
 * {@code resultTypes} answers whether the test belongs in the search at all;
 * {@code components} answers which of its parts may then be chosen.
 */
public class TestDisplayBean extends IdValuePair {

    String resultType;
    List<IdValuePair> resultList;
    List<ComponentBean> components = new ArrayList<>();
    Set<String> resultTypes = new LinkedHashSet<>();

    public TestDisplayBean(String id, String value, String resultType) {
        super(id, value);
        this.resultType = resultType;
        if (resultType != null) {
            this.resultTypes.add(resultType);
        }
    }

    /** One component of the test, with the result type it actually reports. */
    public static class ComponentBean {
        private String id;
        private String value;
        private String resultType;
        private boolean primary;

        public ComponentBean() {
        }

        public ComponentBean(String id, String value, String resultType, boolean primary) {
            this.id = id;
            this.value = value;
            this.resultType = resultType;
            this.primary = primary;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getResultType() {
            return resultType;
        }

        public void setResultType(String resultType) {
            this.resultType = resultType;
        }

        public boolean isPrimary() {
            return primary;
        }

        public void setPrimary(boolean primary) {
            this.primary = primary;
        }
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    public List<IdValuePair> getResultList() {
        return resultList;
    }

    public void setResultList(List<IdValuePair> resultList) {
        this.resultList = resultList;
    }

    public List<ComponentBean> getComponents() {
        return components;
    }

    /**
     * Replaces the components and recomputes the types the test can satisfy. A
     * component that declares no type of its own reports the test's, which is what
     * a single-component test has always done.
     */
    public void setComponents(List<ComponentBean> components) {
        this.components = components == null ? new ArrayList<>() : components;
        this.resultTypes = new LinkedHashSet<>();
        for (ComponentBean component : this.components) {
            this.resultTypes.add(component.getResultType() == null ? resultType : component.getResultType());
        }
        if (this.resultTypes.isEmpty() && resultType != null) {
            this.resultTypes.add(resultType);
        }
    }

    public Set<String> getResultTypes() {
        return resultTypes;
    }

    public void setResultTypes(Set<String> resultTypes) {
        this.resultTypes = resultTypes;
    }
}
