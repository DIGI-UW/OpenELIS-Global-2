<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.provider.validation.PasswordValidationFactory,
		    org.openelisglobal.common.util.Versioning,
			org.openelisglobal.role.action.bean.DisplayRole,
			org.openelisglobal.internationalization.MessageUtil" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<link rel="stylesheet" href="css/jquery_ui/jquery.ui.all.css?">
<link rel="stylesheet" href="css/customAutocomplete.css?">

<script src="scripts/ui/jquery.ui.core.js?"></script>
<script src="scripts/ui/jquery.ui.widget.js?"></script>
<script src="scripts/ui/jquery.ui.button.js?"></script>
<script src="scripts/ui/jquery.ui.menu.js?"></script>
<script src="scripts/ui/jquery.ui.position.js?"></script>
<script src="scripts/ui/jquery.ui.autocomplete.js?"></script>
<script src="scripts/customAutocomplete.js?"></script>
<script type="text/javascript" src="scripts/utilities.js?" ></script>


<script>

jQuery(document).ready( function() {
	var input = jQuery( ":input" );
	document.getElementsByName("save")[0].disabled = true;
	document.getElementById("copyUserPermisions").disabled = true;	
	input.change( function( objEvent ){
		document.getElementsByName("save")[0].disabled = false;
	} );
	renderUserRolesData();
	createDefaultRolesTable();
	makeSystemUserAutoComplete();	
});

function validateForm(form) {
 	 return checkRequiredElements( true );
 }
 
function /*boolean*/ checkRequiredElements( ){
 	var elements = $$(".required");
 	var ok = true;
 	
 	for( var i = 0; i < elements.length; ++i){
 		if( elements[i].value.blank() ){
 			elements[i].style.borderColor = "red";
 			ok = false;
 		}
 	}
 
 	if($("password1").value != $("password2").value ){
 		$("password1").style.borderColor = "red";
 		$("password2").style.borderColor = "red";

 		ok = false;
  	}
 	
 	return ok; 
}
 
function handlePassword1( password1 ){
	var password2 = $("password2");
	password2.value = "";
	password2.style.borderColor = "";
	password1.style.borderColor = "";
} 

function handlePassword2( password2 ){
 	var password1 = $("password1");
 	
 	if( !password1.value.blank() && !password2.value.blank() && password1.value != password2.value ){
 		password2.style.borderColor = "red";
		password1.style.borderColor = "red";
 		alert( '<%= MessageUtil.getMessage("errors.password.match")%>');
 	}else{
 		password2.style.borderColor = "";
		password1.style.borderColor = "";
 	}
}

function /*void*/ selectChildren(selection, childList ){
	if( childList ){
		isChecked = selection.checked;

		children = childList.split('_');

		for( i = 0; i < children.length; i++ ){
			$( "role_" + children[i] ).checked = isChecked;
		}

	}
}

function /*void*/ makeDirty(){
	dirty=true;
	if( typeof(showSuccessMessage) === 'function' ){
		showSuccessMessage(false); //refers to last save
	}

	// Adds warning when leaving page if content has been entered into makeDirty form fields
	function formWarning(){ 
    return "<spring:message code="banner.menu.dataLossWarning"/>";
	}
	window.onbeforeunload = formWarning;
}

function /*void*/ requiredFieldUpdated( field){
	field.style.borderColor = field.value.blank() ? "red" : "";
}

function mySaveAction() {
	window.onbeforeunload = null;
	document.getElementById("mainForm").submit();
}

