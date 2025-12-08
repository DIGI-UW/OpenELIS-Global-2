function defaultFailure(xhr){
    alert(xhr.responseText);
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getNotificationsForTests( testIds, success, failure){
	if( !failure ){	failure = defaultFailure;}
	if (!testIds || testIds === "" || testIds.length === 0) {
		return;
	}

	new Ajax.Request('TestNotificationConfig/raw/list',
			{
				method : 'get', 
				parameters : "testIds=" + testIds.join(",") ,
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : success,
				onFailure : failure
			});

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getProviderInfo( providerId, success, failure){
	if( !failure ){	failure = defaultFailure;}
	if (!providerId || providerId === "") {
		return;
	}

	new Ajax.Request('rest/Provider/raw/' + providerId,
			{
				method : 'get', 
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : success,
				onFailure : failure
			});

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getProviderInfoByPersonId( personId, success, failure){
	if( !failure ){	failure = defaultFailure;}
	if (!personId || personId === "") {
		return;
	}

	new Ajax.Request('rest/Provider/Person/' + personId,
			{
				method : 'get', 
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : success,
				onFailure : failure
			});

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getLabOrder( orderNumber, success, failure){
	if( !failure ){	failure = defaultFailure;}
	
	new Ajax.Request('ajaxQueryXML',
			{
				method : 'get', 
				parameters : "provider=LabOrderSearchProvider&orderNumber=" + orderNumber ,
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : success,
				onFailure : failure
			});

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getSampleForLabOrderOrPatient( orderNumber, patientPK, success, failure, additionalSuccessParams){
	if( !failure ){	failure = defaultFailure;}
	
	new Ajax.Request('ajaxQueryXML',
			{
				method : 'get', 
				parameters : "provider=SampleSearchPopulateProvider&patientKey=" + patientPK + "&accessionNo=" + orderNumber ,
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : additionalSuccessParams !== undefined ? function (xhr) {
				 	success(xhr, additionalSuccessParams);
				} : success,
				onFailure : failure
			});

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getSampleForLabOrderOrPatientWithTestLOINC( orderNumber, patientPK, loinc, unvalidatedTestOnly, success, failure, additionalSuccessParams){
	if( !failure ){	failure = defaultFailure;}
	
	new Ajax.Request('ajaxQueryXML',
			{
				method : 'get', 
				parameters : "provider=SampleSearchPopulateProvider&unvalidatedTestOnly=" + unvalidatedTestOnly + "&loinc=" + loinc + "&patientKey=" + patientPK + "&accessionNo=" + orderNumber ,
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : additionalSuccessParams !== undefined ? function (xhr) {
				 	success(xhr, additionalSuccessParams);
				} : success,
				onFailure : failure
			});

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getTestNames( testId, success, failure){
    if( !failure ){	failure = defaultFailure;}

    new Ajax.Request('ajaxQueryXML',
        {
            method : 'get',
            parameters : "provider=TestNamesProvider&testId=" + testId ,
            //indicator: 'throbbing',
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess : success,
            onFailure : failure
        });
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getTestEntities( testId, success, failure){
    if( !failure ){	failure = defaultFailure;}

    new Ajax.Request('ajaxQueryXML',
        {
            method : 'get',
            parameters : "provider=TestEntitiesProvider&testId=" + testId ,
            //indicator: 'throbbing',
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess : success,
            onFailure : failure
        });
}

function getTestResultLimits(testId, success, failure){
	if( !failure ){	failure = defaultFailure;}

    new Ajax.Request('ajaxQueryXML',
        {
            method : 'get',
            parameters : "provider=TestResultLimitsProvider&testId=" + testId ,
            //indicator: 'throbbing',
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess : success,
            onFailure : failure
        });
}

/**
 * A generic way to get localized names for a given entity rather than a new call for each type.  Expand and document as needed
 * @param entitiyId
 * @param entityName may only be one of "panel"  The names can also be found in EntityNamesProvider
 * @param success
 * @param failure
 */
//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getEntityNames( entitiyId,entityName ,success, failure){
    var permitted = ['panel','sampleType','testSection','unitOfMeasure','method'];

    if( permitted.indexOf(entityName) == -1){
        alert( "\"" + entityName + "\" has not been implemented for getEntityNames");
        return;
    }
    if( !failure ){	failure = defaultFailure;}

    new Ajax.Request('ajaxQueryXML',
        {
            method : 'get',
            parameters : "provider=EntityNamesProvider&entityId=" + entitiyId + "&entityName=" + entityName ,
            //indicator: 'throbbing',
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess : success,
            onFailure : failure
        });
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getDistrictsForRegion( regionId, selectedValue, success, failure){
	if( !failure ){	failure = defaultFailure;}
	
	new Ajax.Request('ajaxQueryXML',
			{
				method : 'get', 
				parameters : "provider=HealthDistrictForRegionProvider&regionId=" + regionId +"&selectedValue=" + selectedValue,
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : success,
				onFailure : failure
			});

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getCodeForOrganization( organizationId, success, failure){
	if( !failure ){	failure = defaultFailure;}
	
	new Ajax.Request('ajaxQueryXML',
			{
				method : 'get', 
				parameters : "provider=CodeForOrganizationProvider&organizationId=" + organizationId,
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : success,
				onFailure : failure
			});

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getDepartmentsForSiteClinic( siteClinicId, selectedValue, success, failure){
	if( !failure ){	failure = defaultFailure;}
	
	new Ajax.Request('ajaxQueryXML',
			{
				method : 'get', 
				parameters : "provider=DepartmentsForReferringClinicProvider&referringClinicId=" + siteClinicId +"&selectedValue=" + selectedValue,
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : success,
				onFailure : failure
			});

}


//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getTestsForSampleType(sampleTypeId, success, failure) {
	var request = "&sampleType=" + sampleTypeId;
	if( !failure ){	failure = defaultFailure;}
	
	new Ajax.Request('ajaxQueryXML', // url
	{// options
		method : 'get', // http method
		parameters : "provider=SampleEntryTestsForTypeProvider" + request,
		// indicator: 'throbbing'
		requestHeaders : {
			"X-CSRF-Token" : getCsrfToken()
		},
		onSuccess : success,
		onFailure : failure
	});
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function validateAccessionNumberOnServer(ignoreYear, ignoreUsage, fieldId, accessionNumber, success, failure) {
    if( !failure ){ failure = defaultFailure;}
    new Ajax.Request(
            'ajaxXML', // url
            {// options
                method : 'get', // http method
                parameters : 'provider=SampleEntryAccessionNumberValidationProvider&ignoreYear=' + ignoreYear + '&ignoreUsage=' + ignoreUsage + '&field='   + fieldId + '&accessionNumber=' + accessionNumber,
                indicator : 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
                onSuccess : success,
                onFailure : failure
            });
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function validateProjectAccessionNumberOnServer(fieldId, recordType, parseForProjectFormName, accessionNumber, success, failure) {
    if( !failure ){ failure = defaultFailure;}
    new Ajax.Request(
            'ajaxXML', // url
            {// options
                method : 'get', // http method
                parameters : 'provider=SampleEntryAccessionNumberValidationProvider&field=' + fieldId + '&recordType=' + recordType + '&parseForProjectFormName=' + parseForProjectFormName + '&accessionNumber=' + accessionNumber,
                indicator : 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
                onSuccess : success,
                onFailure : failure
            });
}


//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function generateNextScanNumber(success, failure, additionalSuccessParams ){
    if( !failure ){	failure = defaultFailure;}

    new Ajax.Request (
        'ajaxQueryXML',  //url
        {//options
            method: 'get', //http method
            parameters : "provider=SampleEntryGenerateScanProvider",
            //indicator: 'throbbing'
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			}
			,onSuccess : additionalSuccessParams !== undefined ? function (xhr) {
				 	success(xhr, additionalSuccessParams);
				} : success,
            onFailure:  failure
        }
    );
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function generateNextProgramScanNumber(prefix, success, failure){
    if( !failure ){	failure = defaultFailure;}

    new Ajax.Request (
        'ajaxQueryXML',  //url
        {//options
            method: 'get', //http method
            parameters : "provider=SampleEntryGenerateScanProvider&programCode=" + prefix,
            //indicator: 'throbbing'
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess:  success,
            onFailure:  failure
        }
    );
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function validateNonConformityRecordNumberOnServer( field, success, failure){
	if( !failure){failure = defaultFailure;	}

	new Ajax.Request('ajaxXML',
			{
				method : 'get', 
				parameters : "provider=NonConformityRecordNumberValidationProvider&fieldId=" + field.id +"&value=" + field.value,
			    //indicator: 'throbbing',
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
				onSuccess : success,
				onFailure : failure
			});

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function validatePhoneNumberOnServer( field, success, failure){
    if( !failure){failure = defaultFailure;	}

    new Ajax.Request('ajaxXML',
        {
            method : 'get',
            parameters : "provider=PhoneNumberValidationProvider&fieldId=" + field.id +"&value=" + field.value,
            //indicator: 'throbbing',
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess : success,
            onFailure : failure
        });

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function validateSubjectNumberOnServer( subjectNumber, type, elementId, success, failure){
    if( !failure ){	failure = defaultFailure;}

    new Ajax.Request('ajaxXML',
        {
            method : 'get',
            parameters : "provider=SubjectNumberValidationProvider&subjectNumber=" + subjectNumber + "&numberType=" + type + "&fieldId=" + elementId,
            //indicator: 'throbbing',
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess : success,
            onFailure : failure
        });

}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function patientSearch(lastName, firstName, STNumber, subjectNumber, nationalId, labNumber, guid, dateOfBirth, gender, suppressExternalSearch, success, failure, additionalSuccessParams){
	if( !failure){failure = defaultFailure;	}
	new Ajax.Request (
            'ajaxQueryXML',  //url
             {//options
               method: 'get', //http method
               parameters : "provider=PatientSearchProvider&lastName=" + lastName +
               			  "&firstName=" + firstName +
               			  "&STNumber=" + STNumber +
               			  "&subjectNumber=" + subjectNumber +
               			  "&nationalID=" + nationalId +
               			  "&labNumber=" + labNumber +
               			  "&guid=" + guid +
               			  "&dateOfBirth=" + dateOfBirth +
               			  "&gender=" + gender +
               			  "&suppressExternalSearch=" + suppressExternalSearch,
          		requestHeaders : {
        					"X-CSRF-Token" : getCsrfToken()
        				},
				onSuccess : additionalSuccessParams !== undefined ? function (xhr) {
				 	success(xhr, additionalSuccessParams);
				} : success,
               onFailure:  failure
              }
           );	
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getReflexUserChoice( resultId, analysisId, testId, accessionNumber, index, success, failure){
    if( !failure){failure = defaultFailure;	}
	
	new Ajax.Request (
            'ajaxQueryXML',  //url
            {//options
            method: 'get', //http method
            parameters: 'provider=TestReflexUserChoiceProvider&resultIds=' + resultId + 
            			'&analysisIds=' +  analysisId + 
            			'&testIds=' + testId + 
            			'&rowIndex=' + index +
            			'&accessionNumber=' + accessionNumber,
            indicator: 'throbbing',
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess:  success,
            onFailure:  failure
                 }
                );
	
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getProvidersForOrg( orgKey, success, failure){
    if( !failure){failure = defaultFailure;	}
    new Ajax.Request(
        'ajaxQueryXML',  //url
        {//options
            method: 'get', //http method
            parameters : "provider=RequestersForOrganizationProvider&orgId=" + orgKey,
            //indicator: 'throbbing'
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess: success,
            onFailure: failure
        }
    );
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getAllTestsForSampleType( sampleTypeId, success, failure){
    if( !failure){failure = defaultFailure;	}
    new Ajax.Request(
        'ajaxQueryXML',  //url
        {//options
            method: 'get', //http method
            parameters : "provider=AllTestsForSampleTypeProvider&sampleTypeId=" + sampleTypeId,
            //indicator: 'throbbing'
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess: success,
            onFailure: failure
        }
    );
}

//sensitive data is being transmitted, therefore a token check should be done even on GET. 
//Otherwise this should be moved to a POST request and rely on regular csrf functionality
function getPendingAnalysisForTest( testId, success, failure){
    if( !failure){failure = defaultFailure;	}
    new Ajax.Request(
        'ajaxQueryXML',  //url
        {//options
            method: 'get', //http method
            parameters : "provider=getPendingAnalysisForTestProvider&testId=" + testId,
            //indicator: 'throbbing'
			requestHeaders : {
				"X-CSRF-Token" : getCsrfToken()
			},
            onSuccess: success,
            onFailure: failure
        }
    );
}

function postBatchSample(success, failure){
    if( !failure){failure = defaultFailure;	}
	new Ajax.Request(
		'SamplePatientEntryBatch',  //url
		{//options
			method: 'POST', //http method
			parameters: jQuery(document.getElementById("mainForm")).serialize().replace(/\+/g,'%20'),
		    onSuccess: success,
		    onFailure: failure
		}
	);
	
}

function postBatchSampleByProject(projectUrl, success, failure) {
    if( !failure){failure = defaultFailure;	}
    new Ajax.Request(
    		projectUrl,  //url
    		{//options
    			method: 'POST', //http method
    			parameters: jQuery(document.getElementById("mainForm")).serialize().replace(/\+/g,'%20'),
    		    onSuccess: success,
    		    onFailure: failure
    		}
    	);
}

function setupExperimentFile(success, failure) {
    if( !failure){failure = defaultFailure;	}
    new Ajax.Request(
    		"AnalyzerSetupAPI",  //url
    		{//options
    			method: 'POST', //http method
    			parameters: jQuery(document.getElementById("mainForm")).serialize().replace(/\+/g,'%20'),
    		    onSuccess: success,
    		    onFailure: failure
    		}
    	);
}

function getPreviousExperimentSetup(id, success, failure) {
    if( !failure){failure = defaultFailure;	}
    new Ajax.Request(
    		"AnalyzerSetup/" + id,  //url
    		{//options
    			method: 'GET', //http method
    		    onSuccess: success,
    		    onFailure: failure
    		}
    	);
}

function runLIStoAnalyzerAction(analyzerType, action, success, failure) {
    if( !failure){failure = defaultFailure;	}
    new Ajax.Request(
    		"analyzer/runAction",  //url
    		{//options
    			method: 'POST', //http method
                parameters : "analyzerType=" + analyzerType + "&actionName=" + action,
				requestHeaders : {
					"X-CSRF-Token" : getCsrfToken()
				},
    		    onSuccess: success,
    		    onFailure: failure
    		}
    	);
}

