/*
	Umschalten der Flotten/Bookmark-Box
*/
var UeberBox = {
	zeige : function(box) {
		var buttons = $('#ueber-box-umschalter button');
		buttons.removeClass('aktiv');
		buttons.filter('[data-box=' + box + ']').addClass('aktiv');

		$('#ueber-flotten').toggle(box === 'fleets');
		$('#ueber-bookmarks').toggle(box === 'bookmarks');

		DS.getJSON({module:'ueber', action:'box', box:box}, function() {});
	}
};

/*
	Discord-Chat
*/
var DiscordChat = {
	INTERVALL : 5000,
	/**
	 * Avatare, Bilder und Videos werden nur vom CDN von Discord geladen (das prueft auch der Server).
	 */
	CDN : /^https:\/\/(cdn\.discordapp\.com|[a-z0-9-]+\.discordapp\.net)\//,
	__timer : null,
	__zustimmung : false,
	__signatur : null,
	__laedt : false,
	__folgeEnde : true,

	init : function() {
		var chat = $('#discord-chat');
		if( chat.length === 0 ) {
			return;
		}

		chat.find('.discord-kanal').on('click', function() {
			DiscordChat.wechsleKanal($(this));
		});
		chat.find('.discord-nachrichten').on('scroll', function() {
			DiscordChat.__folgeEnde = this.scrollTop + this.clientHeight >= this.scrollHeight - 30;
		});
		chat.find('form.discord-eingabe').on('submit', function(event) {
			event.preventDefault();
			DiscordChat.senden();
		});
		chat.find('.discord-zustimmen').on('click', function() {
			DiscordChat.zustimmen();
		});
		chat.find('.discord-bestaetigen').on('click', function(event) {
			event.preventDefault();
			window.open('./ds?module=options&action=discordConnect', 'dsDiscord', 'width=520,height=800');
		});
		$(document).on('visibilitychange', function() {
			if( !document.hidden && DiscordChat.__zustimmung ) {
				DiscordChat.laden();
			}
		});

		this.__aktualisiereEingabe();
		// Ohne Zustimmung des Spielers wird nichts von Discord geladen
		this.__zustimmung = chat.find('.discord-zustimmung').length === 0;
		if( this.__zustimmung ) {
			this.laden();
		}
	},

	/**
	 * Vermerkt die Zustimmung, dass der Chat Inhalte von Discord laedt, und zeigt danach die Nachrichten.
	 */
	zustimmen : function() {
		var kanal = this.__kanal();
		$('#discord-chat .discord-zustimmen').prop('disabled', true);
		jQuery.ajax({
			url: DS.getUrl(),
			type: 'POST',
			data: {module:'ueber', action:'discordConsent', channel:kanal, FORMAT:'JSON'},
			dataType: 'json',
			success: function(result) {
				if( result.consentMissing ) {
					$('#discord-chat .discord-zustimmen').prop('disabled', false);
					DiscordChat.__zeigeFehler(result.error || 'Die Zustimmung konnte nicht gespeichert werden.');
					return;
				}
				DiscordChat.__zustimmung = true;
				$('#discord-chat .discord-nachrichten').empty();
				DiscordChat.__zeigeErgebnis(result, true);
				DiscordChat.__planeNaechstesLaden();
			},
			error: function() {
				$('#discord-chat .discord-zustimmen').prop('disabled', false);
				DiscordChat.__zeigeFehler('Die Zustimmung konnte nicht gespeichert werden.');
			}
		});
	},

	/**
	 * Sperrt die Eingabe in Kanaelen, in die der Spieler nur lesen darf.
	 * Das ist nur Komfort - der Server prueft das Schreibrecht bei jedem Senden selbst.
	 */
	__aktualisiereEingabe : function() {
		var chat = $('#discord-chat');
		var kanal = chat.find('.discord-kanal.aktiv');
		var darfSchreiben = kanal.attr('data-schreiben') === '1';
		var form = chat.find('form.discord-eingabe');
		form.find('input').prop('disabled', !darfSchreiben);
		var hinweis = 'In #' + kanal.attr('data-name') + ' darfst du nur mitlesen';
		if( chat.attr('data-bestaetigen') === '1' ) {
			hinweis = 'Bestätige die Verknüpfung in den Einstellungen erneut';
		}
		form.find('input[name=text]').attr('placeholder', darfSchreiben ? 'Nachricht an #' + kanal.attr('data-name') : hinweis);
	},

	__kanal : function() {
		return $('#discord-chat').attr('data-channel');
	},

	wechsleKanal : function(kanalElement) {
		var chat = $('#discord-chat');
		var kanal = kanalElement.attr('data-channel');
		if( kanal === this.__kanal() ) {
			return;
		}

		chat.attr('data-channel', kanal);
		chat.find('.discord-kanal').removeClass('aktiv');
		kanalElement.addClass('aktiv');
		chat.find('.discord-extern').attr('href', 'https://discord.com/channels/' + chat.attr('data-guild') + '/' + kanal);
		this.__aktualisiereEingabe();

		this.__signatur = null;
		chat.find('.discord-nachrichten').empty().append($('<div class="discord-hinweis"></div>').text('Lade Nachrichten...'));
		this.laden();
	},

	__planeNaechstesLaden : function() {
		clearTimeout(this.__timer);
		this.__timer = setTimeout(function() {
			if( document.hidden || !DiscordChat.__zustimmung ) {
				// Erst beim Zurueckkehren auf die Seite wieder laden (visibilitychange)
				return;
			}
			DiscordChat.laden();
		}, this.INTERVALL);
	},

	laden : function() {
		if( this.__laedt ) {
			return;
		}
		this.__laedt = true;
		var kanal = this.__kanal();

		jQuery.ajax({
			url: DS.getUrl(),
			data: {module:'ueber', action:'discordChat', channel:kanal, FORMAT:'JSON'},
			dataType: 'json',
			success: function(result) {
				if( kanal === DiscordChat.__kanal() ) {
					DiscordChat.__zeigeErgebnis(result);
				}
			},
			error: function() {
				DiscordChat.__zeigeFehler('Der Chat konnte nicht geladen werden.');
			},
			complete: function() {
				DiscordChat.__laedt = false;
				DiscordChat.__planeNaechstesLaden();
			}
		});
	},

	senden : function() {
		var form = $('#discord-chat form.discord-eingabe');
		var eingabe = form.find('input[name=text]');
		var text = $.trim(eingabe.val());
		if( text === '' ) {
			return;
		}

		var kanal = this.__kanal();
		form.find('input').prop('disabled', true);

		jQuery.ajax({
			url: DS.getUrl(),
			type: 'POST',
			data: {module:'ueber', action:'discordSend', channel:kanal, text:text, FORMAT:'JSON'},
			dataType: 'json',
			success: function(result) {
				if( !result.error ) {
					eingabe.val('');
				}
				if( kanal === DiscordChat.__kanal() ) {
					DiscordChat.__zeigeErgebnis(result, true);
				}
			},
			error: function() {
				DiscordChat.__zeigeFehler('Die Nachricht konnte nicht gesendet werden.');
			},
			complete: function() {
				DiscordChat.__aktualisiereEingabe();
				eingabe.focus();
			}
		});
	},

	__zeigeFehler : function(text) {
		var chat = $('#discord-chat');
		var fehler = chat.find('.discord-fehler');
		if( fehler.length === 0 ) {
			fehler = $('<div class="discord-fehler"></div>').insertAfter(chat.find('.discord-nachrichten'));
		}
		fehler.text(text);
	},

	__zeigeErgebnis : function(result, zumEnde) {
		var chat = $('#discord-chat');
		if( typeof result.messages === 'undefined' ) {
			// Fehlerantwort des Frameworks (z.B. nicht mehr eingeloggt)
			this.__zeigeFehler(result.message && result.message.description ? result.message.description : 'Der Chat konnte nicht geladen werden.');
			return;
		}

		if( result.consentMissing ) {
			// Zustimmung wurde in einem anderen Fenster widerrufen
			this.__zustimmung = false;
			this.__zeigeFehler('Der Chat lädt keine Inhalte mehr von Discord. Lade die Seite neu, um wieder zuzustimmen.');
			return;
		}

		if( result.channel === this.__kanal() ) {
			// Rechte koennen sich geaendert haben, seit die Seite geladen wurde
			chat.attr('data-bestaetigen', result.confirmLink ? '1' : '0');
			chat.find('.discord-kanal.aktiv').attr('data-schreiben', result.writeAccess ? '1' : '0');
			this.__aktualisiereEingabe();
		}

		if( result.error ) {
			this.__zeigeFehler(result.error);
		}
		else {
			chat.find('.discord-fehler').remove();
		}

		var signatur = $.map(result.messages, function(nachricht) { return nachricht.id; }).join(',');
		if( signatur === this.__signatur ) {
			return;
		}
		if( result.messages.length === 0 && result.error && this.__signatur !== null ) {
			// Bei einem Fehler die bisherigen Nachrichten stehen lassen
			return;
		}
		this.__signatur = signatur;

		var liste = chat.find('.discord-nachrichten');
		var amEnde = zumEnde || liste.scrollTop() + liste.innerHeight() >= liste[0].scrollHeight - 30;
		this.__folgeEnde = amEnde;

		liste.empty();
		if( result.messages.length === 0 && !result.error ) {
			liste.append($('<div class="discord-hinweis"></div>').text('Noch keine Nachrichten in diesem Kanal.'));
		}
		$.each(result.messages, function(index, nachricht) {
			liste.append(DiscordChat.__erzeugeNachricht(nachricht));
		});

		if( amEnde ) {
			liste.scrollTop(liste[0].scrollHeight);
		}
	},

	__erzeugeNachricht : function(nachricht) {
		var element = $('<div class="discord-nachricht"></div>');
		if( nachricht.ingame ) {
			element.addClass('discord-ingame');
		}

		if( this.CDN.test(nachricht.avatarUrl || '') ) {
			$('<img class="discord-avatar" alt="" />').attr('src', nachricht.avatarUrl).appendTo(element);
		}

		var kopf = $('<div class="discord-kopf"></div>').appendTo(element);
		var autor = $('<span class="discord-autor"></span>').text(nachricht.author).appendTo(kopf);
		if( /^#[0-9a-f]{6}$/.test(nachricht.color || '') ) {
			autor.css('color', nachricht.color);
		}
		if( nachricht.ingame ) {
			$('<span class="discord-ingame-marke" title="Aus Drifting Souls gesendet">DS</span>').appendTo(kopf);
		}
		else if( nachricht.webhook ) {
			$('<span class="discord-app-marke" title="Von einer Discord-Integration gesendet, nicht aus Drifting Souls">APP</span>').appendTo(kopf);
		}
		$('<span class="discord-zeit"></span>').text(this.__formatiereZeit(nachricht.timestamp)).appendTo(kopf);

		var text = $('<div class="discord-text"></div>').appendTo(element);
		this.__fuegeTextMitLinksEin(text, nachricht.text);

		$.each(nachricht.attachments || [], function(index, anhang) {
			if( !DiscordChat.CDN.test(anhang.url || '') ) {
				return;
			}
			var link = $('<a class="discord-anhang" target="_blank" rel="noopener noreferrer"></a>').attr('href', anhang.url);
			if( anhang.type === 'image' || anhang.type === 'video' ) {
				var medium = anhang.type === 'image' ?
					$('<img class="discord-medium" alt="" />').attr('alt', anhang.name || '') :
					$('<video class="discord-medium" autoplay loop muted playsinline></video>');
				DiscordChat.__setzeGroesse(medium, anhang);
				// Ohne bekannte Groesse verschiebt das Laden den Inhalt - dann ggf. wieder ans Ende scrollen
				medium.on('load loadeddata', function() {
					if( DiscordChat.__folgeEnde ) {
						var liste = $('#discord-chat .discord-nachrichten');
						liste.scrollTop(liste[0].scrollHeight);
					}
				});
				medium.attr('src', anhang.url);
				link.append(medium).appendTo(text);
			}
			else {
				link.text('[Anhang] ' + anhang.name).appendTo(text);
			}
		});

		return element;
	},

	MAX_BREITE : 320,
	MAX_HOEHE : 220,

	__setzeGroesse : function(medium, anhang) {
		if( !anhang.width || !anhang.height ) {
			return;
		}
		var faktor = Math.min(1, this.MAX_BREITE / anhang.width, this.MAX_HOEHE / anhang.height);
		medium.attr('width', Math.round(anhang.width * faktor));
		medium.attr('height', Math.round(anhang.height * faktor));
	},

	/**
	 * Fuegt den Text als reinen Text ein - nur Links und Server-Emojis (<:name:id>, animiert <a:name:id>)
	 * werden zu Elementen.
	 */
	__fuegeTextMitLinksEin : function(ziel, text) {
		var muster = /(https?:\/\/[^\s<>"]+)|<(a?):(\w{2,32}):(\d{5,25})>/g;
		var position = 0;
		var treffer;
		while( (treffer = muster.exec(text)) !== null ) {
			ziel.append(document.createTextNode(text.substring(position, treffer.index)));
			if( treffer[1] ) {
				$('<a target="_blank" rel="noopener noreferrer"></a>')
					.attr('href', treffer[1])
					.text(treffer[1])
					.appendTo(ziel);
			}
			else {
				$('<img class="discord-emoji" />')
					.attr('src', 'https://cdn.discordapp.com/emojis/' + treffer[4] + '.' + (treffer[2] ? 'gif' : 'png') + '?size=48')
					.attr('alt', ':' + treffer[3] + ':')
					.attr('title', ':' + treffer[3] + ':')
					.appendTo(ziel);
			}
			position = treffer.index + treffer[0].length;
		}
		ziel.append(document.createTextNode(text.substring(position)));
	},

	__formatiereZeit : function(timestamp) {
		var datum = new Date(timestamp);
		if( isNaN(datum.getTime()) ) {
			return '';
		}
		var zweistellig = function(zahl) { return (zahl < 10 ? '0' : '') + zahl; };
		var zeit = zweistellig(datum.getHours()) + ':' + zweistellig(datum.getMinutes());
		if( datum.toDateString() === new Date().toDateString() ) {
			return zeit;
		}
		return zweistellig(datum.getDate()) + '.' + zweistellig(datum.getMonth() + 1) + '. ' + zeit;
	}
};

$(document).ready(function() {
	$('#ueber-box-umschalter button').on('click', function() {
		UeberBox.zeige($(this).attr('data-box'));
	});

	DiscordChat.init();
});
