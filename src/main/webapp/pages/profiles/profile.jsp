<%@ page language="java" contentType="text/html; charset=UTF-8" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>

 
<script>
    jQuery(document).ready(function () {
    });
</script>

<form:form name="${form.formName}"
		   modelAttribute="form"  
		   method="POST" 
		   action="${form.formAction}" 
		   enctype="multipart/form-data">
	<table>
		<tr>
			<td><form:label path="file"><spring:message code="upload.profile.instructions"/></form:label></td>
			<td><input type="file" name="file" /></td>
		</tr>
		<tr>
	        <td><input type="submit" value="Submit" /></td>
	    </tr>
	</table>
</form:form>