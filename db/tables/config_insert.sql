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
);
