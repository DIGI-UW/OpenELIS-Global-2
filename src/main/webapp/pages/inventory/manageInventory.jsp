<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.openelisglobal.common.action.IActionConstants,
				org.openelisglobal.inventory.form.InventoryKitItem,
				org.openelisglobal.common.util.DateUtil,
				org.openelisglobal.common.util.IdValuePair,
				org.openelisglobal.internationalization.MessageUtil,
				java.util.List" %>


<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%@ taglib prefix="fn" uri="jakarta.tags.functions"%>

	
<c:set var="kitSources" value="${form.sources}" />
<c:set var="kitTypeList" value="${form.kitTypes}"/>
<c:set var="currentKitCount" value="${fn:length(form.inventoryItems)}"/>
<script type="text/javascript">

var dirty = false;
var nextKitValue = 	${currentKitCount} + 1;
var errorColor = "#ff0000";
var DatePattern = /^((((0?[1-9]|[12]\d|3[01])[\.\-\/](0?[13578]|1[02])[\.\-\/]((1[6-9]|[2-9]\d)?\d{2}))|((0?[1-9]|[12]\d|30)[\.\-\/](0?[13456789]|1[012])[\.\-\/]((1[6-9]|[2-9]\d)?\d{2}))|((0?[1-9]|1\d|2[0-8])[\.\-\/]0?2[\.\-\/]((1[6-9]|[2-9]\d)?\d{2}))|(29[\.\-\/]0?2[\.\-\/]((1[6-9]|[2-9]\d)?(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00)|00)))|(((0[1-9]|[12]\d|3[01])(0[13578]|1[02])((1[6-9]|[2-9]\d)?\d{2}))|((0[1-9]|[12]\d|30)(0[13456789]|1[012])((1[6-9]|[2-9]\d)?\d{2}))|((0[1-9]|1\d|2[0-8])02((1[6-9]|[2-9]\d)?\d{2}))|(2902((1[6-9]|[2-9]\d)?(0[48]|[2468][048]|[13579][26])|((16|[2468][048]|[3579][26])00)|00))))$/;
var TEST_KIT_PROTOTYPE_ID = '<input name="inventoryLocationId" size="3" value="" disabled="disabled" type="text">';
var TEST_KIT_PROTOTYPE_NAME = '<input name="kitName" value="" type="text" class="kitName" onblur="checkIfEmpty(this);">';
var TEST_KIT_PROTOTYPE_TYPE = '<select name="type" class="kitType" > \
								<option value="" ></option> ' +
								'<c:forEach items="${kitTypeList}" var="kitTypeValue"> <option value="${kitTypeValue}">${kitTypeValue}</option> </c:forEach>'
								+ '</select>';
var TEST_KIT_PROTOTYPE_RECEIVE = '<input name="receiveDate" class="receiveDate" size="12" value="" type="text" onblur="validateDate(this);">';
var TEST_KIT_PROTOTYPE_EXPIRE = '<input name="expirationDate" class="expirationDate" size="12" value=""  type="text" onblur="validateDate(this);">';
var TEST_KIT_PROTOTYPE_LOT = '<input name="lotNumber" class="lotNumber" size="6" value="" type="text">';
var TEST_KIT_PROTOTYPE_SOURCE = '<select name="organizationId" class="organizationId" > \
								' + '<c:forEach items="${kitSources}" var="pair"> <option value="${pair.id}">${pair.value}</option>	</c:forEach>'
								+ '</select>';
var TEST_KIT_PROTOTYPE_REMOVE = '<input name="removeButton" value="' + '<spring:message code="label.button.remove"/>' + '" onclick="removeTestKit(#);" class="textButton" type="button">';

function validateDate( id ) {
	id.value = id.value.strip();

    if (((id.value.match(DatePattern)) && (id.value!=='')) || (id.value=='')) {
    	id.style.borderColor = "";
    	return true;
    } else {
    	id.style.borderColor  = errorColor;
    	return false;
    }
}

function checkIfEmpty( field ) {
	field.style.borderColor = field.value ? "" :errorColor;
}

function /*void*/ showInactiveKits( sendingButton ){
	sendingButton.hide();
	$('hideInactiveButton').show();
	$('inactiveInventoryItems').show();
}

function /*void*/ hideInactiveKits( sendingButton ){
    sendingButton.hide();
	$('showInactiveButton').show();
	$('inactiveInventoryItems').hide();
}

function /*void*/ deactivateTestKit( index ){
	$("activeRow_" + index).hide();
	$("inactiveRow_" + index).show();
	$("isActive_" + index ).value = false;
	setModifiedFlag(index);
}

function /*void*/ reactivateTestKit( index ){
	$("activeRow_" + index).show();
	$("inactiveRow_" + index).hide();
	$("isActive_" + index ).value = true;
	setModifiedFlag(index);
}

