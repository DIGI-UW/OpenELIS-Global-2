<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<%@ page import="org.openelisglobal.common.action.IActionConstants,
				 org.openelisglobal.common.util.*, org.openelisglobal.internationalization.MessageUtil" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript" src="scripts/utilities.js?" ></script>
<script type="text/javascript">
var validator = new FieldValidator();
validator.setRequiredFields( new Array("heightBlockLabels", "widthBlockLabels", "heightSlideLabels", "widthSlideLabels","heightOrderLabels", "widthOrderLabels", "heightSpecimenLabels", "widthSpecimenLabels", "numMaxOrderLabels", "numMaxSpecimenLabels", "numDefaultOrderLabels", "numDefaultSpecimenLabels") );

function savePage() {
	document.getElementById("mainForm").action = "BarcodeConfiguration"
	document.getElementById("mainForm").submit();
}

function setMyCancelAction() {
	window.location.href = "MasterListsPage";
}

function disableSave() {
	document.getElementById("saveButtonId").disabled = true;
}

function enableSave() {
	document.getElementById("saveButtonId").disabled = false;
}
</script>

<jsp:include page="${numberBarCodesFragment}"/>
<jsp:include page="${elementsBarCodesFragment}"/>
<jsp:include page="${alternateAccessionFragment}"/>
<jsp:include page="${sizeBarCodesFragment}"/>
