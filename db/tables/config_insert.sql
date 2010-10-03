INSERT INTO config (
	`name` ,
	`value` ,
	`description` ,
	`version` 
)
VALUES (
	'ticks', '2774', 'Der aktuelle Tick', '0'
), (
	'disablelogin', '', 'Begruendung, weshalb der Login abgeschalten ist (Leeres Feld == Login moeglich)', '0'
), (
	'disableregister', '', 'Begruendung, weshalb man sich nicht registrieren kann (Leeres Feld == registrieren moeglich)', '0'
), (
	'keys', '*', 'Schluessel mit denen man sich registrieren kann, wenn der Wert * ist braucht man keinen Schluessel zum registrieren', '0'
), (
	'foodpooldegeneration', '0', 'Prozent des Pools, die pro Tick verfaulen', '0'
), (
	'bvsizemodifier', 1, 'Faktor fuer die Groesse bei der Battle Value Formel', '0'
), (
	'bvdockmodifier', 1, 'Faktor fuer die Dockanzahl bei der Battle Value Formel', '0'
), (
	'endtiemodifier', 5, 'Faktor fuer die Anzahl der Schiffe, die man mehr haben muss, um einen Kampf unentschieden zu beenden', '0'
), (
	'vacpointspervactick', 7, 'Vacationpunkte, die ein Tick im Vac kostet', '0'
), (
	'vacpointsperplayedtick', 1, 'Vacationpunkte, die ein gespielter Tick bringt', '0'
),(
	'truemmer_maxitems', 4, 'Die maximale Anzahl an Gegenstaenden (Items/Waren) pro Schiffs-Truemmerteil', '0'
),(
	'nocrewhulldamagescale', '2', 'Skalierungsfaktor fuer den Huellenschaden durch zu wenig Crew', '0'
),(
	'tax', '2', 'Steuern (RE), die ein Arbeiter produziert', '0'
), (
	'socialsecuritybenefit', '1', 'Sozialhilfe (RE), die ein Arbeitsloser kostet', '0'
), (
	'desertedmeetingpoint', '1:15/15', 'Treffpunkt, bei dem alle desertierten Schiffe landen.', '0'
), (
	'adcost', '10', 'Preis, den ein Handelsinserat pro Tick kostet.', '0'
), (
	'immigrationfactor', '1', 'Wert zwischen 0 und 1, der angibt welcher Anteil des freien Platzes maximal von neuen Einwanderern belegt wird.', '0'
), (
	'randomizeimmigration', 'false', 'Zufallswert zwischen 0 und dem Maximum an Einwanderern waehlen.', '0'
), (
	'maxoffstotrain', '20', 'Laenge der Offiziersbauschlange', '0'
), (
	'newoffsiliziumcosts', '20', 'Siliziumkosten fuer einen neuen Offizier', '0'
), (
	'newoffnahrungcosts', '30', 'Nahrungskosten fuer einen neuen Offizier', '0'
), (
	'offdauercosts', '8', 'Laenge der Offiziersausbildung', '0'
), (
	'offnahrungfactor', '1.5', 'Faktor fuer die Nahrungskosten beim Offizierstraining.', '0'
), (
	'offsiliziumfactor', '1', 'Faktor fuer Siliziumkosten beim Offizierstraining.', '0'
), (
	'offdauerfactor', '0.25', 'Faktor fuer die Zeitdauer beim Offizierstraining.', '0'
), (
	'tick', '0', 'Sperrt Accounts waehrend des Ticks. 0 fuer keine Sperre, 1 fuer Sperre.', '0'
), (
	'gtudefaultdropzone', '75', 'Standard-Dropzone der GTU. Kann von jedem genutzt werden, egal, ob er Asteroiden im System hat.', '0'
), (
	'maxverhungern', '100', 'Prozentsatz der Crew die maximal pro Tick verhungert', '0'
), (
	'repaircostdampeningfactor', '0.3', 'Limitiert die Reperaturkosten auf die Baukosten * faktor.', '0'
), (
	'starmapsecret', 'irgendeinZufallswert', 'Wird benutzt, um Anfragen an die Sternenkarte zu checken. Nicht grundlos aendern!', '0'
);