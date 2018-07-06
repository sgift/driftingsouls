insert into rasse (id, name, head_id, playable, playableext, schiffsKlassenNamenGenerator, schiffsNamenGenerator) VALUES (0, 'GCP', -10, false, false, 'KEIN_KUERZEL', 'SCHIFFSTYP');
insert into rasse (id, name, head_id, playable, memberIn_id, personenNamenGenerator, description, playableext, schiffsKlassenNamenGenerator, schiffsNamenGenerator) VALUES (1, 'Terraner', null, true, 0, 'ENGLISCH', '[align=left]
Terraner sind Menschen.
Sie stammen ursprünglich von der Erde, zu der aber keine Verbindung mehr besteht.
[list][*]Bauen kostengünstige aber weniger effiziente Schiffe
[*]Setzen primär auf Deuterium als Energiequelle (Billig &amp; weniger Effizient)
[/list]
[/align]', false, 'TERRANISCH', 'STAEDTE');
insert into rasse (id, name, head_id, playable, memberIn_id, personenNamenGenerator, description, playableext, schiffsKlassenNamenGenerator, schiffsNamenGenerator) VALUES (2, 'Vasudaner', null, true, 0, 'AEGYPTISCH', '[align=left]
Hochentwickelte Spezies.
Ihr Heimatplanet ist Vasuda Prime, ein sehr heisser Planet.
Er wurde im ersten shivanischen Krieg angegriffen und dadurch fast unbewohnbar.
Die Vasudaner haben eine sehr alte Kultur mit mehreren hochentwickelten Sprachen und Nationen.
[list][*]Teure aber effizente Schiffe
[*]Antimaterie ist ihre bevorzugte Energiequelle (Teuer &amp; Effizient)
[/list]
[/align]', false, 'VASUDANISCH', 'AEGYPTISCHE_NAMEN');
insert into rasse (id, name, head_id, playable, playableext) VALUES (3, 'Shivaner', null, false, false);
insert into rasse (id, name, head_id, playable, playableext) VALUES (4, 'Uralte', null, false, false);
insert into rasse (id, name, head_id, playable, playableext) VALUES (5, 'Nomads', null, false, false);
insert into rasse (id, name, head_id, playable, memberIn_id, personenNamenGenerator, description, playableext) VALUES (6, 'NTF', null, false, 1, null, null, true);
insert into rasse (id, name, head_id, playable, memberIn_id, personenNamenGenerator, description, playableext) VALUES (7, 'HoL', null, false, 2, null, null, false);
insert into rasse (id, name, head_id, playable, memberIn_id, personenNamenGenerator, description, playableext) VALUES (8, 'GTU', -2, false, 0, null, null, false);
insert into rasse (id, name, head_id, playable, memberIn_id, personenNamenGenerator, description, playableext) VALUES (9, 'Piraten', -15, false, 1, null, null, true);