<%@ page language="java" contentType="text/html; charset=UTF-8"
         import="org.openelisglobal.common.formfields.FormFields.Field,
				 org.openelisglobal.sample.util.AccessionNumberUtil,
                 org.openelisglobal.common.services.PhoneNumberService,
                 org.openelisglobal.common.util.ConfigurationProperties,
                 org.openelisglobal.common.util.ConfigurationProperties.Property,
                 org.openelisglobal.internationalization.MessageUtil,
                 org.openelisglobal.common.util.IdValuePair,
                 org.openelisglobal.common.util.Versioning,
                 org.openelisglobal.common.util.DateUtil,
                 org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.common.formfields.FormFields" %>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<c:set var="formName" value="${form.formName}" />


<%
	boolean useCollectionDate = FormFields.getInstance().useField( Field.CollectionDate );
    boolean useInitialSampleCondition = FormFields.getInstance().useField( Field.InitialSampleCondition );
    boolean useCollector = FormFields.getInstance().useField( Field.SampleEntrySampleCollector );
    boolean autofillCollectionDate = ConfigurationProperties.getInstance().isPropertyValueEqual( Property.AUTOFILL_COLLECTION_DATE, "true" );
    boolean useReferralSiteList = FormFields.getInstance().useField( FormFields.Field.RequesterSiteList );
    boolean useReferralSiteCode = FormFields.getInstance().useField( FormFields.Field.SampleEntryReferralSiteCode );
    boolean useProviderInfo = FormFields.getInstance().useField( FormFields.Field.ProviderInfo );
    boolean patientRequired = FormFields.getInstance().useField( FormFields.Field.PatientRequired );
    boolean trackPayment = ConfigurationProperties.getInstance().isPropertyValueEqual( Property.TRACK_PATIENT_PAYMENT, "true" );
    boolean acceptExternalOrders = ConfigurationProperties.getInstance().isPropertyValueEqual( Property.ACCEPT_EXTERNAL_ORDERS, "true" );
    boolean restrictNewReferringSiteEntries = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.restrictFreeTextRefSiteEntry, "true");
    boolean restrictNewProviderEntries = ConfigurationProperties.getInstance().isPropertyValueEqual(Property.restrictFreeTextProviderEntry, "true");
	boolean useSiteDepartment = FormFields.getInstance().useField(Field.SITE_DEPARTMENT );
    pageContext.setAttribute("restrictNewReferringSiteEntries", restrictNewReferringSiteEntries);
%>

<script type="text/javascript" src="scripts/additional_utilities.js"></script>
<script type="text/javascript" src="scripts/jquery.asmselect.js?"></script>
<script type="text/javascript" src="scripts/ajaxCalls.js?"></script>
<script type="text/javascript" src="scripts/laborder.js?"></script>



<link rel="stylesheet" type="text/css" href="css/jquery.asmselect.css?"/>


