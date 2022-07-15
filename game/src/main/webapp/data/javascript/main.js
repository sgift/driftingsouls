/*
	Mail Check
*/

var checkPMStatusCanceled = false;
function checkPMStatus() {
	if( !checkPMStatusCanceled ) {
		setTimeout(function() {
			checkPMStatus()
			}, 30000
		);
	}
	var url = getDsUrl();
	var params = {
			module:'main',
			action:'hasNewPm',
			autoAccess:true
	};

	jQuery.get( url, params, updatePMStatus );
}

function updatePMStatus( originalRequest ) {
	var response = originalRequest;

	if( response != null && response.indexOf('nicht eingeloggt') > -1 ) {
		checkPMStatusCanceled = true;
		return;
	}

	if( originalRequest == '1' ) {
		document.getElementById('mailicon').style.display='inline';
	}
	else {
		document.getElementById('mailicon').style.display='none';
	}
}


/*
	Boxfunktionen
*/

function boxClose(box) {
	document.getElementById(box).style.display = 'none';
}

function boxOpen(box) {
	var boxEl = $(box);
	if( boxEl.css('left') == '0px' || boxEl.css('left') == 'auto' ) {
		boxEl.css({
			left : Math.ceil($("body").width()/2-boxEl.width()/2)+"px",
			top : "80px"
		});
	}
	boxEl.css('display', 'block');

	if( box.indexOf('helpbox') > -1 ) {
		helpBox.fetchHelpText();
	}
}


var helpBox = {
	isVisible : function() {
		if( document.getElementById('helpbox').style.display == 'block' ) {
			return true;
		}
		return false;
	},
	fetchHelpText : function() {
		document.getElementById('helpboxtext').innerHTML = '';
		var params = {
				module:'main',
				action:'getHelpText',
				page:currentModule
		};
		jQuery.get( getDsUrl(), params, helpBox.updateHelpText );
	},
	updateHelpText : function( originalRequest ) {
		var response = originalRequest;

		if( response.indexOf('Sie muessen sich einloggen') > -1 ) {
			location.href = './portal';
			return;
		}

		document.getElementById('helpboxtext').innerHTML = response;
	}
}

var SearchBox = {
	execute : function() {
		var term = $('#searchInput').val();
		var resultDiv = $('#searchResult');
		resultDiv.empty();
		resultDiv.append("Suche...");

		var params = {
				module:'search',
				action:'search',
				search:term};
		$.getJSON(getDsUrl(), params, this.__processResponse);
	},
	__processResponse : function(response) {
		var resultDiv = $('#searchResult');
		resultDiv.empty();

		if( typeof response.users === 'undefined' || 
				(response.users.length == 0 && response.ships.length == 0 && response.bases.length == 0) ) {
			resultDiv.append("Keine Objekte gefunden");
			return;
		}

		var out = '<table class="datatable"><tbody>';

		for( var i=0; i < response.bases.length; i++ ) {
			var base = response.bases[i];
			out += '<tr><td><img src="./data/starmap/asti/asti.png" alt="Basis" title="Basis" /></td>';
			out += '<td><a href="'+getDsUrl()+'?module=base&col='+base.id+'&action=default" target="main">'+base.name+'</a></td>';
			out += '<td>'+base.location+'</td></tr>';
		}

		for( var i=0; i < response.ships.length; i++ ) {
			var ship = response.ships[i];
			out += '<tr><td><img style="width:30px" src="'+ship.type.picture+'" alt="'+ship.type.name+'" title="'+ship.type.name+'" /></td>';
			out += '<td><a href="'+getDsUrl()+'?module=schiff&ship='+ship.id+'&action=default" target="main">'+ship.name+'</a></td>';
			out += '<td>'+ship.location+'</td></tr>';
		}

		for( var i=0; i < response.users.length; i++ ) {
			var user = response.users[i];
			out += '<tr><td><img src="./data/interface/menubar/usericon.png" alt="Spieler" title="Spieler" /></td>';
			out += '<td><a href="'+getDsUrl()+'?module=userprofile&user='+user.id+'&action=default" target="main">'+user.name+' ('+user.id+')</a></td>';
			out += '<td></td></tr>';
		}

		out += "</tbody>";

		if( response.maxObjects ) {
			out += '<tfoot><tr><td colspan="3">Und weitere...</td></tr></tfoot>';
		}

		out += '</tr></table>';

		resultDiv.append(out);
	}
};



