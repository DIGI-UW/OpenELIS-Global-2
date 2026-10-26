package org.openelisglobal.qaevent.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.qaevent.valueholder.NceCategory;
import org.openelisglobal.qaevent.valueholder.NceType;

/**
 * OGC-1507 section 8: an existing NCE row is linked to its shipped key by its
 * English name, then by the alias list of older spellings; a key another row
 * already holds is not given twice; a row that matches nothing keeps its key.
 */
@RunWith(MockitoJUnitRunner.class)
public class NceDisplayKeyLinkerTest {

    @Mock
    private NceCategoryService nceCategoryService;
    @Mock
    private NceTypeService nceTypeService;
    @InjectMocks
    private NceDisplayKeyLinker linker;

    @Test
    public void linksByNameThenAliasAndReportsWhatItDid() {
        NceType csvKey = type(1, "Sample collection error", "nce.type.sampleCollectionError");
        NceType upperCase = type(2, "EQUIPMENT FAILURE", null);
        NceType alias = type(3, "Haemolytic sample", null);
        NceType linked = type(4, "Barcode Issue", "nce.type.barcodeIssue");
        NceType siteOwn = type(5, "Ward courier lost it", null);
        when(nceTypeService.getAllNceTypes()).thenReturn(List.of(csvKey, upperCase, alias, linked, siteOwn));
        when(nceCategoryService.getAllNceCategories())
                .thenReturn(List.of(category(1, "Post analytical", null), category(2, "General", null)));

        Map<String, NceDisplayKeyLinker.Report> reports = linker.link();

        assertEquals("nce.type.sample_collection_error", csvKey.getDisplayKey());
        assertEquals("nce.type.equipment_failure", upperCase.getDisplayKey());
        assertEquals("nce.type.haemolyticSample", alias.getDisplayKey());
        assertEquals("nce.type.barcodeIssue", linked.getDisplayKey());
        assertEquals(null, siteOwn.getDisplayKey());
        NceDisplayKeyLinker.Report types = reports.get("NCE types");
        assertEquals(2, types.linkedByName);
        assertEquals(1, types.linkedByAlias);
        assertEquals(1, types.alreadyLinked);
        assertEquals(List.of("Ward courier lost it"), types.unmatched);
        NceDisplayKeyLinker.Report categories = reports.get("NCE categories");
        assertEquals(1, categories.linkedByName);
        assertEquals(1, categories.linkedByAlias);
        verify(nceTypeService, never()).update(linked);
        verify(nceTypeService, never()).update(siteOwn);
    }

    @Test
    public void aKeyAnotherRowHoldsIsNotGivenTwice() {
        NceType holder = type(1, "Hemolytic sample", "nce.type.haemolyticSample");
        NceType duplicate = type(2, "Haemolytic sample", null);
        when(nceTypeService.getAllNceTypes()).thenReturn(List.of(holder, duplicate));
        when(nceCategoryService.getAllNceCategories()).thenReturn(List.of());

        NceDisplayKeyLinker.Report types = linker.link().get("NCE types");

        assertEquals(null, duplicate.getDisplayKey());
        assertEquals(List.of("Haemolytic sample"), types.unmatched);
        verify(nceTypeService, never()).update(any());
    }

    private static NceType type(int id, String name, String key) {
        NceType type = new NceType();
        type.setId(id);
        type.setName(name);
        type.setDisplayKey(key);
        return type;
    }

    private static NceCategory category(int id, String name, String key) {
        NceCategory category = new NceCategory();
        category.setId(id);
        category.setName(name);
        category.setDisplayKey(key);
        return category;
    }
}
