<%@ page language="java"
	contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
			org.openelisglobal.common.util.Versioning,
			org.openelisglobal.internationalization.MessageUtil"
%>
<%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>



<c:set var="success" value="${success}" />

<%--bugzilla 1908 changed some disabled values for Vietnam tomcat/linux--%>
	<%
		    String previousDisabled = "false";
            String nextDisabled = "false";
            if (request.getAttribute(IActionConstants.PREVIOUS_DISABLED) != null) {
               previousDisabled = (String)request.getAttribute(IActionConstants.PREVIOUS_DISABLED);
            }
            if (request.getAttribute(IActionConstants.NEXT_DISABLED) != null) {
               nextDisabled = (String)request.getAttribute(IActionConstants.NEXT_DISABLED);
            }
            String saveDisabled = (String)request.getAttribute(IActionConstants.SAVE_DISABLED);

        %>

<script>
//bugzilla 1467 this is for next/previous buttons on EDIT screens (in case user made changes without saving)
function confirmSaveForwardPopup(direction)
{
  var myWin = createSmallConfirmPopup( "", null , null );

   <%

      out.println("var message = null;");

      String message = MessageUtil.getMessage("message.popup.confirm.saveandforward");

     out.println("message = '" + message +"';");

     String button1 = 	MessageUtil.getMessage("label.button.yes");
     String button2 = 	MessageUtil.getMessage("label.button.no");
     String button3 = 	MessageUtil.getMessage("label.button.cancel");

     String title = 	MessageUtil.getMessage("title.popup.confirm.saveandforward");

     String space = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";


 %>

    var href = "css/openElisCore.css?";

    var strHTML = "";

    strHTML = '<html><link rel="stylesheet" type="text/css" href="' + href + '" /><head><';
    //bugzilla 1510 cross-browser use hasFocus not hasFocus()
    //bugzilla 1903 fix problem - go back to hasFocus() for now
    strHTML += 'SCRIPT LANGUAGE="JavaScript">var tmr = 0;function fnHandleFocus(){if(window.opener.document.hasFocus()){window.focus();clearInterval(tmr);tmr = 0;}else{if(tmr == 0)tmr = setInterval("fnHandleFocus()", 500);}}</SCRIPT';
    strHTML += '><';
    strHTML += 'SCRIPT LANGUAGE="javascript" >'
    strHTML += 'var imp= null; function impor(){imp="norefresh";} ';
    strHTML += ' function fcl(){  if(imp!="norefresh") {  window.opener.reFreshCurWindow(); }}';

    strHTML += '  function goToNextActionSave(){ ';
    strHTML += ' var reqParms = "?direction=next&ID="; ';
    strHTML += ' window.opener.setAction(window.opener.document.getElementById("mainForm"), "UpdateNextPrevious", "yes", reqParms);self.close();} ';
    strHTML += '  function goToPreviousActionSave(){ ';
    strHTML += ' var reqParms = "?direction=previous&ID="; ';
    strHTML += ' window.opener.setAction(window.opener.document.getElementById("mainForm"), "UpdateNextPrevious", "yes", reqParms);self.close();} ';

    strHTML += '  function goToNextActionNoSave(){ ';
    strHTML += ' var reqParms = "?direction=next&ID="; ';
    strHTML += ' window.opener.setAction(window.opener.document.getElementById("mainForm"), "NextPrevious", "no", reqParms);self.close();} ';
    strHTML += '  function goToPreviousActionNoSave(){ ';
    strHTML += ' var reqParms = "?direction=previous&ID="; ';
    strHTML += ' window.opener.setAction(window.opener.document.getElementById("mainForm"), "NextPrevious", "no", reqParms);self.close();} ';

    strHTML += ' setTimeout("impor()",359999);</SCRIPT';
    strHTML += '><title>' + "<%=title%>" + '</title></head>';
    strHTML += '<body onBlur="fnHandleFocus();" onLoad="fnHandleFocus();" ><form name="confirmSaveIt" method="get" action=""><div id="popupBody"><table><tr><td class="popuplistdata">';
    strHTML += message;
    if (direction == 'next') {
     strHTML += '<br><center><input type="button"  name="save" value="' + "<%=button1%>" + '" onClick="goToNextActionSave();" />';
     strHTML += "<%=space%>";
     strHTML += '<input type="button"  name="save" value="' + "<%=button2%>" + '" onClick="goToNextActionNoSave();"/>';
    } else if (direction == 'previous') {
     strHTML += '<br><center><input type="button"  name="save" value="' + "<%=button1%>" + '" onClick="goToPreviousActionSave();" />';
     strHTML += "<%=space%>";
     strHTML += '<input type="button"  name="save" value="' + "<%=button2%>" + '" onClick="goToPreviousActionNoSave();"/>';
    }
    strHTML += "<%=space%>";
    strHTML += '<input type="button"  name="cancel" value="' + "<%=button3%>" + '" onClick="self.close();" /></center></div>';
    strHTML += '</td></tr></table></form></body></html>';

     myWin.document.write(strHTML);

     //bugzilla 1579
     myWin.window.document.close();

     //bugzilla 1579
     setTimeout ('myWin.close()', 360000);
}

