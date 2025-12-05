# Debug Guide: Page Reload Sidenav State Issues

## The Problem

On page reload, the sidenav sometimes gets stuck open and content is hidden behind it.

**Expected Behavior:**
1. **Storage pages** (`/Storage/*`, `/FreezerMonitoring/*`):
   - If user has NO saved preference → default to **LOCK** mode (nav open, content pushed)
   - If user previously set preference → use that preference
2. **Other pages** (`/Dashboard`, `/Results`, etc.):
   - If user has NO saved preference → default to **CLOSE** mode (nav collapsed)
   - If user previously set preference → use that preference

**Problem Behavior:**
- Nav might be visually open but content is NOT pushed (content hidden behind nav)
- OR nav state doesn't match what user previously selected
- OR default mode not being applied correctly

---

## How to Debug (Manual Testing)

### Step 1: Open Browser Console (F12)

All debug logs are prefixed with:
- `[Layout]` - Route changes and sidenav state
- `[useSideNavPreference]` - State initialization and persistence
- `[SideNav]` - Menu rendering and click events

### Step 2: Test Scenarios

#### Scenario A: First Visit (No Saved Preference)

1. **Clear localStorage** (important!):
   ```javascript
   localStorage.removeItem('storageSideNavMode');
   localStorage.removeItem('mainSideNavMode');
   ```

2. **Navigate to Storage page** (`/Storage/StorageManagement`)
   
   **Expected Console Output:**
   ```
   [Layout] Route changed: { pathname: '/Storage/StorageManagement', isStorageContext: true, ... }
   [useSideNavPreference] Reading from localStorage: { storageKey: 'storageSideNavMode', savedValue: null, ... }
   [useSideNavPreference] Using defaultMode (no saved value): lock
   [Layout] Sidenav state: { mode: 'lock', isExpanded: true, isLocked: true, contentClass: 'content-nav-locked' }
   ```

   **Expected Visual:**
   - ✅ Sidenav OPEN
   - ✅ Content PUSHED to the right
   - ✅ No overlap

3. **Navigate to Home/Dashboard** (`/Dashboard`)
   
   **Expected Console Output:**
   ```
   [Layout] Route changed: { pathname: '/Dashboard', isStorageContext: false, ... }
   [useSideNavPreference] storageKeyPrefix changed, resetting mode: { storageKeyPrefix: 'main', newMode: 'close', previousMode: 'lock' }
   [useSideNavPreference] Reading from localStorage: { storageKey: 'mainSideNavMode', savedValue: null, ... }
   [useSideNavPreference] Using defaultMode (no saved value): close
   [Layout] Sidenav state: { mode: 'close', isExpanded: false, isLocked: false, contentClass: 'none' }
   ```

   **Expected Visual:**
   - ✅ Sidenav COLLAPSED (rail mode)
   - ✅ Content fills full width
   - ✅ No overlap

---

#### Scenario B: With Saved Preference

1. **Go to Storage page** (`/Storage/StorageManagement`)

2. **Click hamburger menu button 3 times** to cycle through modes:
   - Click 1: close → show (overlay)
   - Click 2: show → lock (pushed)
   - Click 3: lock → close (rail)

   **Expected Console Output (each click):**
   ```
   [useSideNavPreference] toggle() called: close => show
   [useSideNavPreference] toggle() called: show => lock
   [useSideNavPreference] toggle() called: lock => close
   ```

3. **Set to CLOSE mode** (sidenav collapsed)

