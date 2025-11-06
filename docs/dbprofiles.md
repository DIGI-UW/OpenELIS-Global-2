# Database Profiles

Database profiles are used when one wants to mass deactivate sites ot tests,
this is used when one is **not** using the FHIR facility Registry on the
Consolidated Server. The noral use case is when an administrator has just
installed OpenELIS and wants to deactivate the facilities and tests included
with the OpenELIS installer.

In this case, you will create a CSV with **only the test or facilities you wish
to remain active** all others will be deactivated.

#

**Profiles**

Only some tables support being "profiled". These tables are currently:

- Organization
- Test

When a table is profiled, all the entities you wish to have must already exist
in the database. The action that is done through these urls is simply activating
all entities that match the provided input and deactivating all others. This
action can currently only be done by the admin user.

#

**Applying Organization Profiles**

URL: `https://{server-address}/OpenELIS-Global/OrganizationProfile`

Steps:

1. Compile a comma or new line delimited list of all organizations that are to
   be/stay activated (must match the current name exactly as specified in the
   "name" column in clinlims.organization)
2. Save file as a csv
3. Upload the file at the above url and save the changes
4. Confirm that only your organizations are activated

#

**Applying Test Profiles**

URL: `https://{server-address}/OpenELIS-Global/TestProfile`

Steps:

1. Compile a comma or new line delimited list of all tests that are to be/stay
   activated (can match either the french or english value of the name, but must
   be exact)
2. Save file as a csv
3. Upload the file at the above url and save the changes
4. Confirm that only your tests are activated