<script type="text/javascript">
    var useReferralSiteList = <%= useReferralSiteList%>;
    var useReferralSiteCode = <%= useReferralSiteCode %>;
    var useSiteDepartment = <%= useSiteDepartment %>;
    
    function checkAccessionNumber(accessionNumber) {
        //check if empty
        if (!fieldIsEmptyById("labNo")) {
            validateAccessionNumberOnServer(false, false, accessionNumber.id, accessionNumber.value, processAccessionSuccess, null);
        }
        else {
             selectFieldErrorDisplay(false, $("labNo"));
        }

        setCorrectSave();
    }

    function processAccessionSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var message = xhr.responseXML.getElementsByTagName("message").item(0);
        var success = false;

        if (message.firstChild.nodeValue == "valid") {
            success = true;
        }
        var labElement = formField.firstChild.nodeValue;
        selectFieldErrorDisplay(success, $(labElement));

        if (!success) {
            alert(message.firstChild.nodeValue);
        }

        setCorrectSave();
    }

    function setCorrectSave(){
        if( window.setSave){
            setSave();
        }else if(window.setSaveButton){
            setSaveButton();
        }
    }

    function getNextAccessionNumber() {
        generateNextScanNumber(processScanSuccess);
    }

    function processScanSuccess(xhr) {
        //alert(xhr.responseText);
        var formField = xhr.responseXML.getElementsByTagName("formfield").item(0);
        var returnedData = formField.firstChild.nodeValue;

        var message = xhr.responseXML.getElementsByTagName("message").item(0);

        var success = message.firstChild.nodeValue == "valid";

        if (success) {
            $("labNo").value = returnedData;

        } else {
            alert("<%= MessageUtil.getMessage("error.accession.no.next") %>");
            $("labNo").value = "";
        }

        selectFieldErrorDisplay(success, $("labNo"));
        setValidIndicaterOnField(success, "labNo");

        setCorrectSave();


    	<c:if test="${param.attemptAutoSave}">
    		var validToSave =  patientFormValid() && sampleEntryTopValid();
    		if (validToSave) {
    			savePage();
    		}
    	</c:if>
    }

    function processCodeSuccess(xhr) {
        //alert(xhr.responseText);
        var code = xhr.responseXML.getElementsByTagName("code").item(0);
        var success = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue == "valid";

        if (success) {
            jQuery("#requesterCodeId").val(code.getAttribute("value"));
        }
    }
    
    function siteDepartmentSuccess (xhr) {
        console.log(xhr.responseText);
        var message = xhr.responseXML.getElementsByTagName("message").item(0).firstChild.nodeValue;
    	var departments = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[0].childNodes;
    	var selected = xhr.responseXML.getElementsByTagName("formfield").item(0).childNodes[1];
    	var isValid = message == "<%=IActionConstants.VALID%>";
    	var requesterDepartment = jQuery("#requesterDepartmentId");
    	var i = 0;

    	requesterDepartment.disabled = "";
    	if( isValid ){
    		requesterDepartment.children('option').remove();
    		requesterDepartment.append(new Option('', ''));
    		for( ;i < departments.length; ++i){
//     						is this supposed to be value value or value id?
    		requesterDepartment.append(
    				new Option(departments[i].attributes.getNamedItem("value").value, 
    					departments[i].attributes.getNamedItem("id").value));
    		}
    	}
    	
    	if( selected){
    		requesterDepartment.selectedIndex = getSelectIndexFor( "requesterDepartmentId", selected.childNodes[0].nodeValue);
    	}
    }

    function testLocationCodeChanged(element) {
        if (element.length - 1 == element.selectedIndex) {
            $("testLocationCodeOtherId").show();
        } else {
            $("testLocationCodeOtherId").hide();
            $("testLocationCodeOtherId").value = "";
        }
    }

    function setOrderModified(){
        jQuery("#orderModified").val("true");
        orderChanged = true;
        if( window.makeDirty ){ makeDirty(); }

        setCorrectSave();
    }
    
    function newProvider(){
    	jQuery("#providerPersonId").val("");
    	jQuery('#providerPersonId').next('input').val('');
    }

</script>


