/*
	Mail Check
*/

var checkPMStatusCanceled = false;
var lastDsVersion = null;
function checkPMStatus() {
	function updatePMStatus( data ) {
		if( typeof data.errors !== 'undefined' && data.errors.length > 0 ) {
			angular.forEach(messageContainer.errors, function(error) {
				toastr.error(error.description);
			});
			return false;
		}
		else if( typeof data.message !== 'undefined' &&
			data.message.type === 'error' ) {
			var msg = data.message;

			if( msg.type === 'error' ) {
				toastr.error(data.message.description);
				if( typeof msg.redirect !== 'undefined' && msg.redirect ) {
					if( parent ) {
						parent.location.href=DS.getUrl();
					}
					else {
						location.href=DS.getUrl();
					}
				}
				return false;
			}
		}

		if( DS.istNichtEingeloggtFehler(data) ) {
			checkPMStatusCanceled = true;
			return;
		}

		if( lastDsVersion != null && lastDsVersion !== data.version ) {
			$('#infoicon').addClass('highlight');
		}
		lastDsVersion = data.version;

		$('#mailicon').css('display', data.pm ? 'inline' : 'none');
		$('#comneticon').css('display', data.comNet ? 'inline' : 'none');
	}

	if( !checkPMStatusCanceled ) {
		setTimeout(function() {
			checkPMStatus()
			}, 60000
		);
	}
	var params = {
		module:'main',
		action:'statusUpdate'
	};

	DS.getJSON( params, updatePMStatus );
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
		if( $('#helpbox').css('display') == 'block' ) {
			return true;
		}
		return false;
	},
	fetchHelpText : function() {
		$('#helpboxtext').empty();
		var params = {
				module:'main',
				action:'getHelpText',
				page:currentModule
		};
		$.get( DS.getUrl(), params, helpBox.updateHelpText );
	},
	updateHelpText : function( originalRequest ) {
		var response = originalRequest;

		if( response.indexOf('Sie muessen sich einloggen') > -1 ) {
			location.href = DS.getUrl()+"?module=portal&action=login";
			return;
		}

		$('#helpboxtext').append(response);
	}
};

