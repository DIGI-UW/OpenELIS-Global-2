# Manual Validation Checklist - Storage Management Feature

## Overview

This checklist provides a standardized manual testing procedure for the Storage Management feature in OpenELIS Global. Use this checklist to validate functionality before deployment.

## Prerequisites

- [ ] Application is running locally or in test environment
- [ ] Database is populated with test fixtures (or fixtures are available to load)
- [ ] User has appropriate permissions (Technician/Manager/Admin role)
- [ ] Browser developer tools are open for debugging

---

## 1. Storage Dashboard - Basic Functionality

### 1.1 Dashboard Load

- [ ] Navigate to `/Storage` or `/Storage/samples`
- [ ] Dashboard loads without errors (check browser console)
- [ ] All 4 metric cards are visible:
  - [ ] Total Samples
  - [ ] Active Samples
  - [ ] Disposed Samples
  - [ ] Storage Locations
- [ ] Metric values display correctly (may be 0 if no data)

### 1.2 Tab Navigation

- [ ] Click on each tab (Samples, Rooms, Devices, Shelves, Racks)
- [ ] URL updates correctly (e.g., `/Storage/samples`, `/Storage/rooms`)
- [ ] Tab content loads without errors
- [ ] Tables render with headers even if empty
- [ ] No console errors when switching tabs

### 1.3 Data Tables

- [ ] Each tab displays a data table with appropriate columns
- [ ] Table headers are translated (not showing raw keys like "storage.tab.samples")
- [ ] Table rows display data correctly (if fixtures are loaded)
- [ ] Empty state handles gracefully (no errors when no data)

---

## 2. Dynamic Filters

### 2.1 Filter Visibility by Tab

- [ ] **Samples Tab**: Room filter, Device filter, Status filter all visible
- [ ] **Rooms Tab**: Only Status filter visible (Room and Device filters hidden)
- [ ] **Devices Tab**: Room filter and Status filter visible (Device filter hidden)
- [ ] **Shelves Tab**: Room filter, Device filter, Status filter all visible
- [ ] **Racks Tab**: Room filter, Device filter, Status filter all visible

### 2.2 Filter Functionality

- [ ] **Room Filter**:
  - [ ] Dropdown shows all available rooms
  - [ ] Selecting a room filters the table correctly
  - [ ] "All" option clears the filter
- [ ] **Device Filter**:
  - [ ] Dropdown shows all available devices (when visible)
  - [ ] Selecting a device filters the table correctly
  - [ ] "All" option clears the filter
- [ ] **Status Filter**:
  - [ ] Dropdown shows "All", "Active", "Inactive" options
  - [ ] Selecting status filters the table correctly
  - [ ] "All" option clears the filter

### 2.3 Search Functionality

- [ ] Search input is visible on all tabs
- [ ] Typing in search filters table rows in real-time
- [ ] Search works across all columns (name, code, location, etc.)
- [ ] Clearing search shows all results again

### 2.4 Clear Filters Button

- [ ] "Clear Filters" button appears when filters are active
- [ ] Clicking "Clear Filters" resets all filters and search
- [ ] Table shows all data after clearing

---

## 3. Storage Assignment (P1)

### 3.1 Storage Location Selector Widget

- [ ] Navigate to Order Entry workflow (`/SamplePatientEntry`)
- [ ] Storage Location Selector appears in Sample Entry step
- [ ] Widget is visible and functional (not hidden or broken)

### 3.2 Cascading Dropdowns

- [ ] **Room Selection**:
  - [ ] Room dropdown is enabled
  - [ ] Selecting a room populates device dropdown
- [ ] **Device Selection**:
  - [ ] Device dropdown is disabled until room selected
  - [ ] After room selection, device dropdown enables
  - [ ] Selecting a device populates shelf dropdown
- [ ] **Shelf Selection**:
  - [ ] Shelf dropdown is disabled until device selected
  - [ ] After device selection, shelf dropdown enables
  - [ ] Selecting a shelf populates rack dropdown
- [ ] **Rack Selection**:
  - [ ] Rack dropdown is disabled until shelf selected
  - [ ] After shelf selection, rack dropdown enables
  - [ ] Selecting a rack populates position dropdown
- [ ] **Position Selection**:
  - [ ] Position dropdown is disabled until rack selected
  - [ ] After rack selection, position dropdown enables
  - [ ] Can select a position from the list

### 3.3 Hierarchical Path Display

- [ ] After selecting a complete location (Room → Device → Shelf → Rack → Position)
- [ ] Hierarchical path displays correctly (e.g., "MAIN > FRZ01 > Shelf-A > RKR1 > A5")
- [ ] Path format is readable and consistent

### 3.4 Assignment Submission

- [ ] Complete location selection (all 5 levels)
- [ ] Submit the order/sample entry
- [ ] Assignment is saved successfully
- [ ] Sample appears in Storage Dashboard with correct location

---

## 4. Storage Search (P2A)

### 4.1 Sample ID Search

- [ ] Navigate to Storage Dashboard → Samples tab
- [ ] Enter a sample ID in the search input
- [ ] Table filters to show matching samples
- [ ] Location path displays correctly for found samples

### 4.2 Filter by Room

- [ ] Select a room from the Room filter dropdown
- [ ] Table updates to show only samples in that room
- [ ] All displayed samples have location paths containing the room name/code

### 4.3 Filter by Multiple Criteria

- [ ] Select a room filter
- [ ] Select a device filter
- [ ] Table updates to show samples matching both criteria
- [ ] Results are correctly filtered (AND logic)

### 4.4 Clear Filters