<%-- This define may not be needed, look at usages (not in any other jsp or js page may be radio buttons for ci LNSP--%>
<c:set var="sampleOrderItem" value="${sampleOrderItems}"/>

<form:hidden path="sampleOrderItems.modified" id="orderModified"/>
<form:hidden path="sampleOrderItems.sampleId" id="sampleId"/>

<div id="orderDisplay" >
<table style="width:100%">

<tr>
<td>
<table>
<c:if test="${empty form.sampleOrderItems.labNo}" >
    <tr>
        <td style="width:35%">
            <%=MessageUtil.getContextualMessage( "quick.entry.accession.number" )%>
            :
        </td>
        <td style="width:65%">
            <form:input path="sampleOrderItems.labNo"
                      maxlength='<%= Integer.toString(AccessionNumberUtil.getMaxAccessionLength())%>'
                      onchange="checkAccessionNumber(this);"
                      cssClass="text"
                      id="labNo"/>

            <spring:message code="sample.entry.scanner.instructions" htmlEscape="false"/>
            <input type="button" id="generateAccessionButton" value='<%=MessageUtil.getMessage("sample.entry.scanner.generate")%>'
                   onclick="setOrderModified();getNextAccessionNumber(); " class="textButton">
        </td>
    </tr>
</c:if>

<tr>
    <td><spring:message code="sample.entry.priority" htmlEscape="true"/> : </td>
    <td>    
        <form:select path="sampleOrderItems.priority" 
                    id="priorityId"  >
            <form:options items="${form.sampleOrderItems.priorityList}" itemValue="id" itemLabel="value"/>
        </form:select> 
    </td>
</tr>

<%-- <logic:notEmpty name="${formName}" property="sampleOrderItems.labNo" >
    <tr><td style="width:35%"></td><td style="width:65%"></td></tr>
</logic:notEmpty> --%>
<c:if test="${!empty form.sampleOrderItems.labNo }" >
	<form:hidden path="sampleOrderItems.labNo"/>
    <tr><td style="width:35%"></td><td style="width:65%"></td></tr>
</c:if>
<% if( FormFields.getInstance().useField( Field.SampleEntryUseRequestDate ) ){ %>
<tr>
    <td><spring:message code="sample.entry.requestDate"/>:
        <span class="requiredlabel">*</span><span
                style="font-size: xx-small; "><%=DateUtil.getDateUserPrompt()%></span></td>
    <td><form:input path="sampleOrderItems.requestDate"
                   id="requestDate"
                   cssClass="required"
                   onchange="setOrderModified();checkValidEntryDate(this, 'past')"
                   onkeyup="addDateSlashes(this, event);"
                   maxlength="10"/>
</tr>
<% } %>
<tr>
    <td>
        <%= MessageUtil.getContextualMessage( "quick.entry.received.date" ) %>
        :
        <span class="requiredlabel">*</span>
        <span style="font-size: xx-small; "><%=DateUtil.getDateUserPrompt()%>
        </span>
    </td>
    <td colspan="2">
         <form:input path="sampleOrderItems.receivedDateForDisplay"
                  onchange="setOrderModified();checkValidEntryDate(this, 'past');"
                  onkeyup="addDateSlashes(this, event);"
                  maxlength="10"
                  cssClass="text required"
                  id="receivedDateForDisplay"/> 

        <% if( FormFields.getInstance().useField( Field.SampleEntryUseReceptionHour ) ){ %>
        <spring:message code="sample.receptionTime"/>:
        <form:input                    onkeyup="filterTimeKeys(this, event);"
                   path="sampleOrderItems.receivedTime"
                   id="receivedTime"
                   maxlength="5"
                   onblur="setOrderModified(); checkValidTime(this, true);"/>

        <% } %>
    </td>
</tr>

<% if( FormFields.getInstance().useField( Field.SampleEntryNextVisitDate ) ){ %>
<tr>
    <td><spring:message code="sample.entry.nextVisit.date"/>&nbsp;<span style="font-size: xx-small; ">
        <%=DateUtil.getDateUserPrompt()%>
    </span>:
    </td>
    <td>
        <form:input path="sampleOrderItems.nextVisitDate"
                   onchange="setOrderModified();checkValidEntryDate(this, 'future', true)"
                   onkeyup="addDateSlashes(this, event);"
                   id="nextVisitDate"
                   maxlength="10"/>
    </td>
</tr>
<% } %>

<tr class="spacerRow">
    <td>&nbsp;</td>
</tr>
<% if( FormFields.getInstance().useField( Field.SampleEntryRequestingSiteSampleId ) ){%>
<tr>
    <td>
        <%= MessageUtil.getContextualMessage( "sample.clientReference" ) %>:
    </td>
    <td>
         <form:input path="sampleOrderItems.requesterSampleID"
                  size="50"
                  maxlength="50"
                  onchange="setOrderModified();"/> 
    </td>
    <td style="width:10%">&nbsp;</td>
    <td style="width:45%">&nbsp;</td>
</tr>
<% } %>
<% if( FormFields.getInstance().useField( Field.SAMPLE_ENTRY_USE_REFFERING_PATIENT_NUMBER ) ){%>
<tr>
    <td>
        <%= MessageUtil.getContextualMessage( "sample.referring.patientNumber" ) %>:
    </td>
    <td>
        <form:input path="sampleOrderItems.referringPatientNumber"
                  id="referringPatientNumber"
                  size="50"
                  maxlength="50"
                  onchange="setOrderModified();"/> 
    </td>
    <td style="width:10%">&nbsp;</td>
    <td style="width:45%">&nbsp;</td>
</tr>
<% } %>
<% if( useReferralSiteList ){ %>
<tr>
    <td>
        <%= MessageUtil.getContextualMessage( "sample.entry.project.siteName" ) %>:
        <% if( FormFields.getInstance().useField( Field.SampleEntryReferralSiteNameRequired ) ){%>
        <span class="requiredlabel">*</span>
        <% } %>
    </td>
    <td colspan="3" >
    	<c:if test="${restrictNewReferringSiteEntries}" >
    	
    		<spring:message code="error.site.invalid" var="invalidSite"/>
    	    <spring:message code="sample.entry.project.siteMaxMsg" var="siteMaxMessage"/>
    		<form:select path="sampleOrderItems.referringSiteId" 
    				 id="requesterId" 
                     onkeyup="capitalizeValue( this.value );"
                     capitalize="true"
                     invalidlabid='${invalidSite}'
                     maxrepmsg='${siteMaxMessage}'
                     disabled="<%=!restrictNewReferringSiteEntries%>"
       				 clearNonMatching="<%=restrictNewReferringSiteEntries%>"
                      >
            <option ></option>
            <form:options items="${form.sampleOrderItems.referringSiteList}" itemValue="id" itemLabel="value"/>
            </form:select>
    	</c:if>
    	<c:if test="${not restrictNewReferringSiteEntries}" >
            <form:input id="requesterName" path="sampleOrderItems.referringSiteName"  style="width:300px" onchange="setOrderModified();makeDirty()"/>
    	</c:if>
    </td>
</tr>

	<% if( useSiteDepartment ){ %>
	<tr>
	    <td>
	        <%= MessageUtil.getContextualMessage( "sample.entry.project.siteDepartmentName" ) %>:
	        <% if( FormFields.getInstance().requireField( Field.SITE_DEPARTMENT ) ){%>
	        <span class="requiredlabel">*</span>
	        <% } %>
	    </td>
	    <td colspan="3" >
	    	<c:if test="${form.sampleOrderItems.readOnly == false}" >
	    		<form:select path="sampleOrderItems.referringSiteDepartmentId" 
	    				 id="requesterDepartmentId" 
	                     onchange="setOrderModified();setCorrectSave();"
                         disabled="<%=!restrictNewReferringSiteEntries%>"
	                     onkeyup="capitalizeValue( this.value );" >
	            <option value="0" ></option>
	            <form:options items="${form.sampleOrderItems.referringSiteDepartmentList}" itemValue="id" itemLabel="value"/>
	            </form:select>
	    	</c:if>
	    	<c:if test="${form.sampleOrderItems.readOnly}" >
	            <form:input path="sampleOrderItems.referringSiteDepartmentName"  style="width:300px" />
	    	</c:if>
	    </td>
	</tr>
	<% } %>
<% } %>
<% if( useReferralSiteCode ){ %>
<tr>
    <td>
        <%= MessageUtil.getContextualMessage( "sample.entry.referringSite.code" ) %>:
    </td>
    <td>
        <form:input path="sampleOrderItems.referringSiteCode"
                   onchange="setOrderModified();setCorrectSave();"
                   id="requesterCodeId"/>
    </td>
</tr>
<% } %>
<% if( ConfigurationProperties.getInstance().isPropertyValueEqual( Property.ORDER_PROGRAM, "true" )){ %>
<tr class="spacerRow">
    <td>&nbsp;</td>
</tr>
<tr>
    <td><spring:message code="label.program"/>:</td>
    <td>
    	<form:select path="sampleOrderItems.program" onchange="setOrderModified();" >
    		<form:options items="${form.sampleOrderItems.programList}" itemValue="id" itemLabel="value" />
    	</form:select>
        <%-- <html:select name="${formName}" property="sampleOrderItems.program" onchange="setOrderModified();" >
            <logic:iterate id="optionValue" name='${formName}' property="sampleOrderItems.programList"
                           type="IdValuePair">
                <option value='<%=optionValue.getId()%>' <%=optionValue.getId().equals(sampleOrderItem.getProgram() ) ? "selected='selected'" : ""%>>
                    <bean:write name="optionValue" property="value"/>
                </option>
            </logic:iterate>
        </html:select> --%>
    </td>
</tr>
<% } %>
<tr class="spacerRow">
    <td>&nbsp;</td>
</tr>
<% if( useProviderInfo ){ %>
<tr class="provider-info-row">
    <td>
        <%= MessageUtil.getContextualMessage( "sample.entry.provider" ) %>:
        <% if( FormFields.getInstance().useField( Field.SampleEntryRequesterPersonRequired ) && restrictNewProviderEntries ){%>
        <span class="requiredlabel">*</span>
        <% } %>
    </td>
    <td><spring:message code="error.provider.ininvalid" var="invalidProvider"/>
    	<form:select id="providerPersonId" path="sampleOrderItems.providerPersonId" 
                     capitalize="false"
                     invalidlabid='${invalidProvider}'
       				 clearNonMatching="true"
                     maxrepmsg='maximum reached'
                      >
    		<option value=""></option>
    		<form:options items="${form.sampleOrderItems.providersList}" itemValue="id" itemLabel="value" />
    	</form:select>
    </td> 
</tr>

<tr class="provider-info-row provider-extra-info-row">
<td>
        <%= MessageUtil.getContextualMessage( "sample.entry.provider.name" ) %>:
        <% if( FormFields.getInstance().useField( Field.SampleEntryRequesterPersonRequired ) && !restrictNewProviderEntries ){%>
        <span class="requiredlabel">*</span>
        <% } %>
    </td>
<td>
        <form:input path="sampleOrderItems.providerLastName"
                   id="providerLastNameID"
                   onchange="setOrderModified();setCorrectSave();"
                   size="30"
                   onkeyup="newProvider();"
                  disabled="<%=restrictNewProviderEntries%>"/>
    </td> 

</tr >              
<tr class="provider-info-row provider-extra-info-row">
    <td>
        <spring:message code="sample.entry.provider.firstName"/>:
        <% if( FormFields.getInstance().useField( Field.SampleEntryRequesterPersonRequired ) && !restrictNewProviderEntries ){%>
        <span class="requiredlabel">*</span>
        <% } %>
	</td>
    <td>
        <form:input path="sampleOrderItems.providerFirstName"
                   id="providerFirstNameID" 
                   onchange="setOrderModified();"
                   size="30"
                   onkeyup="newProvider();"
                  disabled="<%=restrictNewProviderEntries%>"/>
    </td>
</tr>
<tr class="provider-info-row provider-extra-info-row">
    <td>
        <%= MessageUtil.getContextualMessage( "humansampleone.provider.workPhone" ) + ": " + PhoneNumberService.getPhoneFormat()%>
    </td>
    <td>
         <form:input path="sampleOrderItems.providerWorkPhone"
                  id="providerWorkPhoneID"
                  size="30"
                  disabled="<%=restrictNewProviderEntries%>"
                  maxlength="30"
                   onkeyup="newProvider();"
                  cssClass="text"
                  onchange="setOrderModified();validatePhoneNumber(this)"/> 
    </td>
</tr>
<% } %>
<% if( FormFields.getInstance().useField( Field.SampleEntryProviderFax ) ){ %>
<tr>
    <td>
        <%= MessageUtil.getContextualMessage( "sample.entry.project.faxNumber" )%>:
    </td>
    <td>
        <form:input path="sampleOrderItems.providerFax"
                  id="providerFaxID"
                  size="20"
                   onkeyup="newProvider();"
                  disabled="<%=restrictNewProviderEntries%>"
                  cssClass="text"
                  onchange="setOrderModified();makeDirty()"/> 
    </td>
</tr>
<% } %>
<% if( FormFields.getInstance().useField( Field.SampleEntryProviderEmail ) ){ %>
<tr>
    <td>
        <%= MessageUtil.getContextualMessage( "sample.entry.project.email" )%>:
    </td>
    <td> 
        <form:input path="sampleOrderItems.providerEmail"
                  id="providerEmailID"
                  size="20"
                   onkeyup="newProvider();"
                  disabled="<%=restrictNewProviderEntries%>"
                  cssClass="text"
                  onchange="setOrderModified();makeDirty()"/> 
    </td>   
<% } %>
<% if( FormFields.getInstance().useField( Field.SampleEntryHealthFacilityAddress ) ){%>
<tr>
    <td><spring:message code="sample.entry.facility.address"/>:</td>
</tr>
<tr>
    <td>&nbsp;&nbsp;<spring:message code="sample.entry.facility.street"/>
    <td>
        <form:input 
                   path="sampleOrderItems.facilityAddressStreet"
                   cssClass="text"
                   onchange="setOrderModified();makeDirty()"/>
    </td>
</tr>
<tr>
    <td>&nbsp;&nbsp;<spring:message code="sample.entry.facility.commune"/>:
    <td>
        <form:input
                   path="sampleOrderItems.facilityAddressCommune"
                   cssClass="text"
                   onchange="setOrderModified();makeDirty()"/>
    </td>
</tr>
<tr>
    <td><spring:message code="sample.entry.facility.phone"/>:
    <td>
        <form:input               path="sampleOrderItems.facilityPhone"
                   cssClass="text"
                   maxlength="17"
                   onchange="setOrderModified(); validatePhoneNumber( this );"/>
    </td>
</tr>
<tr>
    <td><spring:message code="sample.entry.facility.fax"/>:
    <td>
        <form:input 
                   path="sampleOrderItems.facilityFax"
                   cssClass="text"
                   onchange="setOrderModified();makeDirty()"/>
    </td>
</tr>
<% } %>
<tr class="spacerRow">
    <td>&nbsp;</td>
</tr>
<% if( trackPayment ){ %>
<tr>
    <td><spring:message code="sample.entry.patientPayment"/>:</td>
    <td>
        <form:select path="sampleOrderItems.paymentOptionSelection" onchange="setOrderModified();" >
            <option value=''></option>
            <form:options items="${form.sampleOrderItems.paymentOptions}" itemLabel="value" itemValue="id"/>
        </form:select>
    </td>
</tr>
<% } %>
<tr>
<!-- turn off for release 2.2.3.1 gnr -->
<% if( !ConfigurationProperties.getInstance().isPropertyValueEqual( Property.USE_BILLING_REFERENCE_NUMBER, "true" )){ %>
    <td><label for="billingReferenceNumber">
    	<c:out value="${billingReferenceNumberLabel}"/>
    </label>
    </td>
    <td>
        <form:input path="sampleOrderItems.billingReferenceNumber"
                    cssClass="text"
                    id="billingReferenceNumber"
                    onchange="setOrderModified();makeDirty()" />
    </td>
</tr>
<% } %>
<% if( ConfigurationProperties.getInstance().isPropertyValueEqual( Property.CONTACT_TRACING, "true" )){ %>
<tr>
    <td><label for="contactTracingIndexName">
    	<spring:message code="field.contacttracing.indexname.label" />
    </label>
    </td>
    <td>
        <form:input path="sampleOrderItems.contactTracingIndexName"
                    cssClass="text"
                    id="contactTracingIndexName"
                    onchange="setOrderModified();makeDirty()" />
    </td>
</tr>
<tr>
    <td><label for="contactTracingIndexRecordNumber">
    	<spring:message code="field.contacttracing.indexrecordnumber.label" />
    </label>
    </td>
    <td>
        <form:input path="sampleOrderItems.contactTracingIndexRecordNumber"
                    cssClass="text"
                    id="contactTracingIndexRecordNumber"
                    onchange="setOrderModified();makeDirty()" />
    </td>
</tr>
<% } %>
<% if( FormFields.getInstance().useField( Field.TEST_LOCATION_CODE ) ){%>
<tr>
    <td><spring:message code="sample.entry.sample.period"/>:</td>
    <td>
        <form:select
                     path="sampleOrderItems.testLocationCode"
                     onchange="setOrderModified(); testLocationCodeChanged( this )"
                     id="testLocationCodeId">
            <option value=''></option>
            <form:options items="${form.sampleOrderItems.testLocationCodeList}" itemLabel="value" itemValue="id"/>
        </form:select>
        &nbsp;
        <form:input
                   path="sampleOrderItems.otherLocationCode"
                   id="testLocationCodeOtherId"
                   style='display:none'
                    />
    </td>
</tr>
<% } %>
<tr class="spacerRow">
    <td>
        &nbsp;
    </td>
</tr>
</table>
</td>
</tr>
</table>
</div>

<script type="text/javascript">

    <% if( FormFields.getInstance().useField( Field.TEST_LOCATION_CODE ) ){%>
    function showTestLocationCode(){
            if(( jQuery("#testLocationCodeId option").length -1 ) == jQuery("#testLocationCodeId option:selected").index() ){
                jQuery("#testLocationCodeOtherId").show();
            }
    }
    <% } %>
    
    function siteListChanged(siteList) {
        var siteList = $("requesterId");
        //if the index is 0 it is a new entry, if it is not then the textValue may include the index value
        // create new entry has been removed gnr
        if (siteList.selectedIndex == 0) {
//             $("newRequesterName").value = textValue;
        } else if (useReferralSiteCode) {
            getCodeForOrganization(siteList.options[siteList.selectedIndex].value, processCodeSuccess);
        }

    	if ( useSiteDepartment ) {
    		if(document.getElementById("requesterId").selectedIndex != 0){
    			getDepartmentsForSiteClinic( document.getElementById("requesterId").value, "", siteDepartmentSuccess, null);
    		} 
    	}
    }

    function parseRequesterPerson(xhr) {
    	var requester = JSON.parse(xhr.responseText);
            $("providerPersonId").value = requester.id?requester.id:"";
            $("providerFirstNameID").value = requester.firstName?requester.firstName:"";
            $("providerLastNameID").value = requester.lastName?requester.lastName:"";    
            $("providerWorkPhoneID").value = requester.workPhone?requester.workPhone:"";
            $("providerFaxID").value = requester.fax?requester.fax:"";
            $("providerEmailID").value = requester.email?requester.email:"";
    }

    function setSelectComboboxToId(selectId, selectVal) {
    	jQuery("#" + selectId).val(selectVal).change();
    	jQuery("#" + selectId).parent().find("input").val(jQuery("#" + selectId).find(":selected").text());
    	autocompleteResultCallBack(selectId, selectVal)
    }
    
    jQuery(document).ready(function () {
        var dropdown = jQuery("select#requesterId");
        autoCompleteWidth = dropdown.width() + 66 + 'px';
        // Actually executes autocomplete
        <% if(restrictNewReferringSiteEntries ){%>
            if (typeof dropdown.combobox === 'function') {
                dropdown.combobox()
            }
         <% } %>
        var providerDropdown = jQuery("select#providerPersonId");
        autoCompleteWidth = providerDropdown.width() + 66 + 'px';
        // Actually executes autocomplete
       
<%--         <% if(restrictNewProviderEntries ){%> --%>
            if (typeof providerDropdown.combobox === 'function') {
                providerDropdown.combobox();
            }
<%--         <% } %> --%>

        autocompleteResultCallBack = function (selectId, value) {
        	if (selectId === 'requesterId') {
        		requesterId = value;
        		siteListChanged(requesterId);
                setOrderModified();
                setCorrectSave();
        	} else if (selectId === 'providerPersonId') {
        		personId = value;
        		if (personId) {
        			getProviderInfoByPersonId(personId, parseRequesterPerson );
        		} else {
       		 	$("providerFirstNameID").value = '';
       		 	$("providerLastNameID").value = '';
       		 	$("providerPersonId").value = '';
       		 	$("providerWorkPhoneID").value = '';
       		 	$("providerEmailID").value = '';
       		 	$("providerFaxID").value = '';
        		}

                setOrderModified();
                setCorrectSave();
        	}
        	
        }

        if (dropdown.val()) {
        	setSelectComboboxToId('requesterId', dropdown.val());
        }
        if (providerDropdown.val()) {
        	setSelectComboboxToId('providerPersonId', providerDropdown.val());
        }
        
        
        <% if( FormFields.getInstance().useField( Field.TEST_LOCATION_CODE ) ){%>
            showTestLocationCode();
        <% } %>
    });

</script>

