# Equipment Usage Log - Quick Start Guide

## ⚡ 5-Minute Overview

**What**: Standalone module to track permanent laboratory equipment usage
**Why**: MNTD compliance (Format 5.3-003) + regulatory audits **Where**: Side
navigation menu → "Equipment Usage" → "Equipment Usage Log" **Who**: Lab
Technologists (create/view), Lab Supervisors (approve)

## 🚀 Getting Started (Developer)

### Prerequisites

- OpenELIS Global 2 repository cloned
- Java 17+ installed
- Maven 3.8+ installed
- Node.js 14+ (for frontend)
- PostgreSQL/MySQL database running

### Build & Deploy

```bash
# 1. Build backend
mvn clean install -DskipTests -Dmaven.test.skip=true

# 2. Format code (CRITICAL - do this before commit)
mvn spotless:apply

# 3. Build frontend (optional, included in Maven build)
cd frontend && npm run build && cd ..

# 4. Start application
java -jar target/OpenELIS-Global-*.jar
```

### First-Time Setup

1. **Access Application**: http://localhost:8080
2. **Login**: Use a user with RESULTS or GLOBAL_ADMIN role
3. **Create Equipment**:
   - Navigate to side menu → Equipment Usage → Equipment Usage Log
   - Click "New Entry"
   - Select equipment from dropdown (or create equipment via REST API first)
4. **Create Usage Entry**:
   - Fill form fields
   - Click "Save" to save as DRAFT
   - Click "Submit" to send for approval
5. **Approve** (as Supervisor):
   - Go to "Approval" tab
   - Review pending entries
   - Click "Approve" or "Reject"

## 📁 Key Files to Know

### Backend

- **Database**:
  `src/main/resources/liquibase/3.4.x.x/041-equipment-usage-log.xml`
- **Models**: `src/main/java/org/openelisglobal/equipmentusage/valueholder/`
- **Business Logic**: `src/main/java/org/openelisglobal/equipmentusage/service/`
- **API**: `src/main/java/org/openelisglobal/equipmentusage/controller/rest/`

### Frontend

- **Main Component**:
  `frontend/src/components/equipmentUsage/EquipmentUsageManagement.jsx`
- **Service Layer**:
  `frontend/src/components/equipmentUsage/EquipmentUsageService.js`
- **Styling**: `frontend/src/components/equipmentUsage/EquipmentUsage.css`

### Routes

- **Route Definition**: `frontend/src/App.js` (search for `/equipment-usage`)
- **Menu Deployment**: Automatic via Liquibase migration

## 🔌 API Quick Reference

### Create Equipment

```bash
POST /rest/equipment
{
  "name": "Centrifuge A",
  "serialNumber": "CENT-2025-001",
  "department": "Hematology",
  "manufacturer": "Sigma",
  "modelNumber": "3-30K"
}
```

### Create Usage Entry

```bash
POST /rest/equipment-usage
{
  "equipment": { "id": 1 },
  "operatorName": "John Doe",
  "loginTime": "2025-12-31T09:00:00",
  "logoutTime": "2025-12-31T17:00:00",
  "activitiesDone": "Processed 50 samples",
  "equipmentStatus": "FUNCTIONAL",
  "department": "Hematology"
}
```

### Submit for Approval

```bash
PUT /rest/equipment-usage/1/submit
(no body required)
```

### Approve Entry

```bash
PUT /rest/equipment-usage/1/approve?approverId=123
(no body required)
```

### Search Entries

```bash
GET /rest/equipment-usage/search?equipmentId=1&status=SUBMITTED&startDate=2025-12-01&endDate=2025-12-31
```

## 🧪 Quick Test Workflow

### Step 1: Create Equipment

```bash
curl -X POST http://localhost:8080/rest/equipment \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Centrifuge",
    "serialNumber": "TEST-001",
    "department": "Lab A",
    "manufacturer": "TestCorp"
  }'
# Save the returned ID (e.g., 1)
```

### Step 2: Create Usage Entry

