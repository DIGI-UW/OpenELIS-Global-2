package org.openelisglobal.common.validator;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.AbstractBindingResult;

@Component("defaultErrors")
@Scope("prototype")
public class BaseErrors extends AbstractBindingResult {

    /** */
    private static final long serialVersionUID = 959269351827856310L;

    public BaseErrors() {
        super("Non bound errors");
    }

    boolean error = false;

    @Override
    protected Object getActualFieldValue(String arg0) {
        // not bound to actual object
        return null;
    }

    @Override
    public Object getTarget() {
        // not bound to actual object
        return null;
    }
}
