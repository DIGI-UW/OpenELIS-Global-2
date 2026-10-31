                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 
function getNodeNamesByTagName(elements, tag) {
	//initialize helper objects
	allTestsMap = new Object();
	panelTestsMap = new Object();
	
    var nodes = elements.getElementsByTagName(tag);
    var objList = new Array();
    for (var j=0; j<nodes.length; j++) {
        var name  = nodes[j].getElementsByTagName("name")[0].firstChild.nodeValue;
        var id = nodes[j].getElementsByTagName("id")[0].firstChild.nodeValue;
        if (tag == "panel") {
            objList[j] = new Panel(id, name);
            var testNodes = elements.getElementsByTagName("panelTests");
            for (var x=0; x < testNodes.length; x++) {
            	var ptNodes = elements.getElementsByTagName("test");
            	for (var y=0; y < ptNodes.length; y++ ) {
	                var pName  = ptNodes[y].getElementsByTagName("name")[0].firstChild.nodeValue;
	                var pId = ptNodes[y].getElementsByTagName("id")[0].firstChild.nodeValue;
	            	if (objList[j].tests.length == 0) {
	            		objList[j].tests = pName;
	            		objList[j].testIds = pId;
	            	} else {
	            		objList[j].tests = objList[j].tests + "," + pName;
	            		objList[j].testIds = objList[j].testIds + "," + pId;
	            	}
            	}
            		
            }
        } else if (tag == "test") {
            objList[j] = new Test(id, name);
            allTestsMap[id] = name;    
        } else if (tag == "crosssampletype") {
        	var testtag = nodes[j].getElementsByTagName("testid");
        	if (testtag.length > 0) {
        		var testid = testtag[0].firstChild.nodeValue;
        		objList[j] = new CrossSampleType(id, name, testid);
        	}
        	else
        		objList[j] = new CrossSampleType(id, name);
        }
    }
    
    return objList;
    
}

// A global array of SampleType
var SampleTypes = [];


//crossSampleType object (i.e. a hashmap of sampleTypes keyed by id)
var sampleTypeMap = {};

function getSampleTypeMapEntry(id) {
    return sampleTypeMap[id];
}
// SampleType object for holding panels and tests for sample types
function SampleType(id, name) {
    this.id = id;    
    this.name = name;
    this.rowid = "";
    this.panels = new Array();
    this.tests = new Array();
    this.setCrossPanels = "false";
    this.setCrossTests = "false";
    this.crossPanels = new Array();
    this.crossTests = new Array();
}

// Panel object
function Panel(id, name) {
    this.id = id;
    this.name = name;
    this.tests = "";
    this.testIds = "";
}                                                                                         

var initializePanelTests = false;

// Test object
function Test(id, name) {
    this.id = id;
    this.name = name;
    
}

// TestMap object (i.e. a hashmap of chosen tests)
var allTestsMap = new Object();
function getTestMapEntry(id) {
    return allTestsMap[id];
}

//PanelTestMap object (i.e. a hashmap of testIds with panelId as key)
var panelTestsMap = new Object();
function getPanelTestMapEntry(panelId) {
    return panelTestsMap[panelId];
}

function displaySampleTypes(entryDate) {
	for( var i = 0; i < SampleTypes.length; i++ ) {
		SampleTypes[i].rowid = (i + 1);
		assignTestsToSampleTypeTests(SampleTypes[i], entryDate);
	} 
}