</script>
<script>
 	var counter = 0;

	/* This creates a New set of Lab Unit Roles and appends a common preffix to the Roles values in every new Set in order
	 to identify the distinct values for each set,since multiple fields mapped to the same path are be created */
	 
	function createNewRolesTable() {
		counter++
		var content = document.getElementById('rolesTable').innerHTML;
		var newTable = document.createElement('table');
		var tableId = "rolesTable_" + counter;
		newTable.id = tableId
		newTable.innerHTML = content;
		document.getElementById('rolesRow').appendChild(newTable);

		var rows = document.getElementById(tableId).rows;

		for (var i = 0; i < rows.length; i++) {
			if (rows[i].cells[1]) {
				if (i == 0) {
					var options = rows[i].cells[1].getElementsByTagName("option");
					for (var x = 0; x < options.length; x++) {
						var optionsValue = options[x].value
						rows[i].cells[1].getElementsByTagName("option")[x].value = counter + "=" + optionsValue;
					}
				}
				if (i > 0) {
					var val = rows[i].cells[1].getElementsByTagName("input")[0].value;
					rows[i].cells[1].getElementsByTagName("input")[0].value = counter + "=" + val;
				}
			}
		}


		var rolesSection = document.getElementById("rolesRow");
		var tables = rolesSection.getElementsByTagName("table");
		if (tables.length <= 2) {
			document.getElementById("createNewRoles").disabled = true;
		}

		if (tables.length > 2) {
			for (var y = 2; y < tables.length; y++) {
				var allLabUnitsOption = tables[y].rows[0].cells[1].getElementsByTagName("option")[0];
				allLabUnitsOption.remove();
			}
		}
	}

	function removeRolesTable(element) {
		var tableToRemove = element.parentNode.parentNode.parentNode.parentNode;
		tableToRemove.remove();
	}

	//Renders an empty set of Lab Unit Roles if no user roles data exists
	function createDefaultRolesTable() {
		if (Object.keys(userRolesData).length === 0) {
			createNewRolesTable();
		}
	}

	function activateSave() {
		document.getElementsByName('save')[0].disabled = false;
	}

	function selectAllRoles(element) {
		var table = element.parentNode.parentNode.parentNode.parentNode;
		var rows = table.rows;
		for (var y = 2; y < rows.length; y++) {
			var checkBox = rows[y].cells[1].getElementsByTagName("input")[0];
			if (element.checked == true) {
				checkBox.checked = true
			} else {
				checkBox.checked = false
			}
		}
	}

	function checkAdminRoles() {
	var globalRoles = document.querySelectorAll('[id^="role_"]');
	var adminRole = document.getElementById('role_1');
	if (adminRole.checked == true) {
		for (var y = 0; y < globalRoles.length; y++) {
			if (globalRoles[y].id != "role_1") {
				globalRoles[y].checked = true;
			}
		}
	}
   };

	function disableAddRoles() {
		var rolesTable1 = document.getElementById("rolesTable_1");
		var selectedLabUnit = rolesTable1.rows[0].cells[1].getElementsByTagName("select")[0];
		var value = selectedLabUnit.options[selectedLabUnit.selectedIndex].value;
		if (value == "1=AllLabUnits") {
			alert("<spring:message code="systemuserrole.select.allLabUnits.warning"/>");
			document.getElementById("createNewRoles").disabled = true;
		}else {
			document.getElementById("createNewRoles").disabled = false;
		}
	}

</script>

<script>
var userRolesData = JSON.parse('${form.userLabRoleData}' != '' ? '${form.userLabRoleData}' : '{}');
//console.log(userRolesData);