//bugzilla 1467
function previousAction(form, ignoreFields) {
  if (isDirty(form, ignoreFields)) {
     confirmSaveForwardPopup('previous');
  } else {
	  navigationAction(document.getElementById("mainForm"), 'NextPrevious', 'no', '?direction=previous&ID=');
  }
}

//bugzilla 1467
function nextAction(form, ignoreFields) {
  if (isDirty(form, ignoreFields)) {
      //popup to give user option to save, don't save AND go to next, cancel
      confirmSaveForwardPopup('next');
  } else {
	  navigationAction(document.getElementById("mainForm"), 'NextPrevious', 'no', '?direction=next&ID=');
  }
}

function saveOnClickAction() {
	if (typeof mySaveAction === "function") {
		return mySaveAction();
	}
	if(checkClicked()) {
 		return false;
 	} else {
		window.onbeforeunload = null; // Added to flag that formWarning alert isn't needed. Used in unifiedSystemUser.jsp
  		setAction(document.getElementById("mainForm"), '', 'yes', '?ID=');
 	}
 }

<%if( request.getAttribute(IActionConstants.FWD_SUCCESS) != null &&
      ((Boolean)request.getAttribute(IActionConstants.FWD_SUCCESS)) ) { %>
if( typeof(showSuccessMessage) != 'undefined' ){
	showSuccessMessage( true );
}
<% } %>
</script>
<center>
<table border="0" cellpadding="0" cellspacing="0">
	<tbody valign="middle">
		<tr>
	      	<td>
  			<button type="button" 
  					onclick="saveOnClickAction();" 
  					name="save"  
  					<% if (Boolean.valueOf(saveDisabled).booleanValue()) {%>
  					disabled="disabled"
  					<% } %>
  					>
  			   <spring:message code="label.button.save" />
  			</button>
  	    </td>

		<td>&nbsp;</td>
		<td>
  			<button type="button" onclick="cancelAction();"  name="cancel" property="cancel" >
  			   <%--AIS - bugzilla 1860--%>
  			   <spring:message code="label.button.exit"/>
  			</button>
	    </td>
   		<td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
   		<td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
  		<td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
  		<td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
   		<td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
  		<td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
     <%if( request.getAttribute(IActionConstants.DISPLAY_PREV_NEXT) == null ||
       ((Boolean)request.getAttribute(IActionConstants.DISPLAY_PREV_NEXT)) ) { %>     
	    <td>
  			<button type="button" 
  					onclick="previousAction(document.getElementById('mainForm'), '');" 
  					name="previous" 
  					property="previous" 
  					<% if (Boolean.valueOf(previousDisabled).booleanValue()) {%>
  					disabled="disabled"
  					<% } %>
					>
  			   <spring:message code="label.button.previous"/>
  			</button>
	    </td>
     	<td>&nbsp;</td>
	    <td>
  			<button type="button" 
  					onclick="nextAction(document.getElementById('mainForm'), '');"  
  					name="next" 
  					property="next" 
  					<% if (Boolean.valueOf(nextDisabled).booleanValue()) {%>
  					disabled="disabled"
  					<% } %>
  					>
  			   <spring:message code="label.button.next"/>
  			</button>
	    </td>
       <% } %>
	    </tr>
	 </tbody>
</table>
</center>