function assignTestsToSampleTypeTests(sampleType, entryDate){
	var index = sampleType.rowid;
    var addTable = document.getElementsByClassName("samplesAddedTable")[1];
    var sampleDescription = sampleType.name;
    var sampleTypeValue = sampleType.id;
    var currentTime = getCurrentTime();
    
   
    var testIdString = "";
    var panelIdString = "";
    var testDisplayString = "";
    
    addTypeToTable(addTable, sampleDescription, sampleTypeValue, currentTime,  entryDate );

	if ($("testIds_" + index))
		testIdString =  $("testIds_" + index).value;
	if ($("panelIds_" + index))
    	panelIdString = $("panelIds_" + index).value;
	if ($("tests_" + index))
    	testDisplayString = $("tests_" + index).value;
    
    for (var i=0; i<sampleType.tests.length; i++ ) {
        if (testIdString.length == 0) {
            testIdString = sampleType.tests[i].id;
            testDisplayString = sampleType.tests[i].name;
        } else {
            testIdString = testIdString + "," + sampleType.tests[i].id;
            testDisplayString = testDisplayString + "," + sampleType.tests[i].name;
        }                                                                           
    }
    
    if (sampleType.setCrossTests == 'true') {
	    for (var i=0; i<sampleType.crossTests.length; i++ ) {
	        if (testIdString.length == 0) {
	            testIdString = sampleType.tests[i].id;
	            testDisplayString = sampleType.tests[i].name;
	        } else {
	            testIdString = testIdString + "," + sampleType.crossTests[i].id;
	            testDisplayString = testDisplayString + "," + sampleType.crossTests[i].name;
	        }
	    }
    }

    for (var i=0; i<sampleType.panels.length; i++ ) {
        if (panelIdString.length == 0) {
            panelIdString = sampleType.panels[i].id;
        } else {
            panelIdString = panelIdString + "," + sampleType.panels[i].id;
        }
        // add the panel tests to the list of testIds and tests
        if (testIdString.length == 0) {
        	testIdString = sampleType.panels[i].testIds;
        	testDisplayString = sampleType.panels[i].tests;
        } else {
            testIdString = testIdString + "," + sampleType.panels[i].testIds;
            testDisplayString = testDisplayString + "," + sampleType.panels[i].tests;        	
        }
    }
    
    if (sampleType.setCrossPanels == 'true') {
	    for (var i=0; i<sampleType.crossPanels.length; i++ ) {
	        if (panelIdString.length == 0) {
	            panelIdString = sampleType.crossPanels[i].id;
	        } else {
	            panelIdString = panelIdString + "," + sampleType.crossPanels[i].id;
	        }
	    }
    }

    $("tests_" + index).value = testDisplayString;
    $("testIds_" + index).value = testIdString;
    $("panelIds_" + index).value = panelIdString;
    
}

// This is a hash mapping of key cross test name and value is CrossSampleType
// used to store and access testIds and sample type info
var crossTestSampleTypeTestIdMap = {};

function getCrossTestSampleTypeTestIdMapEntry(name) {
	return crossTestSampleTypeTestIdMap[name];
}

var sampleTypeTestIdMap = {};

function getSampleTypeTestIdMapEntry(id) {
    return sampleTypeTestIdMap[id];
}


//SampleType object for holding panels and tests for sample types
function CrossSampleType(id, name, testId) {
    this.id = id;
    this.name = name;
    this.testId = testId;
}

// Panel object
function CrossPanel(id, name) {
    this.id = id;
    this.name = name;
    this.sampleTypes = new Array();
    this.typeMap = new Array();
}

// Test object
function CrossTest(name) {
    this.name = name;
    this.sampleTypes = new Array();
    this.typeMap = new Array();
}

var sampleTypeOrder;

// crossSampleType map object (i.e. a hashmap of sampleTypes keyed by id)
var crossSampleTypeMap = {};

function getCrossSampleTypeMapEntry(id) {
    return crossSampleTypeMap[id];
}

var crossSampleTypeOrderMap = {};

function getCrossSampleTypeOrderMapEntry(id) {
    return crossSampleTypeOrderMap[id];
}


function populateCrossPanelsAndTests(panels, tests, entryDate) {
    var outer = document.createElement("table");
    outer.width = "100%";
    outer.insertRow().insertCell().appendChild(document.createElement("br"));
    var table = document.createElement("table");
    table.id = "crossPanelsTestsTable";
    outer.insertRow().insertCell().appendChild(table);
    populateCrossPanels(panels, table, entryDate);
    populateCrossTests(tests, table, entryDate);
    var target = document.getElementById("crossPanels");
    if (target) target.replaceChildren(outer);
}

function removeCrossPanelsTestsTable() {
	var table = $("crossPanelsTestsTable");
	if (table) {
		var rows = table.rows.length;
	
		for( var i = rows - 1; i > 0; i--){
			table.deleteRow( i );
		}
	}
}

