# Adding Organizations to into FHIRstore

# Step 1:

Choose whether you want to directly connect to the FHIRstore (requires
configuring certificates) or go through OpenHIM (only requires setting and using
a username/password)

## Step 1a: Set up a Direct Connection to the Fhirstore

Because we usually set up trust patterns between servers to use certificates, to
directly talk with the FHIRstore you will need to add a trusted client cert to
either your browser or POSTman.

Tutorials for both options can be found online (it is not covered here as it
varies between browsers), or you can use Step 1b instead to use a username and
password instead.

Test that your connection works by navigating to or making a GET request to
https://&lt;fhir-server-address>:&lt;port usually 8444>/fhir/Organization

You should receive a page of the current Organizations in JSON format.

## Step 1b: Set up a Connection Through OpenHIM

Another option is to communicate through OpenMRS so you can use HTTP Basic auth
to authenticate instead of certs.

Create a client in OpenHIM using the instructions
[here](http://openhim.org/docs/configuration/clients).

- Remember your client ID as it will be your username
- Give the client a role that has access to the FHIRstore channel (most likely
  one of fhir-pusher, fhir-puller, otherwise you will have to add the user to
  the channel as shown [here](http://openhim.org/docs/configuration/channels))
- Configure authentication with Basic Auth, and keep track of your password

Test that your connection works by navigating to or making a GET request to
https://&lt;OpenHIMaddress>:5000/fhir/Organization with basic auth properties
set.

You should receive a page of the current Organizations in JSON format.

# Step 2: Generating a UUID

Because we want to use uuid’s for all our fhir objects, you will need to
generate one. There are many generators online such as
[https://www.uuidgenerator.net/](https://www.uuidgenerator.net/). It is
recommended to use UUID version 4 when generating.

# Step 3: Get the Organization type(s) in FHIR notation

Below is a list of types that have a special meaning in OpenELIS. Any additional
types will be ignored by OpenELIS unless code change is requested, but they can
be added into the FHIR

```
{

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "TestKitVender",

  	"display": "Test Kit Vendor"

	} ]

  }
```

```
 {

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "referring clinic",

  	"display": "Referring clinic"

	} ]

  }
```

```
{

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "referralLab",

  	"display": "Referring Lab"

	} ]

  }
```

```
{

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "Health District",

  	"display": "Health District"

	} ]

  }
```

```
{
	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "Health Region",

  	"display": "Health Region"

	} ]

  }
```

```
{

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "patient referral",

  	"display": "Patient Referral"

	} ]

  }
```

```
 {

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "ARV Service Loc",

  	"display": "ARV Service Loc"

	} ]

  }
```

```
 {

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "EID ACONDA-VS CI",

  	"display": "EID ACONDA-VS CI"

	} ]

  }
```

```
 {

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "EID EGPAF",

  	"display": "EID EGPAF"

	} ]

  }
```

```
 {

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "EID ESTHER",

  	"display": "EID ESTHER"

	} ]

  }
```

```
 {

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "EID ICAP",

  	"display": "EID ICAP"

	} ]

  }
```

```
{

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "RTN HIV Service Loc",

  	"display": "RTN HIV Service Loc"

	} ]

  }
```

```
 {

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "RTN HIV Hospitals",

  	"display": "RTN HIV Hospitals"

	} ]

  }
```

```
{

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "EID SEV-CI",

  	"display": "EID SEV-CI"

	} ]

  }
```

```
{

	"coding": [ {

  	"system": "http://openelis-global.org/orgType",

  	"code": "EID ARIEL",

  	"display": "EID ARIEL"

	} ]

  }
```

# Step 4: Write your Organization in FHIR notation

Below is a template of a FHIR Organization. Items in **ALL CAPS** should be
replaced with specific values:

The following are _Optional_ Fields

1. CLIA Number
1. Short Name
1. Code

1. All address fields
1. Part of

```
{

  "resourceType": "Organization",

  "id": "GENERATED UUID",

  "identifier": [

    {

     "system": "http://openelis-global.org/org_cliaNum",

     "value": "CLIANUM"

    }, {

      "system": "http://openelis-global.org/org_shortName",

      "value": "SHORTNAME"

    }, {

      "system": "http://openelis-global.org/org_code",

      "value": "CODE"

    }

],

  "type": [ {COMMA DELIMITED}, {LIST OF TYPES HERE}, {IN FORMAT FROM STEP 2} ],

  "name": "ORGNAME",

  "address": [ {

    "line": [ "STREET ADDRESS" ],

    "city": "CITY",

    "state": "STATE",

    "postalCode": "POSTAL CODE",

    "country": "COUNTRY"

  } ],

   "partOf": {

	"reference": "Organization/PARENT_UUID"

  }
```

partOf is used for organizations that have structure where one is a subsection
of the other. This is mainly used in OpenELIS for Health Districts vs Health
regions. One should create the parent reference before the child, unless you are
committing all the Organizations in one action (as in Step 5b)

# Step 5:

Decide whether you want to add entities one by one, or commit them in one large
transaction.

## Step 5a: Add a Single Organization to Fhir place

In POSTman:

- select PUT operation
- Set address as the either the FHIR address or the OpenHIM address
  https://&lt;fhir or OpenHIM address>:&lt;port>/fhir/Organization/generated
  uuid
- Add Organization as the request body
- set Content-Type header to application/json
- execute the command

Alternatively use the FHIR UI:

- Navigate to https://&lt;fhir or OpenHIM address>:&lt;port>
- select Organization on the left hand list
- go to CRUD operation and do an UPDATE (NOT INSERT)
- Set the id as the generated uuid
- Execute the command

## Step 5b: Add Organization to Bundle

Template for the bundle is below. The entity in green represents a single entry
in the bundle. Multiple entries can be added as a comma delimited list.

{

"resourceType": "Bundle",

"id": "bundle-transaction",

"type": "transaction",

"entry": [

    {

      "resource":

    {Organization JSON here},

      "request": {

        "method": "PUT",

        "url": "Organization/generated uuid"

    }

}

]

}

Once your bundle contains all the entries you want you can execute the
transaction bundle

In POSTman:

- select POST operation
- Set address as https://&lt;fhir or OpenHIM address>:&lt;port>/fhir
- Add Bundle as the request body
- set Content-Type header to application/json
- execute the command