4. **Reload page (F5)** while on `/Storage/StorageManagement`

   **Expected Console Output:**
   ```
   [useSideNavPreference] Reading from localStorage: { storageKey: 'storageSideNavMode', savedValue: 'close', ... }
   [useSideNavPreference] Using saved mode: close
   [Layout] Sidenav state: { mode: 'close', isExpanded: false, isLocked: false, contentClass: 'none' }
   ```

   **Expected Visual:**
   - ✅ Sidenav COLLAPSED (user's choice, overriding default)
   - ✅ Content fills full width
   - ✅ No overlap

5. **Navigate to different Storage page** (`/Storage/ColdStorageMonitoring`)

   **Expected Console Output:**
   ```
   [Layout] Route changed: { pathname: '/Storage/ColdStorageMonitoring', isStorageContext: true, ... }
   // NO storageKeyPrefix change (still 'storage')
   [Layout] Sidenav state: { mode: 'close', isExpanded: false, isLocked: false, contentClass: 'none' }
   ```

   **Expected Visual:**
   - ✅ Sidenav STILL COLLAPSED (preference persists across Storage pages)

---

#### Scenario C: Cross-Context Navigation

1. **Go to Storage page**, set nav to **LOCK** (open + pushed)
   ```
   [useSideNavPreference] toggle() called: ... => lock
   ```

2. **Navigate to Home** (`/Dashboard`)
   
   **Expected Console Output:**
   ```
   [Layout] Route changed: { pathname: '/Dashboard', isStorageContext: false, ... }
   [useSideNavPreference] storageKeyPrefix changed, resetting mode: { storageKeyPrefix: 'main', newMode: 'close', previousMode: 'lock' }
   ```

   **Expected Visual:**
   - ✅ Sidenav COLLAPSED (different context = different preference)

3. **Navigate back to Storage page** (`/Storage/StorageManagement`)
   
   **Expected Console Output:**
   ```
   [Layout] Route changed: { pathname: '/Storage/StorageManagement', isStorageContext: true, ... }
   [useSideNavPreference] storageKeyPrefix changed, resetting mode: { storageKeyPrefix: 'storage', newMode: 'lock', previousMode: 'close' }
   [useSideNavPreference] Reading from localStorage: { storageKey: 'storageSideNavMode', savedValue: 'lock', ... }
   [useSideNavPreference] Using saved mode: lock
   ```

   **Expected Visual:**
   - ✅ Sidenav OPEN (Storage preference restored)
   - ✅ Content PUSHED

---

## What to Report

### If Issue Occurs:

1. **Copy/paste the full console log** from page load to when issue appears

2. **Screenshot showing the visual issue**:
   - Nav state (open/closed)
   - Content position (pushed/overlapped)
   - Browser viewport width

3. **Describe the exact steps**:
   - Starting URL
   - Any clicks (hamburger menu, nav items, etc.)
   - Reload? Navigate? Back button?
   - Ending URL

4. **Check localStorage**:
   ```javascript
   console.log({
     storageSideNavMode: localStorage.getItem('storageSideNavMode'),
     mainSideNavMode: localStorage.getItem('mainSideNavMode')
   });
   ```
   - Copy/paste the output

### Key Things to Look For in Console:

**🔴 RED FLAGS (Bugs):**
- `mode: 'lock'` but `contentClass: 'none'` → Content not pushed when it should be!
- `mode: 'close'` but `contentClass: 'content-nav-locked'` → Content pushed when it shouldn't be!
- `savedValue: 'show'` instead of `'lock'` or `'close'` → Invalid mode in localStorage
- Missing logs (e.g., no `[Layout] Route changed`) → Component not rendering

**✅ GREEN (Expected):**
- `mode`, `isLocked`, and `contentClass` all match:
  - `mode: 'lock'` + `isLocked: true` + `contentClass: 'content-nav-locked'` ✅
  - `mode: 'close'` + `isLocked: false` + `contentClass: 'none'` ✅
- `storageKeyPrefix changed` when navigating between Storage and non-Storage pages
- `Using saved mode` when you've previously set a preference
- `Using defaultMode` when no preference exists

---

## Quick Reset (If Things Get Stuck)

```javascript
// Clear all sidenav preferences
localStorage.removeItem('storageSideNavMode');
localStorage.removeItem('mainSideNavMode');
// Reload page
location.reload();
```

---

## Next Steps

After testing, report back with:
1. Which scenario(s) failed
2. Console logs (full output)
3. Screenshots of visual issues
4. localStorage values

This will help identify whether the issue is:
- **State initialization** (initialMode not working correctly)
- **State persistence** (toggle/setMode not saving correctly)
- **State synchronization** (Layout not applying correct CSS class)
- **Visual rendering** (SideNav component not expanding despite correct state)