function populateCrossRows(items, table, entryDate, isPanel) {
    if (!items || !items.length) return;
    var heading = table.insertRow();
    heading.insertCell().textContent = isPanel ? "Panel" : "Test";
    for (var i = 0; i <= sampleTypeOrder; i++) {
        heading.insertCell().textContent = crossSampleTypeMap[getCrossSampleTypeOrderMapEntry(i)].name;
    }
    var divider = table.insertRow().insertCell();
    divider.colSpan = sampleTypeOrder + 2;
    divider.appendChild(document.createElement("hr"));
    items.forEach(function (item) {
        var row = table.insertRow();
        var name = row.insertCell();
        name.appendChild(document.createTextNode(item.name));
        var required = document.createElement("span");
        required.className = "requiredlabel";
        required.textContent = "*";
        name.appendChild(required);
        var hidden = document.createElement("input");
        hidden.type = "hidden";
        hidden.className = "required";
        hidden.name = "hidden-" + item.name;
        hidden.value = "unselected";
        name.appendChild(hidden);
        for (var j = 0; j <= sampleTypeOrder; j++) {
            var sampleTypeId = crossSampleTypeOrderMap[j];
            var sampleType = getCrossSampleTypeMapEntry(sampleTypeId);
            var cell = row.insertCell();
            if (item.typeMap[sampleType.name] === "t") {
                var radio = document.createElement("input");
                radio.type = "radio";
                radio.name = isPanel ? item.id : item.name;
                radio.id = sampleTypeId;
                radio.value = sampleTypeId;
                radio.addEventListener("click", (function (id, type) {
                    return function () {
                        if (isPanel) crossPanelSelected(item.id, item.name, id, type.name, entryDate);
                        else crossTestSelected(item.name, id, type.name, entryDate);
                    };
                })(sampleTypeId, sampleType));
                cell.align = "middle";
                cell.appendChild(radio);
            }
        }
    });
}

function populateCrossPanels(panels, table, entryDate) {
    populateCrossRows(panels, table, entryDate, true);
}

function crossTestSelected(testName, sampleTypeId, sampleTypeName, entryDate) {
	
	var testId = "";
	var sTypes = getCrossTestSampleTypeTestIdMapEntry(testName);
	for (var i=0; i<sTypes.length; i++) {
		if (sTypes[i].id == sampleTypeId) {
			testId = sTypes[i].testId;
			break;
		}
	}
	var testIds = [];
	var testNames = [];
	testIds[0] = testId;
	testNames[0] = testName;

	if (document.getElementsByClassName("samplesAddedTable")[1].rows.length > 1)
		removeTestsFromAllSampleTypes(testIds, testNames);
	
	var table = document.getElementsByClassName("samplesAddedTable")[1];
	var length = table.rows.length;
	var rows = table.rows;
	var currentTime = getCurrentTime();
	var sampleTypeMatchingRowIndexes = [];
	
	// Iterate through all sampleAddedTable rows.  Find the selected crossTest if
	// it has already been registered.  
	for( var i = 1; i < length; i++){
		
		//var currentRowTests = $("testIds" + rows[i].id).value;
		var currentRowSampleTypeId = $("typeId" + rows[i].id).value;
		
		// look for any row that matches the selected sampletype for this crossPanel
		if (sampleTypeId == currentRowSampleTypeId)
			sampleTypeMatchingRowIndexes[sampleTypeMatchingRowIndexes.length] = i;
	}
	if (sampleTypeMatchingRowIndexes.length > 0){
		var tempIndex = rows[sampleTypeMatchingRowIndexes[0]].id;
		var tempCrossTests = $("testIds" + tempIndex).value;
		var crossTests = tempCrossTests.split(",");
		if (crossTests.length > 0) {
			$("testIds" + tempIndex).value = $("testIds" + tempIndex).value + "," + testId;
			$("tests" + tempIndex).value = $("tests" + tempIndex).value + "," + testName;
		} else {
			$("testIds" + tempIndex).value = testId;
			$("tests" + lastRowId).value = testName;
		}
		$("select" + tempIndex).click();
	// No sampleType row available yet.	
	} else {
		addTypeToTable(table, sampleTypeName, sampleTypeId, currentTime,  entryDate );
		document.getElementsByClassName("samplesAdded")[1].show();
		table = document.getElementsByClassName("samplesAddedTable")[1];
		length = table.rows.length;
		rows = table.rows;
		var lastRowIndex = length - 1;
		var lastRowId = rows[lastRowIndex].id;
		var tIds = $("testIds" + lastRowId).value;
		//var sId = $("typeId" + lastRowId).value;
		if (tIds.length > 0) {
			$("testIds" + lastRowId).value = tIds + "," + testId;
			$("tests" + lastRowId).value = $("tests" + lastRowId).value + "," + testName;
		} else {
			$("testIds" + lastRowId).value = testId;
			$("tests" + lastRowId).value = testName;
		}
		$("select" + lastRowId).click();
	}
	
	setRequiredSelected("hidden-" + testName);
}

