package org.openelisglobal.storage.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openelisglobal.storage.service.SampleStorageService;

/**
 * OGC-1266 M4: the storage location, its notes and a later change of either
 * travel with the order save, and an item that has never been stored is stored
 * with its notes rather than "moved" from nowhere.
 */
@RunWith(MockitoJUnitRunner.class)
public class SampleStorageAssignmentListenerTest {

    @Mock
    private SampleStorageService sampleStorageService;

    @InjectMocks
    private SampleStorageAssignmentListener listener;

    // Found on the live walk: the location read-back of an unassigned item is
    // not empty (it carries the quantity snapshot), and the save treated the
    // first assignment as a move, losing the notes to the move's reason.
    @Test
    public void anItemNeverStoredIsAssignedWithItsNotes() {
        Map<String, Object> unassigned = new HashMap<>();
        unassigned.put("sampleItemId", "364");
        unassigned.put("location", "");
        unassigned.put("hierarchicalPath", "");
        unassigned.put("quantityRemaining", "5");
        when(sampleStorageService.getSampleItemLocation("364")).thenReturn(unassigned);

        listener.applyStorage("364", "1", "device", "", "Keep cold");

        verify(sampleStorageService).assignSampleItemWithLocation("364", "1", "device", "", "Keep cold");
        verify(sampleStorageService, never()).moveSampleItemWithLocation(anyString(), anyString(), anyString(), any(),
                anyString(), any());
    }

    @Test
    public void anItemNeverStoredAndWithoutNotesRecordsTheDefaultNote() {
        when(sampleStorageService.getSampleItemLocation("364")).thenReturn(new HashMap<>());

        listener.applyStorage("364", "1", "device", "", null);

        verify(sampleStorageService).assignSampleItemWithLocation("364", "1", "device", "",
                "Auto-assigned on order creation");
    }

    @Test
    public void aSaveThatRepeatsTheLocationOnlyUpdatesTheNotes() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("locationId", "1");
        existing.put("locationType", "device");
        existing.put("positionCoordinate", "");
        existing.put("notes", "Old note");
        when(sampleStorageService.getSampleItemLocation("364")).thenReturn(existing);

        listener.applyStorage("364", "1", "device", "", "Keep cold");

        verify(sampleStorageService).updateAssignmentMetadata("364", "", "Keep cold");
        verify(sampleStorageService, never()).moveSampleItemWithLocation(anyString(), anyString(), anyString(), any(),
                anyString(), any());
        verify(sampleStorageService, never()).assignSampleItemWithLocation(anyString(), anyString(), anyString(), any(),
                any());
    }

    @Test
    public void aSaveThatRepeatsTheLocationAndTheNotesWritesNothing() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("locationId", "1");
        existing.put("locationType", "device");
        existing.put("positionCoordinate", "");
        existing.put("notes", "Keep cold");
        when(sampleStorageService.getSampleItemLocation("364")).thenReturn(existing);

        listener.applyStorage("364", "1", "device", "", "Keep cold");

        verify(sampleStorageService, never()).updateAssignmentMetadata(anyString(), any(), any());
        verify(sampleStorageService, never()).moveSampleItemWithLocation(anyString(), anyString(), anyString(), any(),
                anyString(), any());
    }

    @Test
    public void aSaveThatChangesTheLocationMovesTheItemAndKeepsTheNotes() {
        Map<String, Object> existing = new HashMap<>();
        existing.put("locationId", "7");
        existing.put("locationType", "shelf");
        existing.put("positionCoordinate", "");
        existing.put("notes", "Keep cold");
        when(sampleStorageService.getSampleItemLocation("364")).thenReturn(existing);

        listener.applyStorage("364", "1", "device", "", "Keep cold");

        verify(sampleStorageService).moveSampleItemWithLocation(eq("364"), eq("1"), eq("device"), eq(""), anyString(),
                eq("Keep cold"));
    }
}