var adminBox = {
	init : function() {
		$('#adminconsolebox').draggable();
		
		$('#adminConsoleCommand').autocomplete({
			source : adminBox.__autoComplete,
			minLength : 0
		});
	},
	__autoComplete : function(pattern, response) {
		var url = getDsUrl();
		var params = {
				module:'admin',
				action:'ajax',
				namedplugin:'net.driftingsouls.ds2.server.modules.admin.AdminConsole',
				responseOnly:'1',
				autoComplete:'1',
				cmd:pattern.term
		};

		jQuery.getJSON( url, params, function(result) {
			var data = [];
			for( var i=0; i < result.length; i++ ) {
				data.push({label: result[i], value:result[i]});
			}
			response(data);
		});
	},
	execute : function() {
		var command = $('#adminConsoleCommand').val();
		document.getElementById('adminConsoleResponse').innerHTML = '';
		var params = {
				module:'admin',
				action:'ajax',
				namedplugin:'net.driftingsouls.ds2.server.modules.admin.AdminConsole',
				responseOnly:'1',
				cmd:command};
		jQuery.get(getDsUrl(), params, adminBox.showResponse);
	},

	showResponse : function( originalRequest ) {
		var response = originalRequest;

		if( response.indexOf('Sie muessen sich einloggen') > -1 ) {
			location.href = './portal';
			return;
		}

		document.getElementById('adminConsoleResponse').innerHTML = response;
	}
};



/*
	Page-Funktionen
*/
var currentModule;
var currentTitle;

function setCurrentPage(module, title) {
	$(document).ready(function() {
		var link = $('#page_'+currentModule);
		if( link.size() > 0 ) {
			link.removeClass('currentPage');
		}
		else {
			$('#page_currentpage').css('display', 'none');

			// Submenue leeren
			var currentPageDl = $('#currentpage dl:first-child dd');
			currentPageDl.remove();
		}

		var link = $('#page_'+module);
		if( link.size() > 0 ) {
			link.addClass('currentPage');
		}
		else if( title != 'null' ) {
			var reducedTitle = title;
			if( reducedTitle.length > 16 ) {
				reducedTitle = reducedTitle.substring(0,16)+"...";
			}
			document.getElementById('page_currentpage').firstChild.nodeValue = reducedTitle;
			document.getElementById('page_currentpage').style.display='inline';
			document.getElementById('page_currentpage_dropdown').style.display = 'none';
		}

		var oldModule = currentModule;

		currentModule = module;
		currentTitle = title;

		if( helpBox.isVisible() && oldModule != currentModule ) {
			helpBox.fetchHelpText();
		}
	});
}

function addPageMenuEntry(title, url) {
	$(document).ready(function() {
		var currentPageLi = document.getElementById('currentpage');

		var dl = currentPageLi.getElementsByTagName('dl')[0];
		var dd = document.createElement('dd');
		var a = document.createElement('a');
		a.href = url;
		a.appendChild(document.createTextNode(title));
		a.target = 'main';
		dd.appendChild(a);
		dl.appendChild(dd);

		document.getElementById('page_currentpage_dropdown').style.display = 'inline';
	});
}

function completePage() {
	$(document).ready(function() {
		var currentPageLi = $('#currentpage');
		var dds = currentPageLi.find('dd');

		if( dds.size() > 0 ) {
			var dl = currentPageLi.find('dl:first-child');
			dl.append('<dd class="lastelement"><span></span></dd>');
		}
	});
}

$(document).ready(function() {
	checkPMStatus();
	$('#noticebox').draggable();
	$('#helpbox').draggable();
	$('#searchbox').draggable();
	adminBox.init();
});