function crossPanelSelected(panelId, panelName, sampleTypeId, sampleTypeName, entryDate) {
	
	initializePanelTests = true;
	
	if (document.getElementsByClassName("samplesAddedTable")[1].rows.length > 1)
		removePanelFromAllSampleTypes(panelId);
	
	var table = document.getElementsByClassName("samplesAddedTable")[1];
	var length = table.rows.length;
	var rows = table.rows;
	var currentTime = getCurrentTime();
	var sampleTypeMatchingRowIndexes = [];
	//var rowThatContainsCrossPanel = "";
	//var rowThatContainsPriorSampleType = "";
	
	
	// Iterate through all sampleAddedTable rows.  Find the selected crossPanel if
	// it has already been registered.  Also, if the crossPanel has not been
	// registered, then keep track of sample types that already exist that can have 
	// the crossPanel added.
	for( var i = 1; i < length; i++){
		
		//var currentRowPanels = $("panelIds" + rows[i].id).value;
		var currentRowSampleTypeId = $("typeId" + rows[i].id).value;
		
		// look for any row that matches the selected sampletype for this crossTest
		if (sampleTypeId == currentRowSampleTypeId)
			sampleTypeMatchingRowIndexes[sampleTypeMatchingRowIndexes.length] = i;
	}


	if (sampleTypeMatchingRowIndexes.length > 0){
		var tempIndex = rows[sampleTypeMatchingRowIndexes[0]].id;
		
		var tempCrossPanels = $("panelIds" + tempIndex).value;
		var crossPanels = tempCrossPanels.split(",");
		if (crossPanels.length > 0) {
			$("panelIds" + tempIndex).value = $("panelIds" + tempIndex).value + "," + panelId;
		} else {
			$("panelIds" + tempIndex).value = panelId;
		}
		$("select" + tempIndex).click();
	} else {
		addTypeToTable(table, sampleTypeName, sampleTypeId, currentTime,  entryDate );
		document.getElementsByClassName("samplesAdded")[1].show();
		table = document.getElementsByClassName("samplesAddedTable")[1];
		length = table.rows.length;
		rows = table.rows;
		var lastRowIndex = length - 1;
		var lastRowId = rows[lastRowIndex].id;
		var pIds = $("panelIds" + lastRowId).value;
		//var sId = $("typeId" + lastRowId).value;
		if (pIds.length > 0) {
			$("panelIds" + lastRowId).value = $("panelIds" + lastRowId).value + "," + panelId;
		} else {
			$("panelIds" + lastRowId).value = panelId;
		}
		$("select" + lastRowId).click();
	}
	
	setRequiredSelected("hidden-" + panelName);
}

function setRequiredSelected(name) {
	var requireds = document.getElementsByClassName('required');
	for (var i = 0; i < requireds.length; ++i) {
		if (requireds[i].name == name) 
			requireds[i].value = 'selected';  
	}
	setSave();
}

/* check for missing required crossPanels and crossTests */
function missingRequiredValues(){
    var missing = false;
	var requireds = document.getElementsByClassName('required');
	for (var i = 0; i < requireds.length; ++i) {
	    var item = requireds[i];  
	    if (requireds[i].value == 'unselected')
	    	missing = true;
	}

    return missing;
    
}

function removePanelFromAllSampleTypes(panelId) {
	var panelTests = getPanelTestMapEntry(panelId);
	var panelTestsArray = panelTests.split(",");
	removePanelTestsFromTestTable(panelTestsArray);
	removePanel(panelId);
}