var SearchBox = {
	open: function() {
		$('#searchbox').dialog({title: 'Suche', width:450, height:400});
		var input = $('#searchInput');
		input.off('keypress.suche');
		input.val('');
		input.on('keypress.suche', function(e) {
			if(e.which == 13) {
				SearchBox.execute();
			}
		});
	},
	execute : function() {
		var term = $('#searchInput').val();
		var resultDiv = $('#searchResult');
		resultDiv.empty();
		resultDiv.append("Suche...");

		var params = {
				module:'search',
				action:'search',
				search:term};
		$.getJSON(DS.getUrl(), params, this.__processResponse);
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
			out += '<tr><td><img src="'+base.image+'" alt="Basis" title="Basis" /></td>';
			out += '<td><a href="'+DS.getUrl()+'?module=base&col='+base.id+'&action=default" target="main">'+base.name+'</a></td>';
			out += '<td>'+base.location+'</td></tr>';
		}

		for( var i=0; i < response.ships.length; i++ ) {
			var ship = response.ships[i];
			out += '<tr><td><img src="'+ship.type.picture+'" alt="'+ship.type.name+'" title="'+ship.type.name+'" /></td>';
			out += '<td><a href="'+DS.getUrl()+'?module=schiff&ship='+ship.id+'&action=default" target="main">'+ship.name+'</a></td>';
			out += '<td>'+ship.location+'</td></tr>';
		}

		for( var i=0; i < response.users.length; i++ ) {
			var user = response.users[i];
			out += '<tr><td><img src="./data/logos/user/'+user.id+'.gif" alt="Spieler" title="Spieler" /></td>';
			out += '<td><a href="'+DS.getUrl()+'?module=userprofile&user='+user.id+'&action=default" target="main">'+user.name+' ('+user.id+')</a></td>';
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

var InfoBox = {
	open: function() {
		$('#infoicon').removeClass('highlight');
		$('#infobox').dialog({title: 'Über Drifting Souls', width:700, height:400});
		$.getJSON(DS.getUrl(), {module:'main', action: 'loadVersionInfo'})
			.done(function(data) {
				var $infobox = $('#infobox');
				$infobox.find('#lastBuild').text(data.buildTime);
				$infobox.find('#build').empty().append('<a target="_empty" href="'+data.buildUrl+'">'+data.build+'</a>');
				$infobox.find('#commit').text(data.commit);
			});

		$.getJSON(DS.getUrl(), {module:'main', action: 'loadLastCommits'})
			.done(function(data) {
				var $commits = $('#infobox').find('#commits');
				$commits.find('tbody').empty();
				$.each(data.values, function(idx,val) {
					if( val.parents.length > 1 ) {
						// merges ignorieren
						return;
					}

					var msg = val.message;
					var symbol = '';
					var cls = '';
					if( msg.indexOf('[') === 0 && msg.indexOf(']') !== -1 ) {
						var type = msg.substring(1, msg.indexOf(']'))
						if( type.indexOf('feature') !== -1 ) {
							cls = 'feature';
							symbol = '<span class="symbol">+</span>';
						}
						else if( type.indexOf('bug') !== -1 ) {
							cls = 'bug';
							symbol = '<span class="symbol">&#8226;</span>';
						}
					}

					$commits.find('tbody').append('<tr class="'+cls+'"><td>'+symbol+'</td><td>'+val.displayId+'</td><td>'+msg+'</td><td>'+val.author.displayName+'</td></tr>')
				});
			});
	}
};


var adminBox = {
	open : function() {
		$('#adminconsolebox').dialog({title:'Admin Konsole',width:500, height:400});

		$('#adminConsoleResponse').empty();

		var input = $('#adminConsoleCommand');
		input.autocomplete({
			source : adminBox.__autoComplete,
			minLength : 0,
			position: { my: "left bottom", at: "left top", collision: "none" },
			appendTo: '#adminconsolebox'
		});
		input.off('keypress.autosubmit')
		input.on('keypress.autosubmit', function(event){
			if( event.which == 13 ) {
				adminBox.execute();
			}
		});
	},
	__autoComplete : function(pattern, response) {
		var url = DS.getUrl();
		var params = {
				module:'admin',
				action:'ajax',
				namedplugin:'net.driftingsouls.ds2.server.modules.admin.AdminConsole',
				responseOnly:'1',
				autoComplete:'1',
				FORMAT: 'JSON',
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
		var params = {
				module:'admin',
				action:'ajax',
				namedplugin:'net.driftingsouls.ds2.server.modules.admin.AdminConsole',
				responseOnly:'1',
				FORMAT: 'JSON',
				cmd:command};
		jQuery.getJSON(DS.getUrl(), params, adminBox.showResponse);

		var respDiv = $('#adminConsoleResponse');
		respDiv.append("["+new Date().toLocaleTimeString()+"] "+command+"<br />");
		respDiv.animate({ scrollTop: respDiv.height() }, 100);
	},

	showResponse : function( response ) {
		var cls = "success";
		if( !response.success ) {
			cls = "failure";
		}
		var respDiv = $('#adminConsoleResponse');
		respDiv.append("["+new Date().toLocaleTimeString()+"] =&gt; <span class='"+cls+"'>"+response.message+"</span><br />");
		respDiv.animate({ scrollTop: respDiv.height() }, 100);
	}
};

var notizBox = {
	__timeout : false,
	init : function() {
		$('#noticebox').draggable();

		var self = this;
		$('#noticebox textarea').on('keypress', function() {
			self.onChanged();
		});
	},
	onChanged : function() {
		if( !this.__timeout ) {
			this.__timeout = true;
			var self = this;
			setTimeout(function() {
				self.__timeout = false;
				self.__speichern();
			}, 500);
		}
	},
	__speichern : function() {
		var notizen = $('#noticebox textarea').val();
		$.post(
			DS.getUrl(),
			{module:'main', action:'speicherNotizen', notizen:notizen});
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
			$('#page_currentpage')
				.text(reducedTitle)
				.css('display', 'inline');
			$('#page_currentpage_dropdown').css('display', 'none');
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
		var dl = $('#currentpage dl:first-child');
		dl.append('<dd><a target="main" href="'+url+'">'+title+'</a></dd>');

		$('#page_currentpage_dropdown').css('display', 'inline');
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
	if( $('#currentDsModule').val() === 'main' ) {
		checkPMStatus();
		notizBox.init();
		$('#helpbox').draggable();
	}
});