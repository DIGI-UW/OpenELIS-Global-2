<%@ page language="java" contentType="text/html; charset=UTF-8"
import="org.openelisglobal.common.action.IActionConstants,
org.openelisglobal.common.util.ConfigurationProperties,
org.openelisglobal.common.util.ConfigurationProperties.Property,
org.openelisglobal.common.util.Versioning" %> <%@ page isELIgnored="false" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%> <%@
taglib prefix="spring" uri="http://www.springframework.org/tags"%> <%@ taglib
prefix="c" uri="jakarta.tags.core"%> <%@ taglib prefix="ajax"
uri="/tags/ajaxtags" %>

<bean:define
  id="idSeparator"
  value='<%=ConfigurationProperties.getInstance().getPropertyValue("default.idSeparator")%>'
/>
<bean:define
  id="accessionFormat"
  value="<%=ConfigurationProperties.getInstance().getPropertyValue(Property.AccessionFormat)%>"
/>
<bean:define id="genericDomain" value="" />

<script type="text/javascript" src="scripts/utilities.js?"></script>

<script type="text/javascript">
  function /*void*/ setMyCancelAction(form, action, validate, parameters) {
    //first turn off any further validation
    setAction(document.getElementById("mainForm"), "Cancel", "no", "");
  }
</script>
