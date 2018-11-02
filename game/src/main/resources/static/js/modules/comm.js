function PlayerList() {
	fenster1=window.open(DS.getUrl()+"?module=plist&compopup=1",
						 "Spielerliste",
						 "width=350,height=400,locationbar=0,menubar=0,scrollbars=1,statusbar=0,toolbar=0,resizeable=yes,directories=0");
}

function BBCodeInfo() {
	fenster2=window.open(DS.getUrl()+"?module=bbcodeview",
						 "BBCodes",
						 "width=550,height=400,locationbar=0,menubar=0,scrollbars=1,statusbar=0,toolbar=0,resizeable=yes,directories=0");
}

function insertRealTime(){
	var Jetzt = new Date();
	var Hours = appendZero(Jetzt.getHours());
	var Minutes = appendZero(Jetzt.getMinutes());
	var Seconds = appendZero(Jetzt.getSeconds());
	document.getElementById('msg').value = document.getElementById('msg').value + Hours + ':' + Minutes + ':' + Seconds;
	document.getElementById('msg').focus();
}

function appendZero(subject){
	if(subject < 10){
		subject = "0"+subject;
	}
	return subject;
}

function insertSignature(){
	document.getElementById('msg').value = document.getElementById('msg').value + document.getElementById('signature').value;
	document.getElementById('msg').focus();
}

function setRenameAction(id) {
	actionSelectChange("");
	document.getElementById("form_ordnername").style.display = "inline";
	document.getElementById("rename").disabled = false;
	document.getElementById("select_action").value = "rename";
	document.getElementById("subject").value = id;
}

var movePMID = 0;
var movePMToID = 0;

function finishMovePM( response ) {
	document.getElementById("inbox_waiter").style.display = "none";

	if( parseInt(response) > 0 ) {
		document.getElementById("inbox_pm"+movePMID).style.display="none";
		document.getElementById("inbox_pm"+movePMID+"_row").parentNode.removeChild(document.getElementById("inbox_pm"+movePMID+"_row"));
		document.getElementById("inbox_ordner"+movePMToID+"_count").innerHTML = parseInt(document.getElementById("inbox_ordner"+movePMToID+"_count").innerHTML)+1;
	}
	else if( parseInt(response) == 0 ) {
		alert("Der Server konnte die Nachricht nicht finden");
	}
	else {
		alert(response.responseText);
	}
	movePMID = 0;
	movePMToID = 0;
}

function actionSelectChange( newval ) {
	if( newval == "deletePlayer" ) {
		document.getElementById('form_playerid').style.display = "inline";
		document.getElementById('form_ordnername').style.display = "none";
		document.getElementById('form_moveto').style.display = "none";
	}
	else{
		if (newval == "newOrdner"){
			document.getElementById('form_playerid').style.display = "none";
			document.getElementById('form_ordnername').style.display = "inline";
			document.getElementById('form_moveto').style.display = "none";
		}
		else {
			if(newval == "moveAll" || newval == "moveSelected"){
				document.getElementById('form_playerid').style.display = "none";
				document.getElementById('form_ordnername').style.display = "none";
				document.getElementById('form_moveto').style.display = "inline";
			}
			else{
				document.getElementById('form_playerid').style.display = "none";
				document.getElementById('form_ordnername').style.display = "none";
				document.getElementById('form_moveto').style.display = "none";
			}
		}
	}
	document.getElementById('rename').disabled = true;
}

function showpm(pmid) {
	DS.get({module: 'comm', action: 'showPm', pmid: pmid}, function(result) {
		$('#pmviewer').empty().append($(result).filter('#pmcontent'));
	});
}

$(document).ready(function() {
	if( $('#currentDsModule').val() === 'comm' ) {
		$('#pm_to').autocomplete({
			source : function(pattern, response) {
				DsAutoComplete.users(pattern, function(data) {
					if( "ally".indexOf(pattern.term) !== -1 ) {
						data.push({label: "Eigene Allianz", value:"ally"});
					}
					response(data);
				});
			},
			html:true
		});
	}
});