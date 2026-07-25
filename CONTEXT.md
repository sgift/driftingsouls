# Drifting Souls

A browser-based space strategy game. Players own **Bases** and **Ships** within a persistent shared
universe that advances in scheduled steps called **Ticks**.

Identifiers, comments and log messages are historically German. English is canonical going forward:
new code uses the English term, and renamed code is renamed to the English term rather than to another
German one. The `_Avoid_` entries below are the German forms still present in the codebase.

## Language

### Universe and map

**Sector**:
A single addressable position in the universe, identified by star system plus x/y coordinates.
_Avoid_: Feld, Field

**Star System**:
A named region of the map containing sectors. Reachable from other systems via jump nodes.
_Avoid_: System (unqualified)

**Nebula**:
A sector-level phenomenon that damages ships within it and obscures sensors.
_Avoid_: Nebel

**Jump Node**:
A fixed link between two sectors, usually in different star systems, that ships can traverse.
_Avoid_: Sprungpunkt

### Bases and buildings

**Base**:
A planetary settlement owned by a player. Has a grid of fields on which Buildings are placed, and
holds Cargo and Units.
_Avoid_: Basis, Basen, Kolonie

**Building**:
A *kind* of structure that can be placed on a Base field — the type and its behaviour, shared by all
Bases. Distinct from the per-Base state of an individual placed building (see the naming rule below).
_Avoid_: Gebaeude

**Factory**:
The Building that produces Items on a Base.
_Avoid_: Fabrik

**Barracks**:
The Building that trains Units on a Base.
_Avoid_: Kaserne

**Academy**:
The Building that trains Officers on a Base.
_Avoid_: Akademie

**Research Centre**:
The Building on a Base at which Research is carried out.
_Avoid_: Forschungszentrum

**Command Centre**:
The Building that establishes a Base as owned and provides its core administrative functions.
_Avoid_: Kommandozentrale

### Ships

**Ship**:
A player-owned vessel occupying a Sector. The primary mobile unit of play.
_Avoid_: Schiff

**Ship Type**:
The template a Ship is built from, defining its base statistics and capabilities.
_Avoid_: Schiffstyp

**Fleet**:
A named group of Ships commanded together.
_Avoid_: Flotte

**Shipyard**:
A facility that builds and repairs Ships. Exists either on a Base or aboard a Ship; several may be
joined into a Shipyard Complex.
_Avoid_: Werft

**Ship Blueprint**:
The buildable specification of a Ship Type, held as an Item.
_Avoid_: Schiffsbauplan

**Ship Module**:
An Item fitted into a Ship Type's slot to modify its statistics.
_Avoid_: Schiffsmodul

**Officer**:
A trained individual assigned to a Ship or Base who improves its performance.
_Avoid_: Offizier

**Unit**:
Crew or troops carried by a Ship or stationed on a Base, used for boarding and defence. Not a
synonym for Ship.
_Avoid_: Einheit

### Combat

**Battle**:
An engagement between opposing Ships in a Sector, resolved over successive rounds.
_Avoid_: Schlacht, Angriff, Kampf

**Battle Ship**:
A Ship's participation in a Battle — its side, state and standing within that engagement. Not a
class of vessel.
_Avoid_: Schlachtschiff

### Economy

**Item**:
Any discrete ownable object: commodities, ammunition, ship modules and blueprints are all Items.
_Avoid_: Gegenstand

**Commodity**:
An Item that exists purely to be traded and has no other function. A kind of Item, not a synonym
for one.
_Avoid_: Ware, Handelsware

**Cargo**:
The quantified collection of Items held by a Ship or Base.
_Avoid_: Ladung

**Ammunition**:
An Item consumed by weapons during a Battle.
_Avoid_: Munition

**Trade Offer**:
A player-created offer to exchange goods with other players. Player-to-player commerce.
_Avoid_: Handel, Handelsangebot

**Trading Post**:
A Ship at which goods can be bought and sold at posted prices. Distinct from a Trade Offer: this is
commerce with the station, not with another player.
_Avoid_: Handelsposten, Tradepost

**Auction**:
A timed sale in which players bid for a Ship or a quantity of a resource.
_Avoid_: Versteigerung

**GTU**:
An in-game organisation that operates Trading Posts, Auctions and depots. A proper noun — never
translated. Written `GTU` in prose; casing in identifiers is currently inconsistent.

### Players and organisations

**User**:
An account and the player behind it, owning Bases, Ships and Cargo.
_Avoid_: Spieler, Benutzer

**Alliance**:
An organisation of Users who cooperate, with its own membership, ranks and offices. Note that the
existing `Ally` classes denote the Alliance itself, not an allied party — this is a mistranslation
and `Alliance` is canonical.
_Avoid_: Allianz, Ally

**Alliance Post**:
A named office within an Alliance, held by a member.
_Avoid_: AllyPosten, Posten

**Faction**:
A non-player organisation in the game world with which Users interact.
_Avoid_: Fraktion

**Race**:
The species a User belongs to, affecting available Ship Types and Research.
_Avoid_: Rasse

**Rank**:
A User's standing, either within the game generally or within an Alliance.
_Avoid_: Rang

**Research**:
A technology a User unlocks at a Research Centre, gating Ship Types, Buildings and Items.
_Avoid_: Forschung

### Communication and time

**Comnet**:
The in-game public messaging network, organised into channels. A proper noun; not translated.

**PM**:
A private message between Users. Kept as `PM` — it is the term the UI uses.
_Avoid_: Nachricht

**Tick**:
The scheduled step that advances game state — production, movement, training and respawns. A
*regular tick* runs several times daily; a *rare tick* runs once daily.

## Naming

**Building type vs. instance.** A Building is a kind of structure; each Base holds its own state for a
placed building. The type/behaviour class is named `<Concept>Building` (`AcademyBuilding`); the
per-Base state is the bare `<Concept>` (`Academy`). `Fabrik`, `Werft` and `Kommandozentrale` predate
this rule and are to be renamed `FactoryBuilding`, `ShipyardBuilding` and `CommandCenterBuilding`.

**Shipyards.** `Shipyard` is the abstract facility; `BaseShipyard` and `ShipShipyard` are the two
concrete kinds; `ShipyardComplex` is several joined together.

**Factory vs. factory.** `Factory` in a domain name means the production Building. The
object-construction pattern (`RouteFactory`, `TemplateViewResultFactory`) is unrelated — do not read a
domain meaning into it.
