package org.openelisglobal.result.service;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class ResultServiceImplStaticStateTest {

    @Test
    public void serviceDoesNotRetainMutableStateAcrossApplicationContexts() {
        List<String> mutableStaticFields = Arrays.stream(ResultServiceImpl.class.getDeclaredFields())
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .filter(field -> !Modifier.isFinal(field.getModifiers())).map(Field::getName).sorted()
                .collect(Collectors.toList());

        assertTrue("Mutable static fields retain collaborators or cached data across test contexts: "
                + mutableStaticFields, mutableStaticFields.isEmpty());
    }
}
