<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants"
%>

<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<%--bugzilla 1908 changed some disabled values for Vietnam tomcat/linux--%>


<center>
<table border="0" cellpadding="0" cellspacing="0">
	<tbody valign="middle">
		<tr>
		<% 	
		    String previousDisabled = "false";
            String nextDisabled = "false"; 
            if (request.getAttribute(IActionConstants.PREVIOUS_DISABLED) != null) {
               previousDisabled = (String)request.getAttribute(IActionConstants.PREVIOUS_DISABLED);
            }
            if (request.getAttribute(IActionConstants.NEXT_DISABLED) != null) {
               nextDisabled = (String)request.getAttribute(IActionConstants.NEXT_DISABLED);
            }
 
        %>
	   <td >
  			<button type="button" 
  					onclick="setMenuAction(this, document.getElementById('menuForm'), '', 'yes', '?paging=1');return false;" 
  					name="previous" 
					<%if ( Boolean.valueOf(previousDisabled).booleanValue() ) {%>
					disabled="disabled"
					<%} %>
					>
  			   <spring:message code="label.button.previous"/>
  			</button>
	   </td>
	   <td>
	   &nbsp;
	   </td>
	   <td>
  			<button type="button" 
  					onclick="setMenuAction(this, document.getElementById('menuForm'), '', 'yes', '?paging=2');return false;" 
  					name="next"  
					<%if ( Boolean.valueOf(nextDisabled).booleanValue() ) {%>
					disabled="disabled"
					<%} %>
					>
  			   <spring:message code="label.button.next"/>
  			</button>
	   </td>
	   <%--td removed 02/27/2006 not needed>
	   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
	   </td>
	  <td>
  			<html:button onclick="setMenuActionUrl(this, document.getElementById('mainForm'), 'Cancel', 'no', '');" property="cancel" >
  			   <spring:message code="label.button.cancel"/>
  			</html:button>
	    </td>
	    </tr--%>
	 </tbody>
</table>