// this dynamically Renders sets of Lab Unit Roles with data if user roles data exists			 
function renderUserRolesData() {
	for (let labSection in userRolesData) {
		counter++
		var content = document.getElementById('rolesTable').innerHTML;
		var newTable = document.createElement('table');
		var tableId = "rolesTable_" + counter;
		newTable.id = tableId
		newTable.innerHTML = content;
		document.getElementById('rolesRow').appendChild(newTable);
		var rows = document.getElementById(tableId).rows;

		var data = userRolesData[labSection]
		for (var y = 0; y < data.length; y++) {
			for (var i = 0; i < rows.length; i++) {
				if (rows[i].cells[1]) {
					if (i == 0) {
						var options = rows[i].cells[1].getElementsByTagName("option");
						for (var x = 0; x < options.length; x++) {
							var optionsValue = options[x].value
							if (optionsValue == labSection) {
								options[x].selected = "selected";
							}
						}
					}
					if (i > 0) {
						var testCheck = rows[i].cells[1].getElementsByTagName("input")[0];
						var testVal = testCheck.value;
						if (testVal == data[y]) {
							testCheck.checked = "checked";
						}

					}
				}
			}
		}

		for (var i = 0; i < rows.length; i++) {
			if (rows[i].cells[1]) {
				if (i == 0) {
					var options = rows[i].cells[1].getElementsByTagName("option");
					for (var x = 0; x < options.length; x++) {
						var optionsValue = options[x].value
						options[x].value = counter + "=" + optionsValue;
					}
				}
				if (i > 0) {
					var testCheck = rows[i].cells[1].getElementsByTagName("input")[0];
					var testVal = testCheck.value;
					testCheck.value = counter + "=" + testVal;
				}
			}
		}
		if (labSection == "AllLabUnits") {
			document.getElementById("createNewRoles").disabled = true;
		}
	}
}
</script>
<script>
function makeSystemUserAutoComplete() {
	jQuery('#systemUserToCopySelector').autocomplete({
		source: JSON.parse('${form.systemUsers}') ,
		focus: function(event, ui) {
			event.preventDefault();
			jQuery(this).val(ui.item.label);
		},
		select: function(event, ui) {
			event.preventDefault();
			jQuery(this).val(ui.item.label);
			jQuery('#systemUserToCopy').val(ui.item.value);
		}
	});
}
function copyPermisions(){
	if(window.confirm('<spring:message code="systemuserrole.warning.replace"/>')){
	  document.getElementById("allowCopyUserRoles").value = "Y";
      mySaveAction() ;
	}	
}

function activateCopyPermisions(){
	 document.getElementById("copyUserPermisions").disabled = false;	
}
function handleCopyPermisions(element){
	if(element.value == ""){
       document.getElementById("copyUserPermisions").disabled = true;
	} 	
}
</script>
<form:hidden path="systemUserId"/>
<form:hidden path="loginUserId"/>
<%-- <form:hidden path="systemUserLastupdated"/> --%>
<table >
		<tr>
						<td class="label" >
							<spring:message code="login.login.name"/> <span class="requiredlabel">*</span>
						</td>
						<td >
							<form:input path="userLoginName" cssClass='required' onchange="requiredFieldUpdated( this); makeDirty(); "/>
						</td>
		</tr>
		<tr>
			<td colspan="2">
				<%=PasswordValidationFactory.getPasswordValidator().getInstructions() %>
			</td>
		</tr>
		<tr>
						<td class="label">
							<spring:message code="login.password"/> <span class="requiredlabel">*</span>
						</td>
						<td>
							<form:password path="userPassword" 
										   id="password1" 
										   cssClass='required'
										   showPassword="true"
										   onchange="handlePassword1(this);  makeDirty();"/>
						</td>
		</tr>
		<tr>
						<td class="label">
							<spring:message code="login.repeat.password" /> <span class="requiredlabel">*</span>
						</td>
						<td>
							<form:password path="confirmPassword"  
							               id="password2" 
							               cssClass='required'
							               showPassword="true"
							               onchange="handlePassword2(this); makeDirty();" />
						</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
		</tr>
		<tr>
						<td class="label">
							<spring:message code="person.firstName" /> <span class="requiredlabel">*</span>
						</td>
						<td>
							<form:input path="userFirstName" 
							          cssClass='required' 
							          onchange="requiredFieldUpdated( this); makeDirty();"/>
						</td>
		</tr>
		<tr>
						<td class="label">
							<spring:message code="person.lastName" /> <span class="requiredlabel">*</span>
						</td>
						<td>
							<form:input path="userLastName" 
							          cssClass='required' onchange="requiredFieldUpdated( this); makeDirty();"/>
						</td>
		</tr>
		<tr>
						<td class="label">
							<spring:message code="login.password.expired.date" /> <span class="requiredlabel">*</span>
						</td>
						<td>

							<form:input path="expirationDate" 
							          cssClass='required' onchange="makeDirty();" />

						</td>
		</tr>
		<tr>
						<td class="label">
							<spring:message code="login.timeout" />
						</td>
						<td>
							<form:input path="timeout" onchange="makeDirty();" />
						</td>
						<td>&nbsp;</td>
		</tr>
		<tr>
			<td>&nbsp;</td>
		</tr>
		<tr>
						<td class="label">
							<spring:message code="login.account.locked" />
						</td>
						<td>
							<form:radiobutton path="accountLocked" value="Y" onchange="makeDirty();" label="Y" />
							<form:radiobutton path="accountLocked" value="N" onchange="makeDirty();" label="N" />
						</td>
		</tr>
		<tr>
						<td class="label">
							<spring:message code="login.account.disabled" />
						</td>
						<td>
							<form:radiobutton path="accountDisabled" value="Y" onchange="makeDirty();" label="Y" />
							<form:radiobutton path="accountDisabled" value="N" onchange="makeDirty();" label="N" />
						</td>
		</tr>
		<tr>
						<td class="label">
							<spring:message code="systemuser.isActive" />
						</td>
						<td>
							<form:radiobutton path="accountActive" value="Y" onchange="makeDirty();" label="Y" />
							<form:radiobutton path="accountActive" value="N" onchange="makeDirty();" label="N" />
						</td>
		</tr><tr>
			<td>&nbsp;</td>
		</tr>