function /*void*/ setModifiedFlag( index ){
	$("isModified_" + index ).value = true;
	makeDirty();
}

function /*void*/ addNewTestKit(){

	var testKitTable = $("testKitTable");
	var rows = testKitTable.rows;
	var origionalRowCount = rows.length;
	var newRow = testKitTable.insertRow( origionalRowCount );
	//N.B. assigning innerHTML to a row does not work for IE
	newRow.insertCell(0).innerHTML = TEST_KIT_PROTOTYPE_ID;
	newRow.insertCell(1).innerHTML = TEST_KIT_PROTOTYPE_NAME;
	newRow.insertCell(2).innerHTML = TEST_KIT_PROTOTYPE_TYPE;
	newRow.insertCell(3).innerHTML = TEST_KIT_PROTOTYPE_RECEIVE;
	newRow.insertCell(4).innerHTML = TEST_KIT_PROTOTYPE_EXPIRE;
	newRow.insertCell(5).innerHTML = TEST_KIT_PROTOTYPE_LOT;
	newRow.insertCell(6).innerHTML = TEST_KIT_PROTOTYPE_SOURCE;
	newRow.insertCell(7).innerHTML = TEST_KIT_PROTOTYPE_REMOVE.replace(/\#/g, nextKitValue);

	newRow.id = "addedKit_" + nextKitValue;
	newRow.className = "addedKit";
	nextKitValue++;

	makeDirty();
	//alert( newRow.innerHTML );
}

function /*void*/ removeTestKit( index ){
	var testKitTable = $("testKitTable");
	var row = $("addedKit_" + index);
	testKitTable.deleteRow( row.rowIndex );
}


function /*void*/ createNewKitXML(){
	var xml = "<?xml version='1.0' encoding='utf-8'?><newKits>";
	var newKitRows = $$('tr.addedKit');
	var rowsLength = newKitRows.length;
    var i;

	for( var rowIndex = 0; rowIndex < rowsLength; ++rowIndex ){

		xml += "<kit ";

		var row = newKitRows[rowIndex];
		var cells = row.getElementsByTagName("input");
		var cellsLength = cells.length;

		for( i = 1; i < cellsLength; ++i ){ //the first cell is the kit id #

			xml += cells[i].className + "=\"";
			xml += cells[i].value;
			xml += "\" ";
		}

		cells = row.getElementsByTagName("select");
		cellsLength = cells.length;

		for( i = 0; i < cellsLength; ++i ){

			xml += cells[i].className + "=\"";
			xml += cells[i].value;
			xml += "\" ";
		}

		xml += " />";
	}

	xml += "</newKits>";

	$('newKits').value = xml;
}

function  /*void*/ setMyCancelAction(form, action, validate, parameters)
{
	//first turn off any further validation
	setAction(document.getElementById("mainForm"), 'Cancel', 'no', '');
}

function  /*void*/ savePage()
{
	var receiveDates, expirationDates,i;
	var kits = $$("input.kitName");
		
	for(i = 0; i < kits.length; ++i) {
		if( kits[i].value == null || kits[i].value == "" ) {
			alert("Le nom est obligatoire");
			return;
		}
	}

	receiveDates = $$("input.receiveDate");

	for(i = 0; i < receiveDates.length; ++i) {
		if( !validateDate(receiveDates[i]) ){
			alert("Mal formaté la date");
			return;
		}
	}
	
	expirationDates = $$("input.expirationDate");
	
	for(i = 0; i < expirationDates.length; ++i) {
		if( !validateDate(expirationDates[i]) ){
			alert("Mal formaté la date");
			return;
		}
	}
	
	
	createNewKitXML();
  
	window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed.
	var form = document.getElementById("mainForm");
	form.action = "ManageInventory";
	form.submit();

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

</script>

<div id="PatientPage" class="colorFill" style="display:inline" >
	<c:if test="${not (form.inventoryItems == null)}">
	<form:hidden path="newKitsXML" id="newKits" />
	<table id="testKitTable"   style="width:100%" >
	<tr >
		<th style="width:5%">
			<spring:message code="inventory.testKit.id"/>
		</th>
		<th style="width:15%">
			<spring:message code="inventory.testKit.name"/>
			<span class="requiredlabel">*</span>
		</th>
		<th style="width:10%">
  			<spring:message code="inventory.testKit.type"/>
		</th>
		<th style="width:10%">
			<spring:message code="inventory.testKit.receiveDate"/><br><span style="font-size: xx-small; "><%=DateUtil.getDateUserPrompt()%></span>
		</th>
		<th style="width:10%">
			<spring:message code="inventory.testKit.expiration"/><br><span style="font-size: xx-small; "><%=DateUtil.getDateUserPrompt()%></span>
		</th>
		<th style="width:10%">
			<spring:message code="inventory.testKit.lot"/>
		</th>
		<th style="width:20%">
			<spring:message code="inventory.testKit.source"/>
			<span class="requiredlabel">*</span>
		</th>
		<th style="width:10%">
		</th>
	</tr>
	<c:forEach var="inventoryItems"  items="${form.inventoryItems}" varStatus="iter">
		<tr
			<c:if test="${not inventoryItems.isActive}">style="display:none"</c:if> id='activeRow_${iter.index}'>
			<td >
				<form:hidden path="inventoryItems[${iter.index}].isActive" id='isActive_${iter.index}'/>
				<form:hidden path="inventoryItems[${iter.index}].isModified" id='isModified_${iter.index}'/>
				<form:input path="inventoryItems[${iter.index}].inventoryLocationId" disabled="true" size="3" />
			</td>
			<td>
			<form:input path="inventoryItems[${iter.index}].kitName"
			            cssClass="kitName"
			            onchange='setModifiedFlag(${iter.index});'
			            onblur="checkIfEmpty(this);"/>
			</td>
			<td >
				<form:select path="inventoryItems[${iter.index}].type"  
				 	value='${inventoryItems.type}'
					onchange= 'setModifiedFlag(${iter.index});' >
				    <option value="" ></option>
					<form:options items="${form.kitTypes}" />
				</form:select>
			</td>
			<td >
				<form:input path="inventoryItems[${iter.index}].receiveDate"
							cssClass="receiveDate"
							size="12"
							onchange='setModifiedFlag(${iter.index});' 
							onblur="validateDate(this);"/>
			</td>
			<td >
				<form:input path="inventoryItems[${iter.index}].expirationDate"
				            cssClass="expirationDate"
				            size="12"
				            onchange='setModifiedFlag(${iter.index});' 
				            onblur="validateDate(this);"/>
			</td>
			<td >
				<form:input path="inventoryItems[${iter.index}].lotNumber"
				            size="6"
				            onchange='setModifiedFlag(${iter.index});'/>
			</td>
			<td >
						<form:select path="inventoryItems[${iter.index}].organizationId"
				             value='${inventoryItems.organizationId}'
				             onchange='setModifiedFlag(${iter.index});'>

					<form:options items="${form.sources}" itemValue="id" itemLabel="value"/>
				</form:select>
			</td>
			<td >
                <input type="button"
                       value='<spring:message code="label.button.deactivate"/>'
                       onclick='deactivateTestKit(${iter.index});'
                       class="textButton">
			</td>
		</tr>
	</c:forEach>
	</table>
	<div id="inactiveInventoryItems" style="display:none" >
	<hr>

	<table style="width:100%" >
	<tr>
		<th colspan="8"  align="left"><spring:message code="invnetory.testKit.inactiveKits"/></th>
	</tr>
	<c:forEach var="item"  items="${form.inventoryItems}" varStatus="iter">
	
		<tr <c:if test="${item.isActive}"> style="display:none"  </c:if> id='inactiveRow_${iter.index}'>
			<td style="width:5%">
				<form:input path="inventoryItems[${iter.index}].inventoryLocationId" disabled="true" size="3" />
			</td>
			<td style="width:15%">
				<form:input path="inventoryItems[${iter.index}].kitName" disabled="true" />
			</td>
			<td style="width:10%" >
				<form:input path="inventoryItems[${iter.index}].type" disabled="true" size="12" />
			</td>
			<td style="width:10%">
				<form:input path="inventoryItems[${iter.index}].receiveDate" disabled="true" size="12" />
			</td>
			<td style="width:10%">
				<form:input path="inventoryItems[${iter.index}].expirationDate" disabled="true" size="12" />
			</td>
			<td style="width:10%">
				<form:input path="inventoryItems[${iter.index}].lotNumber" disabled="true" size="6" />
			</td>
			<td style="width:20%">
				<form:input path="inventoryItems[${iter.index}].source" disabled="true" />
			</td>
			<td style="width:10%">
                    <input type="button"
                           value='<%= MessageUtil.getMessage("label.button.reactivate")%>'
                           onclick='<%= "reactivateTestKit(${iter.index});"%>'
                           class="textButton">
			</td>
		</tr>
	</c:forEach>
	</table>
	</div>
	<br/>
        <input type="button"
                   value='<%= MessageUtil.getMessage("inventory.testKit.showAll")%>'
                   onclick="showInactiveKits( this );"
                   class="textButton"
                   id="showInactiveButton">
        <input type="button"
               value='<%= MessageUtil.getMessage("inventory.testKit.hideInactive")%>'
               onclick="hideInactiveKits( this );"
               class="textButton"
               style="display:none"
               id="hideInactiveButton">
        <input type="button"
               value='<%= MessageUtil.getMessage("inventory.testKit.add")%>'
               onclick="addNewTestKit();"
               class="textButton">
</c:if>
<c:if test="${form.inventoryItems == null}" >
	<spring:message code="inventory.testKit.none"/>
</c:if>


</div>
