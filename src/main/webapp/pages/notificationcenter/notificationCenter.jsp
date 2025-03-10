<%@ page language="java" contentType="text/html; charset=UTF-8"
	import="org.openelisglobal.common.action.IActionConstants,
                 org.openelisglobal.internationalization.MessageUtil"%>

<%@ page isELIgnored="false"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>


<style>
.tab-label {
	overflow: hidden;
	border: 1px solid #ccc;
	background-color: #f1f1f1;
}

/* Style the buttons that are used to open the tab content */
.tab-label button {
	background-color: inherit;
	float: left;
	border: none;
	outline: none;
	cursor: pointer;
	padding: 14px 16px;
	transition: 0.3s;
}

/* Change background color of buttons on hover */
.tab-label button:hover {
	background-color: #ddd;
}

/* Create an active/current tablink class */
.tab-label button.active {
	background-color: #ccc;
}

/* Style the tab content */
.tabcontent {
	visibility: collapse;
	padding: 6px 12px;
	border: 1px solid #ccc;
	border-top: none;
}

.electronicOrders {
	visibility: visible;
}
</style>


<script>
	function openTab(event, tabClass) {
		// Declare all variables
		var i, tabcontent, tablinks, rows;

		// Get all elements with class="tabcontent" and hide them
		tabcontent = document.getElementsByClassName("tabcontent");
		for (i = 0; i < tabcontent.length; i++) {
			tabcontent[i].style.visibility = "collapse";
		}

		// Get all elements with class="tablinks" and remove the class "active"
		tablinks = document.getElementsByClassName("tablinks");
		for (i = 0; i < tablinks.length; i++) {
			tablinks[i].className = tablinks[i].className
					.replace(" active", "");
		}

		// Show the current tab, and add an "active" class to the button that opened the tab
		rows = document.getElementsByClassName(tabClass);
		for (i = 0; i < rows.length; i++) {
			rows[i].style.visibility = "visible";
		}
		event.currentTarget.className += " active";
	}
</script>

<table width="100%">
	<tbody>
		<tr>
			<td>
			<h1><spring:message code=""
						text="Notification Center" />
			</h1>
			</td>
		</tr>
		<tr>
			<td>

				<div class="tab-label">
				<button type="button" class="tablinks active"
					onclick="openTab(event, 'electronicOrders')">Electronic
					Orders</button>
				<button type="button" class="tablinks"
					onclick="openTab(event, 'systemMessages')">System Messages</button>
				<button type="button" class="tablinks"
					onclick="openTab(event, 'referralResults')">Referral Results</button>
					</div>
			</td>
		</tr>

		<tr class="electronicOrders tabcontent">
			<td>
				<h2>
					<spring:message code=""
						text="Electronic Orders" />
				</h2>
			</td>
		</tr>
		<tr class="electronicOrders tabcontent">
			<td><iframe src="ElectronicOrders" height="500px"
					style="width: 100%;"></iframe></td>
		</tr>

		<tr class="systemMessages tabcontent">
			<td>
				<h2>
					<spring:message code=""
						text="System Messages" />
				</h2>
			</td>
		</tr>
		<tr class="systemMessages tabcontent">
			<td>System messages</td>
		</tr>

		<tr class="referralResults tabcontent">
			<td>
				<h2>
					<spring:message code=""
						text="Referral Results" />
				</h2>
			</td>
		</tr>
		<tr class="referralResults tabcontent">
			<td>Referral results</td>
		</tr>

	</tbody>
</table>