</table>
<hr/>
   <spring:message code="systemuserrole.instruction" htmlEscape="false"/>
<hr/>
<table>
	<tr>
	    <td> <spring:message code="systemuserrole.copypermissions"/> </td>
		<td>
		<input type="text" id="systemUserToCopySelector" onchange="handleCopyPermisions(this);" oninput="activateCopyPermisions();"/>
		 <form:hidden id="systemUserToCopy" path="systemUserIdToCopy" />
		 <form:hidden id="allowCopyUserRoles" path="allowCopyUserRoles" />	
		</td>
		<td> <button type="button"  id="copyUserPermisions" onClick="copyPermisions();"><spring:message code="systemuserrole.apply"/></button>  </td>
	</tr>
</table>
<hr/>
<table>
	<tr>
		<td class="label" width="50%">
			<spring:message code="systemuserrole.roles.global" />
		</td>
	</tr>
	<c:forEach items="${form.globalRoles}" var="role">
		<tr>
		<td>
		<c:forEach begin="0" end="${role.nestingLevel}">
			&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
		</c:forEach>
		<form:checkbox path="selectedRoles" 
						id="role_${role.roleId}" 
						onclick="selectChildren(this, ${role.childrenID});makeDirty();"
						value="${role.roleId}"
						onchange="checkAdminRoles();"
						/>
			<c:out value="${role.roleName}" />
		</td>
		</tr>
	</c:forEach>
	<tr>
		<td class="label" width="50%">
			<spring:message code="systemuserrole.roles.labunit" />
		</td>
	</tr>
	 <tr>
		<td id="rolesRow">
			<table id="rolesTable"  style="display: none">
			<tr> 
			    <td>
				     <button type="button" name="removeRow" id="removeRoles" onClick="removeRolesTable(this);activateSave();">-</button> 
				</td>
				<td>
					<form:select path="testSectionId" onchange="activateSave();disableAddRoles()">
					            <option value="AllLabUnits"><spring:message code="systemuserrole.allLabUnits"/></option>
								<form:options items="${form.testSections}" itemLabel="value" itemValue="id" />
					</form:select>
				</td>		
			</tr>
			<tr>
			<td>&nbsp;</td>
			<td>
			   &nbsp;&nbsp;&nbsp;
			   <input type="checkbox" onchange="activateSave();" onclick="selectAllRoles(this);"> <spring:message code="systemuserrole.allpermissions"/></input>
			</td>
			</tr>
			<c:forEach items="${form.labUnitRoles}" var="role">
				<tr>
				<td>&nbsp;</td>
				<td>
				<c:forEach begin="0" end="${role.nestingLevel}">
					&nbsp;
				</c:forEach>
				<form:checkbox path="selectedLabUnitRoles" 
								onclick="selectChildren(this, ${role.childrenID});makeDirty();"
								value="${role.roleId}"
								onchange="activateSave();"
								/>		
					<c:out value="${role.roleName}" />
				</td>
				</tr>
			</c:forEach>
			</table>
		</td>	
	</tr>
	<tr>
		<td>
		  <button type="button" name="createNewRoles" id="createNewRoles" onClick="createNewRolesTable();">+</button> 
		  &nbsp;
		  <spring:message code="systemuserrole.newpermissions"/>
		</td>
	</tr>
</table>
