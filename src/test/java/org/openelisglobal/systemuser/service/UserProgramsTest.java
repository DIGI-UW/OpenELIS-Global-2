package org.openelisglobal.systemuser.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.program.service.ProgramService;
import org.openelisglobal.program.valueholder.Program;
import org.openelisglobal.role.service.RoleService;
import org.openelisglobal.role.valueholder.Role;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1222: the program list is cached at start-up and rebuilt only when a
 * program is saved, so a program deleted straight from the database lingers in
 * it. Resolving that dead id must drop the entry, not throw: the exception
 * became a 500 on /rest/user-programs and blanked every order entry lane.
 * <p>
 * The lookup is {@code getMatch}, not {@code get}: the latter raises
 * ObjectNotFoundException for a missing row rather than returning null, which
 * is what made a null check alone insufficient here.
 */
public class UserProgramsTest {

    private UserServiceImpl userService;
    private ProgramService programService;
    private DisplayListService displayList;
    private DisplayListService previousInstance;

    @Before
    public void setUp() throws Exception {
        userService = new UserServiceImpl();
        programService = mock(ProgramService.class);
        RoleService roleService = mock(RoleService.class);
        Role role = new Role();
        role.setId("1");
        when(roleService.getRoleByName(any())).thenReturn(role);

        ReflectionTestUtils.setField(userService, "programService", programService);
        ReflectionTestUtils.setField(userService, "roleService", roleService);

        displayList = mock(DisplayListService.class);
        previousInstance = DisplayListService.getInstance();
        setDisplayListInstance(displayList);
    }

    @After
    public void tearDown() throws Exception {
        setDisplayListInstance(previousInstance);
    }

    @Test
    public void programDeletedFromTheDatabase_isDroppedRatherThanDereferenced() {
        when(displayList.getList(ListType.PROGRAM))
                .thenReturn(List.of(new IdValuePair("2", "Routine Testing"), new IdValuePair("9", "Water Testing")));
        when(programService.getMatch("id", "2")).thenReturn(Optional.of(programWithNoSection("2")));
        when(programService.getMatch("id", "9")).thenReturn(Optional.empty());

        List<IdValuePair> programs = userService.getUserPrograms("17", "Reception");

        assertEquals("the stale id must not appear", 1, programs.size());
        assertEquals("2", programs.get(0).getId());
    }

    @Test
    public void everyProgramGone_returnsAnEmptyListRatherThanFailing() {
        when(displayList.getList(ListType.PROGRAM)).thenReturn(List.of(new IdValuePair("9", "Water Testing")));
        when(programService.getMatch("id", "9")).thenReturn(Optional.empty());

        assertEquals(0, userService.getUserPrograms("17", "Reception").size());
    }

    @Test
    public void programsWithoutATestSection_areKept() {
        when(displayList.getList(ListType.PROGRAM)).thenReturn(List.of(new IdValuePair("2", "Routine Testing")));
        when(programService.getMatch("id", "2")).thenReturn(Optional.of(programWithNoSection("2")));

        assertEquals(1, userService.getUserPrograms("17", "Reception").size());
    }

    private Program programWithNoSection(String id) {
        Program program = new Program();
        program.setId(id);
        program.setTestSection(null);
        return program;
    }

    private void setDisplayListInstance(DisplayListService value) throws Exception {
        Field field = DisplayListService.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, value);
    }
}
