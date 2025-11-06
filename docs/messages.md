# Examples of the messages sent between a “referring” facility and a “referral” lab.

The **referring **lab/clinic is the one which has an order that they would like
to send to the **referral **lab for analysis and results.

This is an example of what is currently sent between referring and referral
labs. Not every field is necessary for the exchange

# Created By Referring Lab

## Original Order Task:

_Used internally by the referring lab_

{

"resourceType": "Task",

"id": "c523f13b-6803-458e-ac26-15a65abf1731",

"meta": {

"versionId": "1",

"lastUpdated": "2021-05-17T23:54:55.409+00:00",

"source": "#1fhTdTXxW5b57qZZ"

},

"identifier": [ {

"system":[ "http://openelis-global.org/order_uuid"](http://openelis-global.org/order_uuid),

"value": "c523f13b-6803-458e-ac26-15a65abf1731"

} ],

"basedOn": [ {

"reference": "ServiceRequest/6207a536-68ed-4b5d-828d-79f4c046af75"

} ],

"status": "ready",

"intent": "original-order",

"priority": "routine",

"for": {

"reference": "Patient/329f09da-0fc9-419d-9575-ace689544269"

},

"authoredOn": "2021-05-17T00:00:00-07:00"

}

## Referring Order Task:

_Sent to the referral lab to start the import process_

{

"resourceType": "Task",

"id": "b3ac906b-0a38-405c-8fb9-11d6313f26c7",

"meta": {

"versionId": "1",

"lastUpdated": "2021-05-17T23:54:55.409+00:00",

"source": "#1fhTdTXxW5b57qZZ"

},

"basedOn": [ {

"reference": "ServiceRequest/6207a536-68ed-4b5d-828d-79f4c046af75"

} ],

"status": "requested",

"for": {

"reference": "Patient/329f09da-0fc9-419d-9575-ace689544269"

},

"authoredOn": "2021-05-17T16:53:34-07:00",

"requester": {

"reference": "Organization/8136af30-901c-4d77-b133-99de824804ee"

},

"owner": {

"reference": "Organization/9dfdb1b3-0cc0-4835-a3b5-e8cb87f460ac"

},

"reasonCode": {

"coding": [ {

"system":[ "http://openelis-global.org/refer_reason"](http://openelis-global.org/refer_reason)

} ]

}

}

## ServiceRequest:

_Contains the loinc code of what is referred_

{

"resourceType": "ServiceRequest",

"id": "6207a536-68ed-4b5d-828d-79f4c046af75",

"meta": {

"versionId": "2",

"lastUpdated": "2021-05-17T23:54:55.546+00:00",

"source": "#bFZoGNc5gaEDkkIx"

},

"identifier": [ {

"system":[ "http://openelis-global.org/analysis_uuid"](http://openelis-global.org/analysis_uuid),

"value": "6207a536-68ed-4b5d-828d-79f4c046af75"

} ],

"requisition": {

"system":[ "http://openelis-global.org/samp_labNo"](http://openelis-global.org/samp_labNo),

"value": "20210000000000090"

},

"status": "active",

"intent": "original-order",

"priority": "routine",

"code": {

"coding": [ {

     "system":[ "http://loinc.org"](http://loinc.org),

     "code": "94500-6",

     "display": "COVID-19 PCR"

} ]

},

"subject": {

"reference": "Patient/329f09da-0fc9-419d-9575-ace689544269"

},

"authoredOn": "2021-05-17T16:53:12-07:00",

"specimen": [ {

"reference": "Specimen/6f93831a-8745-4105-9f2a-0140bc27469f"

} ]

}

## Patient:

{

"resourceType": "Patient",

"id": "329f09da-0fc9-419d-9575-ace689544269",

"meta": {

"versionId": "4",

"lastUpdated": "2021-04-30T00:07:43.944+00:00",

"source": "#PsZSVy7K5fn83mfx"

},

"text": {

"status": "generated",

"div": "&lt;div xmlns=\"http://www.w3.org/1999/xhtml\">&lt;div
class=\"hapiHeaderText\">crày &lt;b>CRAY &lt;/b>&lt;/div>&lt;table
class=\"hapiPropertyTable\">&lt;tbody>&lt;tr>&lt;td>Identifier&lt;/td>&lt;td>121212&lt;/td>&lt;/tr>&lt;tr>&lt;td>Date
of birth&lt;/td>&lt;td>&lt;span>16 May
1994&lt;/span>&lt;/td>&lt;/tr>&lt;/tbody>&lt;/table>&lt;/div>"

},

"identifier": [ {

"system":[ "http://openelis-global.org/pat_subjectNumber"](http://openelis-global.org/pat_subjectNumber),

"value": "121212"

}, {

"system":[ "http://openelis-global.org/pat_nationalId"](http://openelis-global.org/pat_nationalId),

"value": "121212"

}, {

"system":[ "http://openelis-global.org/pat_guid"](http://openelis-global.org/pat_guid),

"value": "329f09da-0fc9-419d-9575-ace689544269"

}, {

"system":[ "http://openelis-global.org/pat_uuid"](http://openelis-global.org/pat_uuid),

"value": "329f09da-0fc9-419d-9575-ace689544269"

} ],

"name": [ {

"family": "cray",

"given": [ "crày" ]

} ],

"gender": "male",

"birthDate": "1994-05-16"

}

## Specimen:

_If present, this will be used to determine the sample type if the loinc code
matches multiple sample types_

{

"resourceType": "Specimen",

"id": "6f93831a-8745-4105-9f2a-0140bc27469f",

"meta": {

"versionId": "2",

"lastUpdated": "2021-05-17T23:54:55.845+00:00",

"source": "#aSxblsC7BShvZNCU"

},

"identifier": [ {

"system":[ "http://openelis-global.org/sampleItem_uuid"](http://openelis-global.org/sampleItem_uuid),

"value": "6f93831a-8745-4105-9f2a-0140bc27469f"

} ],

"accessionIdentifier": {

"system":[ "http://openelis-global.org/sampleItem_labNo"](http://openelis-global.org/sampleItem_labNo),

"value": "20210000000000090-1"

},

"status": "available",

"type": {

"coding": [ {

     "system":[ "http://openelis-global.org/sampleType"](http://openelis-global.org/sampleType),

     "code": "Fluid",

     "display": "Fluid"

} ]

},

"subject": {

"reference": "Patient/329f09da-0fc9-419d-9575-ace689544269"

},

"receivedTime": "2021-05-17T16:52:49-07:00",

"request": [ {

"reference": "ServiceRequest/6207a536-68ed-4b5d-828d-79f4c046af75"

} ]

}

# Created By Referral Lab

## Task:

_Used solely internally by the referring lab_

{

"resourceType": "Task",

"id": "88e75067-a145-4090-99f4-d05a3eba5ea5",

"meta": {

"versionId": "1",

"lastUpdated": "2021-05-18T00:08:14.720+00:00",

"source": "#Cingez3Fyi6zrkKz"

},

"identifier": [ {

"system":[ "http://openelis-global.org/order_uuid"](http://openelis-global.org/order_uuid),

"value": "88e75067-a145-4090-99f4-d05a3eba5ea5"

} ],

"basedOn": [ {

"reference": "ServiceRequest/f4d7feef-c5f6-4222-8d90-dc763cedbbd0"

} ],

"partOf": [ {

"reference": "Task/b3ac906b-0a38-405c-8fb9-11d6313f26c7"

} ],

"status": "completed",

"intent": "order",

"priority": "routine",

"for": {

"reference": "Patient/b8245643-403f-4ef1-945c-0f1f0d39d9e7"

},

"authoredOn": "2021-05-17T00:00:00-07:00"

}

## ServiceRequest:

_Based on the referred service request, used to link the observation and
diagnostic report_

{

"resourceType": "ServiceRequest",

"id": "f4d7feef-c5f6-4222-8d90-dc763cedbbd0",

"meta": {

"versionId": "2",

"lastUpdated": "2021-05-18T00:08:14.959+00:00",

"source": "#zM0ddL5giip3iEtT"

},

"identifier": [ {

"system":[ "http://openelis-global.org/analysis_uuid"](http://openelis-global.org/analysis_uuid),

"value": "f4d7feef-c5f6-4222-8d90-dc763cedbbd0"

} ],

"basedOn": [ {

"reference": "ServiceRequest/6207a536-68ed-4b5d-828d-79f4c046af75"

} ],

"requisition": {

"system":[ "http://openelis-global.org/samp_labNo"](http://openelis-global.org/samp_labNo),

"value": "reflb210000000000107"

},

"status": "completed",

"intent": "order",

"priority": "routine",

"code": {

"coding": [ {

"system":[ "http://loinc.org"](http://loinc.org),

"code": "94500-6",

"display": "COVID-19 PCR"

} ]

},

"subject": {

"reference": "Patient/b8245643-403f-4ef1-945c-0f1f0d39d9e7"

},

"authoredOn": "2021-05-17T17:04:17-07:00",

"specimen": [ {

"reference": "Specimen/ebdc7dd2-08d0-45d0-95ab-43d0fe30860a"

} ]

}

## DiagnosticReport:

{

"resourceType": "DiagnosticReport",

"id": "3ac214f9-19cb-4d52-a0d4-5c4db53842be",

"meta": {

"versionId": "1",

"lastUpdated": "2021-05-18T00:12:55.509+00:00",

"source": "#O6wVL2zh3N9y62fG"

},

"text": {

"status": "generated",

"div": "&lt;div xmlns=\"http://www.w3.org/1999/xhtml\">&lt;div
class=\"hapiHeaderText\"/>&lt;table
class=\"hapiPropertyTable\">&lt;tbody>&lt;tr>&lt;td>Status&lt;/td>&lt;td>PRELIMINARY&lt;/td>&lt;/tr>&lt;/tbody>&lt;/table>&lt;table
class=\"hapiTableOfValues\">&lt;thead>&lt;tr>&lt;td>Name&lt;/td>&lt;td>Value&lt;/td>&lt;td>Interpretation&lt;/td>&lt;td>Reference
Range&lt;/td>&lt;td>Status&lt;/td>&lt;/tr>&lt;/thead>&lt;tbody>&lt;tr
class=\"hapiTableOfValuesRowOdd\">&lt;td/>&lt;td>SARS-COV-2 RNA NOT
DETECTED&lt;/td>&lt;td/>&lt;td/>&lt;td>PRELIMINARY&lt;/td>&lt;/tr>&lt;/tbody>&lt;/table>&lt;/div>"

},

"identifier": [ {

"system":[ "http://openelis-global.org/analysisResult_uuid"](http://openelis-global.org/analysisResult_uuid),

"value": "6207a536-68ed-4b5d-828d-79f4c046af75"

} ],

"basedOn": [ {

"reference": "ServiceRequest/6207a536-68ed-4b5d-828d-79f4c046af75"

} ],

"status": "preliminary",

"subject": {

"reference": "Patient/329f09da-0fc9-419d-9575-ace689544269"

},

"specimen": [ {

"reference": "Specimen/6f93831a-8745-4105-9f2a-0140bc27469f"

} ],

"result": [ {

"reference": "Observation/50ccf09a-962b-4219-9582-26d4c341dc27"

} ]

}

## Observation:

{

"resourceType": "Observation",

"id": "50ccf09a-962b-4219-9582-26d4c341dc27",

"meta": {

"versionId": "1",

"lastUpdated": "2021-05-17T23:54:55.710+00:00",

"source": "#ibQ8Gj5noiOawShQ"

},

"identifier": [ {

"system":[ "http://openelis-global.org/result_uuid"](http://openelis-global.org/result_uuid),

"value": "50ccf09a-962b-4219-9582-26d4c341dc27"

} ],

"basedOn": [ {

"reference": "ServiceRequest/6207a536-68ed-4b5d-828d-79f4c046af75"

} ],

"status": "preliminary",

"subject": {

"reference": "Patient/329f09da-0fc9-419d-9575-ace689544269"

},

"valueString": "SARS-COV-2 RNA NOT DETECTED",

"specimen": {

"reference": "Specimen/6f93831a-8745-4105-9f2a-0140bc27469f"

}

}