```bash
curl -X POST http://localhost:8080/rest/equipment-usage \
  -H "Content-Type: application/json" \
  -d '{
    "equipment": {"id": 1},
    "operatorName": "Test User",
    "loginTime": "2025-12-31T08:00:00",
    "logoutTime": "2025-12-31T16:00:00",
    "activitiesDone": "Tested equipment",
    "equipmentStatus": "FUNCTIONAL",
    "department": "Lab A"
  }'
# Save the returned ID (e.g., 1)
```

### Step 3: Submit for Approval

```bash
curl -X PUT http://localhost:8080/rest/equipment-usage/1/submit
```

### Step 4: Approve

```bash
curl -X PUT http://localhost:8080/rest/equipment-usage/1/approve?approverId=1
```

### Step 5: Verify

```bash
curl http://localhost:8080/rest/equipment-usage/1
# Should see entryStatus: "APPROVED"
```

## 🐛 Common Issues & Fixes

| Issue                           | Cause                     | Solution                                                                 |
| ------------------------------- | ------------------------- | ------------------------------------------------------------------------ |
| "Equipment Usage" not in menu   | Migration not applied     | Check Liquibase logs; ensure 041-equipment-usage-log.xml is in changelog |
| 404 on `/equipment-usage` route | Route not added to App.js | Verify EquipmentUsageManagement import and route definition              |
| Cannot create equipment entry   | No equipment in dropdown  | Create equipment first via REST API or admin                             |
| "Access Denied" on approval     | Wrong role                | Ensure user has RESULTS or GLOBAL_ADMIN role                             |
| Dropdown empty after filter     | Backend service issue     | Check REST endpoint returns data; verify equipment is active             |

## 📊 Database Verification

```sql
-- Check equipment table
SELECT COUNT(*) FROM equipment;

-- Check usage entries
SELECT COUNT(*) FROM equipment_usage_entry;

-- Check audit trail
SELECT COUNT(*) FROM equipment_usage_audit;

-- Check menu items
SELECT * FROM menu WHERE element_id LIKE 'menu_equipment%';

-- Sample query: All approved entries
SELECT * FROM equipment_usage_entry WHERE entry_status = 'APPROVED';

-- Sample query: Entries pending approval
SELECT * FROM equipment_usage_entry WHERE entry_status = 'SUBMITTED';
```

## 📖 Documentation Map

| Document                              | Purpose                                   |
| ------------------------------------- | ----------------------------------------- |
| `EQUIPMENT_USAGE_IMPLEMENTATION.md`   | Complete technical documentation          |
| `EQUIPMENT_USAGE_DELIVERY_SUMMARY.md` | What was delivered & deployment checklist |
| `EQUIPMENT_USAGE_QUICKSTART.md`       | This file - quick reference               |
| `.specify/memory/constitution.md`     | Architecture principles                   |
| `AGENTS.md`                           | Project guidelines for AI agents          |

## ✅ Production Checklist

Before going live:

- [ ] Database migration applied successfully
- [ ] All Java classes compiled without errors
- [ ] Frontend components bundled successfully
- [ ] Menu item appears in navigation
- [ ] Can create equipment entries
- [ ] Can submit for approval
- [ ] Can approve as supervisor
- [ ] Audit trail records changes
- [ ] Export framework tested (UI appears)
- [ ] Role-based access tested

## 🎯 Next Steps

1. **For Users**: Navigate to Equipment Usage → Equipment Usage Log
2. **For Admins**: Create equipment master data via REST API or admin interface
3. **For Developers**: See `EQUIPMENT_USAGE_IMPLEMENTATION.md` for detailed API
   docs
4. **For QA**: Run test workflow above and verify each step

## 📞 Need Help?

1. Check `EQUIPMENT_USAGE_IMPLEMENTATION.md` Troubleshooting section
2. Review application logs: `tail -f logs/openelis.log`
3. Check Liquibase migration logs
4. Verify database connection
5. Ensure user has correct role

## 🔗 Related Resources

- OpenELIS Documentation: [README.md](README.md)
- Constitution (Architecture):
  [.specify/memory/constitution.md](.specify/memory/constitution.md)
- Agent Guidelines: [AGENTS.md](AGENTS.md)
- Inventory Module (similar pattern): Refer to InventoryManagement.jsx

---

**Quick Start Guide v1.0** **Last Updated: 2025-12-31**
