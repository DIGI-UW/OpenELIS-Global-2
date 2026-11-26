package org.openelisglobal.common.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The display lists were replaced by an empty set before being rebuilt, so a
 * page read during a refresh (a test imported, a lab unit saved), or after a
 * refresh that stopped partway, got no list for its type: the home page's lab
 * units answered 500. The rebuilt lists are published only once complete.
 *
 * <p>
 * The test context mocks the service, so the test builds a real one wired to
 * the context's services.
 */
public class DisplayListServiceRefreshTest extends BaseWebContextSensitiveTest {

    @Autowired
    private AutowireCapableBeanFactory beanFactory;

    @Test
    public void aRefreshThatStopsPartwayLeavesThePreviousListsInPlace() {
        DisplayListService lists = new DisplayListService();
        beanFactory.autowireBean(lists);
        lists.refreshLists();
        List<IdValuePair> before = lists.getList(ListType.TEST_SECTION_INACTIVE);
        assertNotNull(before);

        TypeOfSampleService real = (TypeOfSampleService) ReflectionTestUtils.getField(lists, "typeOfSampleService");
        TypeOfSampleService failing = (TypeOfSampleService) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { TypeOfSampleService.class }, (proxy, method, args) -> {
                    if ("getAllTypeOfSamplesSortOrdered".equals(method.getName())) {
                        throw new IllegalStateException("sample types unavailable mid-refresh");
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
        ReflectionTestUtils.setField(lists, "typeOfSampleService", failing);
        try {
            lists.refreshLists();
            fail("the refresh reaches the sample types and stops there");
        } catch (IllegalStateException expected) {
            assertSame("the lab units read after the failed refresh are the previous ones", before,
                    lists.getList(ListType.TEST_SECTION_INACTIVE));
        } finally {
            ReflectionTestUtils.setField(lists, "typeOfSampleService", real);
        }
        lists.refreshLists();
        assertNotNull(lists.getList(ListType.TEST_SECTION_INACTIVE));
    }
}
