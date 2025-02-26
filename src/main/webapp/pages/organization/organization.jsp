<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.formfields.FormFields,
			org.openelisglobal.common.formfields.FormFields.Field,
			org.openelisglobal.internationalization.MessageUtil" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>




<div id="sound"></div>

 
<%
	boolean useOrgLocalAbbrev = FormFields.getInstance().useField( Field.OrgLocalAbrev);
	boolean useState = FormFields.getInstance().useField( Field.OrgState);
	boolean useZip = FormFields.getInstance().useField( Field.ZipCode);
	boolean useMLS = FormFields.getInstance().useField( Field.MLS);
	boolean useInlineOrganizationTypes = FormFields.getInstance().useField(Field.InlineOrganizationTypes);
	boolean useAddressInfo = FormFields.getInstance().useField(Field.OrganizationAddressInfo);
	boolean useCLIA = FormFields.getInstance().useField(Field.OrganizationCLIA);
	boolean useParent = FormFields.getInstance().useField(Field.OrganizationParent);
	boolean useShortName = FormFields.getInstance().useField(Field.OrganizationShortName);
	boolean useMultiUnit = FormFields.getInstance().useField(Field.OrganizationMultiUnit);
	boolean showId = FormFields.getInstance().useField(Field.OrganizationOrgId);
	boolean showCity = FormFields.getInstance().useField(Field.ADDRESS_CITY );
	boolean showDepartment = FormFields.getInstance().useField(Field.ADDRESS_DEPARTMENT );
	boolean showCommune= FormFields.getInstance().useField(Field.ADDRESS_COMMUNE );
	boolean showVillage = FormFields.getInstance().useField(Field.ADDRESS_VILLAGE );

%>

<script>
var useOrganizationTypes = <%= useInlineOrganizationTypes %>;
function validateForm(form) {
	if( useOrganizationTypes ){
		var orgTypeList = $$(".orgTypeId");

		for( var i = 0; i < orgTypeList.length; ++i ){
			if(orgTypeList[i].checked ){
				//TODO add proper javascript validation
 				//return validateOrganizationForm(form);
				return true;
			}
		}
		alert("<%= MessageUtil.getMessage("error.organizationType.required")%>");
		return false;
	}

	//TODO add proper javascript validation
	return true;
 	//return validateOrganizationForm(form);
}
</script>

