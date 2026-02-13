# Odoo Integration

## Overview

OpenELIS Global includes an optional integration with
[Odoo ERP](https://www.odoo.com/) for automated billing and invoicing. When
enabled, the system automatically creates invoices in Odoo whenever lab orders
are created in OpenELIS.

**Key Features:**

- Automatic invoice creation in Odoo when samples are created
- Patient synchronization as Odoo partners (customers)
- Configurable test-to-product mapping
- Graceful degradation when Odoo is unavailable
- Event-driven asynchronous processing

---

## Deployment Options

OpenELIS offers two approaches for integrating with Odoo:

### Option 1: Built-in XML-RPC Integration (Documented Here)

**Best for:** Organizations with an existing Odoo instance

This approach uses OpenELIS's built-in XML-RPC client to connect to your
existing Odoo installation. Configuration is done through properties files, and
the integration is `enabled/disabled` with a single flag.

### Option 2: Docker-based Full Stack (DIGI-UW Connector)

**Best for:** New deployments or development/testing environments

The
[DIGI-UW odoo-openelis-connector](https://github.com/DIGI-UW/odoo-openelis-connector)
provides a complete Docker-based deployment with:

- Pre-configured Odoo instance
- OpenELIS instance
- Nginx reverse proxy
- Custom `odoo_initializer` addon
- One-command setup: `./setup.sh`

**Quick Start:**

```bash
git clone https://github.com/DIGI-UW/odoo-openelis-connector
cd odoo-openelis-connector
chmod +x setup.sh
./setup.sh
```

Then access:

- Odoo: http://localhost:8069 (admin/admin)
- OpenELIS: https://localhost (admin/adminADMIN!)

## Architecture (Built-in Integration)

### Components

The Odoo integration is built on a modular architecture with five core
components working together to provide seamless billing functionality. The
**OdooClient**
([OdooClient.java](../src/main/java/org/openelisglobal/odoo/client/OdooClient.java))
serves as the XML-RPC client for communicating with Odoo's API, handling
authentication and connection management while providing essential CRUD
operations including create, write, delete, search, and searchAndRead.

The **OdooConnection Interface**
([OdooConnection.java](../src/main/java/org/openelisglobal/odoo/client/OdooConnection.java))
provides an abstraction layer with two implementations: `RealOdooClient` for
active connections when the integration is enabled, and `NoOpOdooClient` as a
no-operation fallback when the integration is disabled. This design pattern
allows the system to gracefully handle both enabled and disabled states without
code changes.

At the heart of the integration is the **OdooIntegrationService**
([OdooIntegrationService.java](../src/main/java/org/openelisglobal/odoo/service/OdooIntegrationService.java)),
which orchestrates the main invoice creation workflow. This service handles
patient partner creation and lookup in Odoo, manages the mapping of OpenELIS
tests to Odoo products, and coordinates the entire billing process from sample
creation to invoice generation.

The **Event Listener**
([SamplePatientUpdateDataCreatedEventListener.java](../src/main/java/org/openelisglobal/sample/event/listener/SamplePatientUpdateDataCreatedEventListener.java))
implements the event-driven architecture by listening for sample creation events
and triggering invoice creation asynchronously. This decoupled approach ensures
that billing operations don't interfere with the primary lab workflow.

Finally, the **Test Product Mapping** component
([TestProductMapping.java](../src/main/java/org/openelisglobal/odoo/config/TestProductMapping.java))
manages the CSV-based mapping between OpenELIS tests and Odoo products,
including configurable pricing and quantities. This externalized configuration
allows administrators to update billing rates without code changes or system
restarts.

### Data Flow

```
Sample Created → Event Fired → Event Listener (Async)
                                      ↓
                              OdooIntegrationService
                                      ↓
                         ┌────────────┴────────────┐
                         ↓                         ↓
                 Find/Create Partner      Map Tests to Products
                  (Patient in Odoo)             ↓
                         ↓                 Create Invoice Lines
                         └────────────┬────────────┘
                                      ↓
                            Create Invoice in Odoo
```

---

## Configuration

### 1. Enable Odoo Integration

Edit `volume/properties/common.properties` or
`src/main/resources/application.properties`:

```properties
# Enable Odoo integration
org.openelisglobal.odoo.enabled=true

# Odoo connection settings
org.openelisglobal.odoo.baseUrl=http://odoo.example.com:8069
org.openelisglobal.odoo.database=your_database_name
org.openelisglobal.odoo.username=your_odoo_username
org.openelisglobal.odoo.password=your_odoo_password

# Test name locale for mapping (en or fr)
org.openelisglobal.odoo.map.testname.locale=en
```

### 2. Configure Test-to-Product Mapping

Edit `volume/odoo/odoo-test-product-mapping.csv`:

```csv
TEST_NAME,PRODUCT_NAME,QUANTITY,PRICE_UNIT
CD4 Absolute count (mm3),CD4 Test,1,42
Hemoglobin,Hemoglobin Test,1,44
Viral load,HIV Viral Load,1,85
```

**Mapping Format:**

- `TEST_NAME`: Exact name of the test in OpenELIS (case-sensitive)
- `PRODUCT_NAME`: Product description in Odoo invoice line
- `QUANTITY`: Quantity to bill (typically 1)
- `PRICE_UNIT`: Unit price in Odoo currency

**Note:** The test name must match the localized test name in OpenELIS. Use the
locale configured in `org.openelisglobal.odoo.map.testname.locale` (default:
`en`).

## How It Works

### Sample Creation Trigger

When a sample is created in OpenELIS through Order Entry or similar workflows,
the system fires a `SamplePatientUpdateDataCreatedEvent`. This event is picked
up asynchronously by a dedicated event listener, which then calls the
`OdooIntegrationService.createInvoice()` method to begin the billing process.
This asynchronous approach ensures that invoice creation happens in the
background without blocking or slowing down the sample entry workflow.

### Patient Partner Creation and Lookup

The integration service attempts to find an existing Odoo partner (customer)
record for the patient before creating the invoice. It first searches for a
partner with a matching national ID in Odoo's `ref` field. If no match is found
by national ID, the service falls back to searching by the patient's name. If
the patient still cannot be found in Odoo, the service creates a new partner
record containing the patient's full name, national ID, email, phone, address,
and a comment noting the OpenELIS Patient ID for reference. If all partner
lookup and creation attempts fail, the system defaults to using partner ID `1`,
which is typically the main company record in Odoo.

### Invoice Line Creation

For each test included in the order, the service looks up the test name in the
configured CSV mapping file. When a mapping exists, the service creates an
invoice line containing the product name or description, quantity (typically 1),
unit price, and account ID (which defaults to 1). If no mapping is found for a
particular test, the service logs a warning and skips that test, ensuring that
unmapped tests don't prevent invoice creation but are documented for
administrative review.

### Invoice Creation

The service creates a complete Odoo invoice of type `account.move` configured as
a customer invoice (`out_invoice`). The invoice includes the patient's partner
ID, a reference number formatted as `OE-{AccessionNumber}` (for example,
"OE-20250214-0001"), the current date as the invoice date, and all the mapped
test line items with their quantities and prices. This creates a draft invoice
in Odoo that can be reviewed, confirmed, and sent to the patient through Odoo's
standard invoicing workflow.
