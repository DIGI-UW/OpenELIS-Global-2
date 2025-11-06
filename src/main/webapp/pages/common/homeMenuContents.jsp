<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<div  id="IEWarning" style="display:none;background-color:#b0c4de"  ><b>
<spring:message code="banner.menu.ie.warning" text="banner.menu.ie.warning"/><br/>
<spring:message code="banner.menu.ie.instructions" text="banner.menu.ie.instructions"/></b>
</div>

<script>

function initWarning(){
	var ua = navigator.userAgent;

	//all we care about is if it is IE in non-capatibility mode
	var regEx = new RegExp("MSIE 8");
	var messageNeeded = regEx.test(ua);

	if( messageNeeded ){
		$("IEWarning").show();
	}else{
		$("IEWarning").hide();
	}
}

initWarning();
</script>