function removePanelTestsFromTestTable(panelTestsArray){
	var testTable = $("addTestTable");

	var inputs = testTable.getElementsByTagName( "input" );
	
	var testIndexes = [];
	var testNames = [];
	var testIds = [];
	var cnt = 0;
	
	for( var i = 0; i < inputs.length; i = i + 2 ){
		var testIndex = Number(inputs[i].id.substring(5));
		
		if (panelTestsArray.indexOf(testIndex) > -1) {
			var testName = inputs[i+1].parentNode.lastChild.nodeValue;
			var testid = inputs[i+1].value;
			testIndexes[cnt] = testIndex;
			testIds[cnt] = testid;
			testNames[cnt++] = testName;
		}
	}
	
	removeTestsFromAllSampleTypes(testIds, testNames);	

}


function removePanel(panelId) {
	for (var i=0; i < SampleTypes.length; i++) {
		if (SampleTypes[i].panels) {
			for (var j=0; j < SampleTypes[i].panels.length; j++) {
				if (SampleTypes[i].panels[j].id == panelId) {
					SampleTypes[i].panels.splice(j, 1);
		            break;
				}
			}
		}
	}
	var table = document.getElementsByClassName("samplesAddedTable")[1];
	var rows = table.rows.length;

	for( var i = 1; i < rows; i++){
		var rowid = parseInt(table.rows[i].id.substr(1));
		var allPanelIds = $("panelIds_" + rowid ).value;
		var arrayPanelIds = allPanelIds.split(",");
		
		if (arrayPanelIds.indexOf(panelId) > -1)
			arrayPanelIds.splice(arrayPanelIds.indexOf(panelId), 1);
			
		$("panelIds_" + rowid ).value = arrayPanelIds.join(',');
		
	}

}


function removeTestsFromAllSampleTypes(testIds, testNames) {
	var table = document.getElementsByClassName("samplesAddedTable")[1];
	var rows = table.rows.length;
	var rowsToRemove = [];
	var rowsToRemoveIndex = 0;

	for( var i = 1; i < rows; i++){
		var rowid = parseInt(table.rows[i].id.substr(1));
		var sampleTypeId = $("typeId_" + rowid ).value;
		var allTestIds = $("testIds_" + rowid ).value;
		var allTests = $("tests_" + rowid ).value;
		var arrayTestIds = allTestIds.split(",");
		var arrayTests = allTests.split(",");
		
		for (var k=0; k<testIds.length; k++) {
			if (arrayTestIds.indexOf(testIds[k]) > -1)
				arrayTestIds.splice(arrayTestIds.indexOf(testIds[k]), 1);
		}
		for (var j=0; j<testNames.length; j++) {
			
			// Check if we have a crossTest in
			// the list of tests.  We have to go 
			// through the list and switch test ids 
			// for the different sampleTypes if 
			// crossTests are present.
			var sTypes = getCrossTestSampleTypeTestIdMapEntry(testNames[j]);
			if (sTypes) {
				for (var m=0; m<sTypes.length; m++) {
					if (sTypes[m].id == sampleTypeId) {
						testId = sTypes[m].testId;
						if (arrayTestIds.indexOf(testId) > -1) {
							arrayTestIds.splice(arrayTestIds.indexOf(testId), 1);
						}
						break;
					}
				}			
			}
			//////
			if (arrayTests.indexOf(testNames[j]) > -1)
				arrayTests.splice(arrayTests.indexOf(testNames[j]), 1);
		}
		
		if (arrayTests.length == 0) {
			rowsToRemove[rowsToRemoveIndex++] = rowid;
		} else {		
			$("testIds_" + rowid ).value = arrayTestIds.join(',');
			$("tests_" + rowid ).value = arrayTests.join(',');
		}
	}
	// remove any sampleType rows that are empty
	for( var x = (rows-1); x >= 1; x--){
		var rowid = parseInt(table.rows[x].id.substr(1));
		if (rowsToRemove.indexOf(rowid) > -1)
			table.deleteRow( x );
	}
}

function populateCrossTests(tests, table, entryDate) {
    populateCrossRows(tests, table, entryDate, false);
}

function getCurrentTime(){
	var date = new Date();

	return (formatToTwoDigits(date.getHours()) + ":"  + formatToTwoDigits(date.getMinutes()));
}

function formatToTwoDigits( number ){
	return number > 9 ? number : "0" + number;
}