- [ ] Apply multiple filters
- [ ] Click "Clear Filters" button
- [ ] All filters reset, table shows all samples

---

## 5. Storage Movement (P2B)

### 5.1 Single Sample Move

- [ ] Navigate to Storage Dashboard → Samples tab
- [ ] Find a sample with an assigned location
- [ ] Click overflow menu (⋮) on sample row
- [ ] **If Move option exists**:
  - [ ] Click "Move" option
  - [ ] Move modal opens
  - [ ] Current location is displayed
  - [ ] Select new target location using Storage Location Selector
  - [ ] Enter reason (optional)
  - [ ] Confirm move
  - [ ] Success notification appears
  - [ ] Sample location updates in table
  - [ ] Previous position is freed (if applicable)
- [ ] **If Move option not implemented**:
  - [ ] Note: This is expected for POC scope - movement may be deferred

### 5.2 Bulk Move

- [ ] Select multiple samples using checkboxes (if implemented)
- [ ] Click "Bulk Move" action
- [ ] **If Bulk Move modal exists**:
  - [ ] Modal opens with selected samples
  - [ ] Select target rack
  - [ ] Auto-assigned positions are displayed
  - [ ] Can manually edit position assignments
  - [ ] Confirm bulk move
  - [ ] Success notification appears
  - [ ] All samples move to new locations
- [ ] **If Bulk Move not implemented**:
  - [ ] Note: This is expected for POC scope - bulk move may be deferred

### 5.3 Occupied Position Prevention

- [ ] Attempt to move a sample to an occupied position
- [ ] **If validation exists**:
  - [ ] Error message displays
  - [ ] Move is prevented
  - [ ] Sample remains in original location
- [ ] **If validation not implemented**:
  - [ ] Note: This validation may be deferred to post-POC

---

## 6. Internationalization (i18n)

### 6.1 Label Translation

- [ ] All UI labels display translated text (not raw keys like "storage.tab.samples")
- [ ] Metric card titles are translated
- [ ] Tab labels are translated
- [ ] Table headers are translated
- [ ] Filter labels are translated
- [ ] Button labels are translated

### 6.2 Language Switching (if multiple languages supported)

- [ ] Switch application language (if feature exists)
- [ ] Storage Dashboard labels update correctly
- [ ] No raw keys visible in any language

---

## 7. Error Handling

### 7.1 Network Errors

- [ ] Disconnect network or stop backend server
- [ ] Navigate to Storage Dashboard
- [ ] Error handling is graceful (no blank screen)
- [ ] Error message displays appropriately

### 7.2 Empty States

- [ ] Navigate to Storage Dashboard with no data
- [ ] Tables render with headers
- [ ] Empty state is handled gracefully
- [ ] No console errors

### 7.3 Invalid Data

- [ ] **If test data with invalid relationships exists**:
  - [ ] Application handles gracefully
  - [ ] No crashes or blank screens
  - [ ] Error messages are user-friendly

---

## 8. Performance

### 8.1 Load Times

- [ ] Dashboard loads within 3 seconds
- [ ] Tab switching is responsive (< 1 second)
- [ ] Filter application is responsive (< 1 second)
- [ ] Search filtering is responsive (real-time or near real-time)

### 8.2 Large Data Sets

- [ ] **If 100+ storage locations exist**:
  - [ ] Dashboard loads without performance issues
  - [ ] Tables render efficiently
  - [ ] Filters work correctly
  - [ ] No browser freezing or slowdown

---

## 9. Browser Compatibility

### 9.1 Chrome/Edge

- [ ] All functionality works correctly
- [ ] No console errors
- [ ] UI renders correctly

### 9.2 Firefox

- [ ] All functionality works correctly
- [ ] No console errors
- [ ] UI renders correctly

### 9.3 Safari (if applicable)

- [ ] All functionality works correctly
- [ ] No console errors
- [ ] UI renders correctly

---

## 10. Accessibility

### 10.1 Keyboard Navigation

- [ ] Can navigate dashboard using Tab key
- [ ] Can select filters using keyboard
- [ ] Can switch tabs using keyboard
- [ ] Focus indicators are visible

### 10.2 Screen Reader Support

- [ ] **If screen reader available**:
  - [ ] Labels are announced correctly
  - [ ] Table structure is understandable
  - [ ] Filter states are announced

---

## 11. Integration Points

### 11.1 Order Entry Integration

- [ ] Storage Location Selector appears in Sample Entry step
- [ ] Assignment is saved when order is submitted
- [ ] Location appears in Storage Dashboard after assignment

### 11.2 FHIR Integration (if applicable)

- [ ] **If FHIR server is running**:
  - [ ] Storage locations are synced to FHIR
  - [ ] FHIR Location resources are created correctly
  - [ ] Hierarchical relationships are correct

---

## 12. Known Limitations / POC Scope

### 12.1 POC Scope Items

- [ ] **Deferred Features** (not in POC scope):
  - [ ] Sample disposal workflow (P3)
  - [ ] Barcode generation (post-POC)
  - [ ] Advanced reporting (post-POC)
  - [ ] Bulk operations (may be partially implemented)

### 12.2 Test Environment Notes

- [ ] Note any missing test data or fixtures
- [ ] Note any known bugs or issues
- [ ] Document any workarounds needed

---

## Test Results Summary

**Date**: **\*\***\_\_\_**\*\***
**Tester**: **\*\***\_\_\_**\*\***
**Environment**: **\*\***\_\_\_**\*\***
**Browser**: **\*\***\_\_\_**\*\***

**Overall Status**: ☐ PASS ☐ FAIL ☐ PARTIAL

**Issues Found**:

1.
2.
3.

**Notes**:
