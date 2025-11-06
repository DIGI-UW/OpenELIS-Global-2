<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.internationalization.MessageUtil"
%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<script type="text/javascript">

function /*void*/ showSuccessMessage( show ){
	$("successMsg").style.visibility = show ? 'visible' : 'hidden';
}

function printBarcodeInNewWindow(success, failure) {
	var labNo = document.getElementById('searchValue').value;
	document.getElementById('getBarcodePDF').href = "LabelMakerServlet";
	document.getElementById('getBarcodePDF').click();
}
</script>

<c:set var="pageSuccess" value="${success || param.forward == 'success'}"/>

<div id="successMsg" style="text-align:center; color:seagreen;  width : 100%;font-size:170%; <c:if test="${not pageSuccess}">visibility : hidden</c:if>">
	<spring:message code="save.success"/>
	<div>
		<input type="button"
        	value="<%= MessageUtil.getMessage("barcode.common.button.print")%>"
        	id="printBarcodeButton"
        	onclick="printBarcodeInNewWindow();">
        <a href="" id="getBarcodePDF" target="_blank"></a>
	</div>
</div>