<table>
		<% if(showId){ %>
		<tr>
						<td class="label">
							<spring:message code="organization.id"/>:
						</td>
						<td>
							<form:input path="id" readonly="readonly"/>
						</td>
		</tr>
		<% }  else { %>
			<form:hidden path="id" />
		<% } %>
		<% if( useParent ){ %>
		<tr>
			<td class="label">
				<spring:message code="organization.parent"/>:
			</td>
			<td>
	   			<form:input path="parentOrgName" id="parentOrgName" size="30" />
	   			<span id="indicator1" style="display:none;"><img src="images/indicator.gif"/></span>

	   			<input id="selectedOrgId" name="selectedOrgId" type="hidden" size="30" />
<%-- 	   			<form:select path="selectedOrgId"> --%>
<%-- 	   				<form:options items="${form.activeParentOrgs}" itemLabel="organizationName" itemValue="id"/> --%>
<%-- 	   			</form:select> --%>
							<%--html:text id="organizationName" cssClass="form-autocomplete" size="30" name="${form.formName}" property="organizationName" /> &nbsp;&nbsp;&nbsp;&nbsp;Org ID: <input id="selectedOrgId" name="selectedOrgId" type="text" size="30" /--%>

			</td>
		</tr>
		<% } %>
		<% if( useOrgLocalAbbrev ){ %>
		<tr>
						<td class="label">
						    <spring:message code="organization.localAbbreviation"/>:<span class="requiredlabel">*</span>
						</td>
						<td>
						 	<form:input path="organizationLocalAbbreviation" />
						</td>
		</tr>
		<% } %>
		<tr>
						<td class="label">
							<spring:message code="organization.organizationName"/>:<span class="requiredlabel">*</span>
						</td>
						<td>
							<form:input path="organizationName" />
						</td>
		</tr>
		<% if(useShortName){ %>
		<tr>
						<td class="label">
						    <%= MessageUtil.getContextualMessage("organization.short") %>
						</td>
						<td>
						 	<form:input path="shortName" />
						</td>
		</tr>
		<% } %>
        <tr>
						<td class="label">
							<spring:message code="organization.isActive"/>:<span class="requiredlabel">*</span>
						</td>
						<td width="1">
							<form:input path="isActive" size="1" onblur="this.value=this.value.toUpperCase()"/>
						</td>
		</tr>
		<% if( useAddressInfo){ %>
		<% if(useMultiUnit){ %>
		<tr>
						<td class="label">
							<spring:message code="organization.multipleUnit"/>:
						</td>
						<td>
							<form:input path="multipleUnit" />
						</td>
		</tr>
		<% } %>
		<tr>
						<td class="label">
							<spring:message code="organization.streetAddress"/>:
						</td>
						<td>
						   	<form:input path="streetAddress" cssClass="text" size="35"/>
						</td>
		</tr>
		<% if( showCity){ %>
		<tr>
						<td class="label">
							<spring:message code="organization.city"/>:
						</td>
						<td>
							<form:input path="city" id="city" size="30" />
				   			<span id="indicator2" style="display:none;"><img src="images/indicator.gif"/></span>
							<input id="cityID" name="cityID" type="hidden" size="30" />
						</td>
		</tr>
	    <% } %>
		<% if( showDepartment ){ %>
		<tr>
			<td class="label">
				<spring:message code="organization.department" />
			</td>
			<td>
				<form:select path="department" >
					<option value="0"></option>
				<form:options items="${form.departmentList}" itemValue="id" itemLabel="dictEntry"/>
				</form:select>
			</td>
		</tr>
		<% } %>
		<% if( showCommune){ %>
		<tr>
			<td class="label">
				<spring:message code="organization.commune" />
			</td>
			<td>
				<form:input path="commune" />
			</td>
		</tr>
		<% } %>
		<% if( showVillage){ %>
		<tr>
			<td class="label">
				<spring:message code="organization.village" />
			</td>
			<td>
				<form:input path="village" />
			</td>
		</tr>
		<% } %>
		<% if( useState ){ %>
		<tr>
						<td class="label">
							<spring:message code="organization.state"/>:<span class="requiredlabel">*</span>
						</td>
						<td>
							<%--html:text property="state" /--%>

						  <form:select path="state">
						  <option></option>
					   	  <form:options items="${form.states}"
										itemLabel="state"
										itemValue="state"  />

                       	   </form:select>
						</td>
		</tr>
		<% } %>
		<% if( useZip ){ %>
		<tr>
						<td class="label">
							<spring:message code="organization.zipCode"/>:<span class="requiredlabel">*</span>
						</td>
						<td>
							<form:input path="zipCode" />
						</td>
		</tr>
		<% } %>
		<% } %>
		<tr>
						<td class="label">
							<spring:message code="organization.internetAddress"/>:
						</td>
						<td>
							<form:input path="internetAddress" />
						</td>
		</tr>
		<% if( useMLS ) { %>
		<tr>
						<td class="label">
							<spring:message code="organization.mls.sentinelLab"/>:<span class="requiredlabel">*</span>
						</td>
						<td>
							<form:input path="mlsSentinelLabFlag" />
						</td>
		</tr>
		<% } %>
		<% if( useCLIA ){ %>
		<tr>
						<td class="label">
							<spring:message code="organization.clia.number"/>:
						</td>
						<td>
							<form:input path="cliaNum" />
						</td>
		</tr>
		<% } %>
		<% if( useMLS ){ %>
		<tr>
						<td class="label">
							<spring:message code="organization.mls.lab"/>:<span class="requiredlabel">*</span>
						</td>
						<td>
							<form:input path="mlsLabFlag" onblur="this.value=this.value.toUpperCase()"/>
						</td>
		</tr>
		<% } %>
 		<tr>
		<td>&nbsp;</td>
		</tr>
</table>
<% if( useInlineOrganizationTypes ){ %>

<h3><%=MessageUtil.getContextualMessage("organization.type")%><span class="requiredlabel">*</span></h3><br/>

<table >
	<tr>
		<th >&nbsp;</th>
		<th ><spring:message code="generic.name"/></th>
	</tr>
<c:forEach items="${form.orgTypes}" var="organizationType" >
	<tr>
	<td>
		<form:checkbox path="selectedTypes" cssClass="orgTypeId" value="${organizationType.id}"/>
	</td>
	<td>
		<c:out value="${organizationType.localizedName}"  />&nbsp;
	</td>
	</tr>
	</c:forEach>
</table>


<% } %>
<% if( useAddressInfo){ %>
  <ajax:autocomplete
  source="city"
  target="cityID"
  baseUrl="ajaxAutocompleteXML"
  className="autocomplete"
  parameters="city={city},provider=CityAutocompleteProvider,fieldName=city,idName=id"
  indicator="indicator2"
  minimumCharacters="1"
  />
<% } %>
<% if( useParent){ %>
  <ajax:autocomplete
  source="parentOrgName"
  target="selectedOrgId"
  baseUrl="ajaxAutocompleteXML"
  className="autocomplete"
  parameters="organizationName={parentOrgName},provider=OrganizationAutocompleteProvider,fieldName=organizationName,idName=id"
  indicator="indicator1"
  minimumCharacters="1" />
<% } %>



