<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.localization.valueholder.Localization,
			org.openelisglobal.siteinformation.valueholder.SiteInformation,
			org.owasp.encoder.Encode" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 <script>

	document.addEventListener("DOMContentLoaded", function() {
        var imageNames = ["headerRightImage", "labDirectorSignature", "headerLeftImage"]
        imageNames.forEach(imageName => {
            fetch('./dbImage/siteInformation/' + imageName)
                .then(response => response.json())
                .then(data => {
                    var imageElement = document.getElementById(imageName);
                    imageElement.src = data.value;
                })
                .catch(error => {
                    console.log('Error fetching image data:', error);
                });

        })
    });
 </script>


<table width="80%" border="2">
	<tr>
		<th>&nbsp;</th>
	   	<th><spring:message code="generic.name" /></th>
	   	<th><spring:message code="generic.description"/></th>
	   	<th><spring:message code="generic.value"/></th>
	</tr>
	<form:form name="${form.formName}" 
		   action="${form.formAction}" 
		   modelAttribute="form" 
		   method="${form.formMethod}"
		   id="menuForm">
	<c:forEach items="${form.menuList}" var="site" varStatus="iter">
		<c:set var="siteId" value="${site.id}"/>
	  	<tr>
	   		<td class="textcontent">
	      		<form:checkbox path="selectedIDs" value="${siteId}" onclick="output()"/>
   	   		</td>
   	   		<td class="textcontent">
	    		<c:out value="${site.name}"/>
	   		</td>
   	  	 	<td class="textcontent">
	   	  	 <spring:message code="${site.descriptionKey}"/>
	   	    </td>
	   	    <td class='textcontent'>
	   	    <c:choose>
	   	    <c:when test="${site.valueType == 'logoUpload'}">
	   			<c:choose>
	   			<c:when test="${site.name == 'headerLeftImage'}">
                <img src=""  
                	 height="42" 
	   		         width="42" 
					 id="headerLeftImage" /> 
                </c:when>
	   			<c:when test="${site.name == 'labDirectorSignature'}">
                <img src=""  
                	 height="42" 
	   		         width="42" 
					 id="labDirectorSignature" />
                </c:when><c:otherwise>
                <img src=""  
                	 height="42" 
	   		         width="42" 
					 id="headerRightImage" />
                </c:otherwise>
                </c:choose>
	   		</c:when><c:when test="${site.tag == 'localization'}">
	   			<c:forEach items="${site.localization.localesAndValuesOfLocalesWithValues}" var="localizationValue">
	   				<c:out value="${localizationValue}"/>
	   				<br>
	   			</c:forEach>
            </c:when><c:otherwise>
	   	  		<c:out value="${site.value}"/>
	   		</c:otherwise>
	   		</c:choose>
	   		</td>
     	</tr>
	</c:forEach>
	</form:form>
</table>
