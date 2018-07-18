alter table battles drop index coords;
create index battle_coords on battles (x, y, system);
alter table jumpnodes drop index coords;
create index jumpnode_coords on jumpnodes (x, y, system);
alter table quests_running drop index questid;
create index runningquest_questid on quests_running (questid, userid);

alter table schiff_einstellungen drop index idx_feeding, drop index idx_allyfeeding, drop index bookmark;
create index schiffeinstellungen_feeding on schiff_einstellungen (isfeeding);
create index schiffeinstellungen_bookmark on schiff_einstellungen (bookmark);
create index schiffeinstellungen_allyfeeding on schiff_einstellungen (isallyfeeding);

alter table ships drop index coords, drop index owner, drop index status, drop index docked;
create index ship_coords on ships (system, x, y);
create index ship_owner on ships (owner, id);
create index ship_docked on ships (docked);
create index ship_status on ships (status);

alter table ships_lost drop index owner, drop index ally, drop index destowner, drop index destally, drop index battle, drop index battlelog;
create index shiplost_ally on ships_lost (ally);
create index shiplost_battle on ships_lost (battle);
create index shiplost_battlelog on ships_lost (battlelog);
create index shiplost_destally on ships_lost (destally);
create index shiplost_destowner on ships_lost (destowner);
create index shiplost_owner on ships_lost (owner);

alter table ships_modules drop index versorger;
create index shipmodules_versorger on ships_modules (versorger);

alter table buildings drop index category;
create index building_category on buildings (category);

alter table ship_types drop index versorger;
create index shiptype_versorger on ship_types (versorger);

alter table ship_loot drop index shiptype;
create index shiploot_shiptype on ship_loot (shiptype);

alter table scripts drop index name;
create index script_name on scripts (name);

alter table quests_completed drop index questid;
create index completedquest_questid on quests_completed (questid, userid);

alter table user_values drop index id;
create index uservalue_id on user_values (user_id, name);

alter table users drop index un;
create index user_un on users (un);
