function OEPager(formName, sufix) {
	this.formName = formName;
	this.sufix = sufix;
	this.curentPageNumber = 0;
	this.form = document.getElementById("mainForm");

	this.setCurrentPageNumber = function(number) {
		this.currentPageNumber = Number(number);
	};

	this.pageFoward = function() {
		window.onbeforeunload = null;
		var queryParams = new URLSearchParams(window.location.search);
		var baseURL= window.location.pathname;
		var url = window.location.href;
		this.form.action = this.formName.sub('Form', '') +"?page=" + (this.currentPageNumber + 1) + this.sufix;
		if (url.indexOf("RetroC")!==-1){
			//this.form.action = this.formName.sub('Form', '') +"RetroC?pageResults=true&page=" + (this.currentPageNumber + 1) + this.sufix;
			queryParams.set("pageResults", true);
			queryParams.set("page", (this.currentPageNumber + 1));
			this.form.action = baseURL+"?"+queryParams.toString();
			//this.form.action = url+"&pageResults=true&page=" + (this.currentPageNumber + 1);
		}
		else { 
		//this.form.action = this.formName.sub('Form', '') +"?pageResults=true&page=" + (this.currentPageNumber + 1) + this.sufix;
			queryParams.set("pageResults", true);
			queryParams.set("page", (this.currentPageNumber + 1));
			this.form.action = baseURL+"?"+queryParams.toString();
		//this.form.action = url+"&pageResults=true&page=" + (this.currentPageNumber + 1);
		}
		//'<%= logbookType == "" ? "" : "&type=" + logbookType  %>';
		this.form.submit();
	};

	this.pageBack = function() {
		window.onbeforeunload = null;
		var queryParams = new URLSearchParams(window.location.search);
		var baseURL= window.location.pathname;
		//this.form.action = this.formName.sub('Form', '') +"RetroC?page=" + (this.currentPageNumber - 1) + this.sufix;
		var url = window.location.href;
		if (url.indexOf("RetroC")!==-1){
			//this.form.action = this.formName.sub('Form', '') +"RetroC?pageResults=true&page=" + (this.currentPageNumber - 1) + this.sufix;
			queryParams.set("pageResults", true);
			queryParams.set("page", (this.currentPageNumber - 1));
			this.form.action = baseURL+"?"+queryParams.toString();
		}
		else { 
		//this.form.action = this.formName.sub('Form', '') +"?pageResults=true&page=" + (this.currentPageNumber - 1) + this.sufix;
			queryParams.set("pageResults", true);
			queryParams.set("page", (this.currentPageNumber - 1));
			this.form.action = baseURL+"?"+queryParams.toString();
		}
		//		+ '<%= logbookType == "" ? "" : "&type=" + logbookType  %>';
		this.form.submit();
	};

	this.jumpToPage = function(page) {
		window.onbeforeunload = null;
		this.form.action = this.formName.sub('Form', '') + "?pageResults=true&page=" + page + this.sufix;
			//+ '<%= logbookType == "" ? "" : "&type=" + logbookType  %>';
		this.form.submit();
	};

	this.jumpToPageToSearch = function(page, term) {
		window.onbeforeunload = null;
		this.form.action = this.formName.sub('Form', '') + "?pageResults=true&page=" + page + "&searchTerm=" + term + this.sufix;
			//+ '<%= logbookType == "" ? "" : "&type=" + logbookType  %>';
		this.form.submit();
	};
};

//
// notFoundElement - the elemenet containing the text indicating the search target was not found
// targetElementTag - the tag element containing the search target value(i.e. ""td" or "tr" where the search target is displayed)
// oePager - the instance of the OEPager
function OEPageSearch( notFoundElement, targetElementTag, oePager ){
	this.notFoundElement = notFoundElement;
	this.highlightElementTag = targetElementTag + "."; 
	this.previousLabSearch;
	this.previousSearchTerm;
	this.oePager = oePager;
	
	this.doLabNoSearch = function(searchElement){
		if( this.previousLabSearch){
			this.previousLabSearch.style.backgroundColor = "";
		}

		this.notFoundElement.style.visibility = "hidden";
		
		if( searchElement.value.length > 0){
			if (typeof altAccessionHighlightSearch  === 'function') {
				altAccessionHighlightSearch(searchElement.value);
			} else {
				this.highlightSearch( searchElement.value, true);
			}
		}
	};
	
	this.highlightSearch = function( target, jumpIfNeeded ){

		var targetsOnPage = $$(this.highlightElementTag + target);


		if( targetsOnPage.length > 0  ){
		    this.previousLabSearch = targetsOnPage[0];
			targetsOnPage[0].style.backgroundColor = "#ffff24";
			//until we figure a better solution the timeout is needed when the target is on a different page
			setTimeout(function(){window.scrollTo(0, targetsOnPage[0].offsetTop);}, 100);
			//window.scrollTo( 0, targetsOnPage[0].offsetTop );
		}else{
			var page = pagingSearch[target];
			
			if( page && jumpIfNeeded ){ 
				this.oePager.jumpToPageToSearch( page, target );
				this.previousSearchTerm = null;
			}else{
				notFoundElement.style.visibility = "visible";
			}
		}
	};
}
