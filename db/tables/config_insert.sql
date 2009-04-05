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
);