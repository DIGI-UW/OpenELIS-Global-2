
function  /*void*/ savePage() {

	if ( fieldValidator.isAnyConflicted() == 0 ) {
		savePage__();
	}
}


function convertToAge(field,ageField){
	let today = new Date();
	let d = field.value.split("/");
    let birthdate = new Date(d[2] + '/' + d[1] + '/' + d[0]);
	let year_difference = today.getFullYear() - birthdate.getFullYear();  
	let one_or_zero = (today.getMonth() < birthdate.getMonth()) ||
	                  (today.getMonth() === birthdate.getMonth() && today.getDate() < birthdate.getDate()) ? 1 : 0;
	let age = year_difference - one_or_zero;
	if(age){
		document.getElementById(ageField).value = age;
	}
}

function checkDateOfBirth(blanksAllowed) {
		dobField = $("dateOfBirthID");
		handlePatientBirthDateChange();
		var compared = comparePatientField(dobField.id, true, blanksAllowed, "dob");
		checkValidDate(dobField);
		checkRequiredField(dobField, blanksAllowed);
}
	
	


