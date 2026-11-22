/*
 * resultBox -- the text box into which the results were entered
 * row -- the row number of the test results
 * lowerNormal -- the low end of the normal range
 * upperNormal -- the high end of the normal range
 * lowerAbnormal -- the low end of the range in which the test is considered valid
 * highAbnormal -- the high end of the range in which the test is considered valid
 * reportingDecimalPlaces -- optional display/report decimal places (not an input constraint)
 *
 *  N.B.  If the upper and lower ranges of the normal and abnormal settings are the same then those ranges will
 *  be ignored.
 */
var outOfValidRangeMsg = null;

function validateNumberFormat( resultBox, row, reportingDecimalPlaces){
	//ignore < or > from the analyser on validation
	var greaterThanOrLessThan = "";
	if(resultBox.value.startsWith("<") ||resultBox.value.startsWith(">") ){
		greaterThanOrLessThan = resultBox.value.charAt(0);
	}
	var actualValue = resultBox.value.replace(/[<>]/g, '')
	

    if(actualValue.blank()){
        resultBox.title = "";
        resultBox.style.background = "#ffffff";
        $("valid_" + row).value = false;
        return true;
    }

    if( actualValue.trim() == "."){
        resultBox.value = greaterThanOrLessThan + "0.0";
    }

    if(isNaN(actualValue)){
        $("valid_" + row).value = false;
        return false;
    }

    // Presentation settings never alter a clinical entry.

    return true;
}

function /*void*/ validateResults( resultBox, row, lowerNormal, upperNormal, lowerAbnormal, upperAbnormal, reportingDecimalPlaces, specialCase ){
    //ignore < or > from the analyser on validation
	var greaterThanOrLessThan = "";
	if(resultBox.value.startsWith("<") ||resultBox.value.startsWith(">") ){
		greaterThanOrLessThan = resultBox.value.charAt(0);
	}
	var actualValue = resultBox.value.replace(/[<>]/g, '')

    var isSpecialCase = specialCase == actualValue.toUpperCase();
    var validFormat = validateNumberFormat( resultBox,row, reportingDecimalPlaces);

    resultBox.style.borderColor = validFormat ? "" : "red";


	if( isSpecialCase ){
		resultBox.title = "";
		resultBox.value = greaterThanOrLessThan + actualValue.toUpperCase();
		resultBox.style.borderColor = "";
		resultBox.style.background = "#ffffff";
		$("valid_" + row).value = true;
		return;
	}
	
	if( lowerAbnormal != upperAbnormal &&
	   (actualValue < lowerAbnormal || actualValue > upperAbnormal) ){
		resultBox.style.background = "#ffa0a0";
		resultBox.title = "En dehors de la plage valide"; //FIXME: Uses hardcoded French labels. Switch to refer to resource file.
		$("valid_" + row).value = false;

		if( outOfValidRangeMsg ){
			alert( outOfValidRangeMsg);
		}
	}else if( lowerNormal != upperNormal &&
		(actualValue < lowerNormal || actualValue > upperNormal) ){
		resultBox.style.background = "#ffffa0";
		resultBox.title = "En dehors de la plage normale"; //FIXME: Uses hardcoded French labels. Switch to refer to resource file.
		$("valid_" + row).value = true;
	}else{
		resultBox.style.background = "#ffffff";
		resultBox.title = "";
		$("valid_" + row).value = true;
	}
}

function checkNumberFormat( resultBox, row, reportingDecimalPlaces){
    var validFormat = validateNumberFormat( resultBox, row, reportingDecimalPlaces);

    resultBox.style.borderColor = validFormat ? "" : "red";
}

