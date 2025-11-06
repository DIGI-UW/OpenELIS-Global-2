<%@ page language="java" contentType="text/html; charset=UTF-8"
         import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.internationalization.MessageUtil" %>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



 
<c:set var="valueEditable" value="${form.editable}"/>
<script>
    jQuery(document).ready(function () {
    	<c:if test='${not form.editable}'>
            jQuery(".inputWidget").prop('disabled', true);
       </c:if>
    });


    function validateForm(form) {
        return true;
    }

    function checklogLogoFile(uploader) {
        if (uploader.value.search("\.jpg") == -1 &&
                uploader.value.search("\.png") == -1 &&
                uploader.value.search("\.gif") == -1) {
            alert("<%= MessageUtil.getMessage("siteInformation.logo.warning") %>");
            jQuery('input[name="save"]').attr('disabled', '');
        } else {
            jQuery('input[name="save"]').removeAttr('disabled');
        }
    }

      var imageName = '${form.paramName}';
       fetch('./dbImage/siteInformation/' + imageName)
                .then(response => response.json())
                .then(data => {
                    var imageElement = document.getElementById("imagePreview");
                    imageElement.src = data.value;
                })
                .catch(error => {
                    console.log('Error fetching image data:', error);
        });
</script>


<spring:message code="generic.name"/>:&nbsp;<c:out value="${form.paramName}"/><br/><br/>
<c:out value="${form.description}"/><br/><br/>
<spring:message code="generic.value"/>:&nbsp;

<form:hidden path="paramName" id="siteInfoName"/>
<c:if test="${form.valueType == 'text'}">
    <c:if test="${form.encrypted}">
        <form:password path="value" size="60" maxlength="120"/>
    </c:if>
    <c:if test="${not form.encrypted}">
        <c:if test="${form.tag == 'localization'}" >
        	<table>
        	<c:forEach items="${form.localization.localesSortedForDisplay}" var="locale" varStatus="iter">
        		<c:if test="${iter.index % 2 == 0 }"> <tr> </c:if>
        		<td>
	            <label for="${locale.displayLanguage}"><c:out value="${locale.displayLanguage}"/>: </label>
	            <form:input path="localization.localeValues['${locale}']" size="60" maxlength="120" cssClass="inputWidget" id="${locale.displayLanguage}"/>
        		</td>
        		<c:if test="${iter.index % 2 == 1 }"> </tr> </c:if>
        	</c:forEach>
        	</table>
        </c:if>
        <c:if test="${form.tag != 'localization'}">
            <form:input path="value" size="60" maxlength="120" cssClass="inputWidget"/>
        </c:if>
    </c:if>
</c:if>
<c:if test="${form.valueType == 'boolean'}">
	<spring:message code="label.true" var="trueMsg"/>
	<spring:message code="label.false" var="falseMsg"/>
    <form:radiobutton path="value" value="true" cssClass="inputWidget" label="${trueMsg}"/>
    <form:radiobutton path="value" value="false" label="${falseMsg}"/>
</c:if>
<c:if test="${form.valueType == 'dictionary'}">
    <form:select path="value" cssClass="inputWidget">
    	<form:options items="${form.dictionaryValues}" />
    </form:select>
</c:if>
<c:if test="${form.valueType == 'freeText'}">
    <form:textarea path="value" rows="2" style="width:50%"/>
</c:if>
<c:if test="${form.valueType == 'logoUpload'}">
	<input type="hidden" name="logoName" id="logoName" />
    <img id="imagePreview" src="./dbImage/siteInformation/${form.paramName}"  
       	 height="42" 
	     width="42"  />
    <br>
    <input type="file" name="logoFile" onchange="checklogLogoFile( this )" id="inputWidget"/><br/>
    <spring:message code="label.remove.image" />
    <input type="checkbox" name="removeImage" id="removeImage" value="true" />
    <script type="text/javascript">
        jQuery("#mainForm").attr("enctype", "multipart/form-data");
        jQuery(":file").css("width", "600px"); 
        function setAction(form, action, validate, parameters) {
        	jQuery("#logoName").val(jQuery("#siteInfoName").val());
            form.action = '<%= request.getContextPath() %>' + "/logoUpload";
            form.validateDocument = new Object();
            form.validateDocument.value = validate;

            validateAndSubmitForm(form);
        }
        
        jQuery('#removeImage').click(function() {
            if(jQuery(this).is(":checked")) {
            	jQuery("#imagePreview").hide();
            }
            else if(jQuery(this).is(":not(:checked)")) {
            	jQuery("#imagePreview").show();
            }
      	});
        
        jQuery('#inputWidget').change(function(){
            const file = this.files[0];
            console.log(file);
            if (file){
              let reader = new FileReader();
              reader.onload = function(event){
                jQuery('#imagePreview').attr('src', '');
              }
              reader.readAsDataURL(file);
            }
          });
    </script>

</c:if>


		