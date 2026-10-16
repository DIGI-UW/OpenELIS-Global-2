package org.openelisglobal.reportconfiguration.service;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.menu.service.MenuService;
import org.openelisglobal.menu.valueholder.Menu;
import org.openelisglobal.reportconfiguration.dao.ReportDAO;
import org.openelisglobal.reportconfiguration.form.ReportConfigurationForm;
import org.openelisglobal.reportconfiguration.valueholder.Report;
import org.openelisglobal.reportconfiguration.valueholder.ReportCategory;
import org.openelisglobal.siteinformation.service.SiteInformationService;

@RunWith(MockitoJUnitRunner.class)
public class ReportServiceImplTest {

    @Mock
    private ReportDAO reportDAO;

    @Mock
    private MenuService menuService;

    @Mock
    private SiteInformationService siteInformationService;

    @InjectMocks
    private ReportServiceImpl reportService;

    private static final String REPORT_ID = "1";
    private static final String OLD_CATEGORY_ID = "oldCat";
    private static final String NEW_CATEGORY_ID = "newCat";
    private static final String REPORT_MENU_ELEMENT_ID = "reportMenuElementId";
    private static final String NEW_CATEGORY_MENU_ELEMENT_ID = "newCategoryMenuElementId";

    private Report existingReport;
    private Menu reportMenu;
    private Menu newCategoryMenu;

    @Before
    public void setUp() {
        existingReport = new Report();
        existingReport.setId(REPORT_ID);
        existingReport.setName("Existing Report");
        existingReport.setCategory(OLD_CATEGORY_ID);
        existingReport.setMenuElementId(REPORT_MENU_ELEMENT_ID);
        existingReport.setIsVisible(true);

        reportMenu = new Menu();
        reportMenu.setElementId(REPORT_MENU_ELEMENT_ID);

        newCategoryMenu = new Menu();
        newCategoryMenu.setElementId(NEW_CATEGORY_MENU_ELEMENT_ID);

        // Backing store for baseObjectDAO.get(id) so both the initial fetch
        // and the trailing sort-order loop resolve consistently.
        when(reportDAO.get(REPORT_ID)).thenReturn(Optional.of(existingReport));
        when(menuService.getMenuByElementId(REPORT_MENU_ELEMENT_ID)).thenReturn(reportMenu);
    }

    private ReportConfigurationForm buildForm(String newCategoryId) {
        ReportConfigurationForm form = new ReportConfigurationForm();

        Report formReport = new Report();
        formReport.setId(REPORT_ID);
        formReport.setName("Updated Report Name");
        formReport.setCategory(newCategoryId);
        formReport.setIsVisible(true);
        form.setCurrentReport(formReport);

        form.setIdOrder(REPORT_ID);

        ReportCategory reportCategory = new ReportCategory(NEW_CATEGORY_ID, "New Category",
                NEW_CATEGORY_MENU_ELEMENT_ID, "1", null);
        form.setReportCategoryList(Arrays.asList(reportCategory));

        return form;
    }

    @Test
    public void updateReport_whenCategoryChanges_reassignsMenuToNewCategoryParent() {
        when(menuService.getMenuByElementId(NEW_CATEGORY_MENU_ELEMENT_ID)).thenReturn(newCategoryMenu);

        ReportConfigurationForm form = buildForm(NEW_CATEGORY_ID);

        boolean result = reportService.updateReport(form, "testUser");

        assertTrue("updateReport should complete without error", result);
        // reportMenu is saved twice: once by the category-reassignment branch,
        // and once more by the trailing sort-order/visibility loop that runs
        // for every report regardless of category change.
        verify(menuService, times(2)).save(eq(reportMenu));
        assertSame("reportMenu should now be parented under the new category menu",
        newCategoryMenu, reportMenu.getParent());
    }

    @Test
    public void updateReport_whenCategoryUnchanged_doesNotReassignMenuParent() {
        // Form submits the same category the report already has.
        ReportConfigurationForm form = buildForm(new String(OLD_CATEGORY_ID));
        // Match the "no category" lookup: reportCategoryList only contains
        // NEW_CATEGORY_ID, so no ReportCategory matches OLD_CATEGORY_ID -
        // fine, since the category-changed branch should not run at all.

        boolean result = reportService.updateReport(form, "testUser");

        assertTrue("updateReport should complete without error", result);
        // reportMenu is still saved once by the trailing sort-order loop
        // (which runs unconditionally), but never re-parented, since the
        // category-reassignment branch should not have run.
        verify(menuService, times(1)).save(eq(reportMenu));
        assertNull("reportMenu parent should remain unset", reportMenu.getParent());
    }
}
