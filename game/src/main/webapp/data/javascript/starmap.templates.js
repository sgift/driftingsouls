const templateUserFn = user => `
    <div class="user-toggle-boundary" id="user-${user.id}">
        <div class="user-toggle clickable">
            <div style="display:flex;flex-direction:row;justify-content:space-between;">
                <div>
                    <span class="signum">+</span>
                    ${$('<div/>').html(user.name).text()}
                </div>
                <span>${getCount(user)}</span>
            </div>
        </div>
        <ul class="shipclasses dashed-list toggleContent user-sectordata" style="display:none;">
            ${templateAllShiptypesFn(user.shiptypes)}
        </ul>
    </div>
    `;


function getCount(user){
    let sum = 0;
    for(let i=0; i<user.shiptypes.length;i++)
    {
        sum += user.shiptypes[i].count;
    }
    return sum;
}

const templateAlarmRedFn = ar => `
<div class="warnung"><span>Warnung: Mindestens ein feindliches Schiff hat seine Waffensysteme aktiviert und wird angreifen, solltest Du mit einem Schiff in diesen Sektor fliegen.</span></div>
`;


const templateBattlesFn = battles => `
<div ui-if="sektor.battles.length>0" class="ng-scope">
    <ul class="battles">
        ${templateAllBattles(battles)}
    </ul>
</div>
`;

const templateAllBattles = battles => battles.map(list => { return templateBattleFn(list) + "\n" }).join("");

const templateBattleFn = battle => `
<li class="ng-scope">
            <img src="./data/interface/battle.svg">
            ${battle.einsehbar ? templateScanableBattleFn(battle) : templateNotScanableBattleFn()}
            <div class="details">
                <div class="side ng-scope">

                    ${battle.sides[0].ally ? templateAllySide(battle.sides[0]) : templateNonAllySide(battle.sides[0])}
                    ${battle.sides[1].ally ? templateAllySide(battle.sides[1]) : templateNonAllySide(battle.sides[1])}

                </div>
            </div>
        </li>
`;

const templateScanableBattleFn = battle => `<div class="name ng-scope"><a href="./ds?module=angriff&amp;battle=${battle.id}">Schlacht</a></div>`;
const templateNotScanableBattleFn = battle => `<div class="name ng-scope">Schlacht</div>`;

const templateAllySide = side => `<a href="ds?module=allylist&action=details&details=${side.ally.id}}">${side.ally.name}</a>`;
const templateNonAllySide = side => `<a ui-if="!side.ally" href="ds?module=userprofile&action=default&user=${side.commander.id}">side.commander.name</a>`;

const templateSubraumspaltenFn = subraumspalten => `
<div class="subraumspalten">
    <img src='./data/objects/subraumspalt.gif' />
    <div class="name">${subraumspalten} Subraumspalte${subraumspalten > 1 && 'n' || ''}</div>
</div>
`;

const templateJumpnodesFn = jns => `
    <div ui-if="sektor.jumpnodes.length>0" class="ng-scope">
        <ul class="jumpnodes">
            ${templateAllJumpnodes(jns)}
            <!-- ngRepeat: jumpnode in sektor.jumpnodes -->
        </ul>
    </div>
`;

const templateNebulaFn = nebula => `
    <div class="ng-scope" style="margin-bottom:10px;"><img class="nebel" alt="Nebel" src="${nebula.image}"></div>
`

const templateAllJumpnodes = jns => jns.map(list => { return templateJumpnodeFn(list) + "\n" }).join("");

const templateJumpnodeFn = jn => `
    <li ng-repeat="jumpnode in sektor.jumpnodes" class="ng-scope">
        <img src="./data/interface/jumpnode.svg">
        <div class="name">Sprungpunkt</div>
        <div class="details ng-binding">
            ${jn.name}
            ${jn.blocked ? templateJnBlocked(jn) : ""}
        </div>
    </li>
`;

const templateJnBlocked = jn => `
    <span> - blockiert</span>
`;

const templateBasesFn = bases => `
    <ul class="bases">
        ${templateAllBasesFn(bases)}
    </ul>
`;

const templateAllBasesFn = data => /*html*/
 data.map(list => { return templateBaseFn(list) + "\n" }).join("");

const templateBaseFn = base => `
    <li ng-repeat="base in sektor.bases" class="ng-scope">
        <img src="${base.image}">
        <div class="name">
            ${base.eigene ? templateOwnBaseFn(base) : templateForeignBaseFn(base)}

        </div>
        <div class="typ ng-binding">${base.typ}</div>
        <div class="owner ng-binding">${base.username}</div>
    </li>
`;

const templateOwnBaseFn = base => `
    <a ui-if="base.eigene" href="./base?col=${base.id}" class="ng-scope ng-binding">${base.name}</a>
`
const templateForeignBaseFn = base => `
    <span class="ng-scope ng-binding">${base.name}</span>
`

const templateShiptypeFn = shiptype => `
<li class="ng-scope shiptypetoggle">
    <div class="shiptype clickable" style="display:flex; flex-direction:row;justify-content:space-between;">
        <div>
            ${shiptype.count}x ${shiptype.name}
        </div>
        <button data-click="${shiptype.id}" class="shiptype-bind" style="border:none;background-color:transparent;">
            <img class="schiffstypengrafik" src="${shiptype.picture}">
        </button>
    </div>
    <table id="${shiptype.userId}-${shiptype.id}" style="display:none;border-left: white dashed 1px;margin-top: 2px;">
        ${templateAllShipsFn(shiptype.ships)}
    </table>
</li>
`;

 const templateAllShiptypesFn = data => /*html*/
     data.map(list => { return templateShiptypeFn(list) + "\n" }).join("");




  const templateAllShipsFn = data => /*html*/
      data.map(list => { return templateShipFn(list) + "\n" }).join("");


const templateShipFn = ship => `
<tr class="map-shiprow ${ship.isOwner ? "scanner" : ""} ${ship.landedShips != null && ship.landedShips.length > 0 ? " clickable" : ""}" ${ship.landedShips != null && ship.landedShips.length > 0 ? 'id="landed-toggle-' + ship.id + '"' : ""} data-click="${ship.isOwner ? ship.sensorRange : "0"}">
    <td class="name">
        ${ship.isOwner ? templateNameOwn(ship) : templateNameForeign(ship)}
        ${ship.fleet != undefined ? templateFleetNameFn(ship) : ""}
    </td>
    <td class="status">
        ${ship.maxGedockt > 0 ? templateGedocktFn(ship) : ""}
        ${ship.maxGelandet > 0 ? templateGelandetFn(ship) : ""}
        ${ship.maxEnergie > 0 ? templateMaxEnergyFn(ship) : ""}
        ${ship.ueberhitzung != undefined ? templateHeatFn(ship) : ""}
    </td>
    <td class="aktionen">
        ${ship.kannFliegen ? templateCanFlyFn(ship) : ""}
    </td>
</tr>
${ship.landedShips != null && ship.landedShips.length > 0 ? '<tr id="landed-on-' + ship.landedShips[0].carrierId + '" style="display:none;"><td colspan="3">' : ""}
${ship.landedShips != null && ship.landedShips.length > 0 ? AllLandedShips(ship.landedShips) : ""}
${ship.landedShips != null && ship.landedShips.length > 0 ? '</td></tr>' : ""}
`;

const AllLandedShips = landedShips => landedShips.map(list => { return templateLandedShipFn(list) + "\n" }).join("");

const templateLandedShipFn = landedShip => `
<div style="display:flex;flex-direction:row;wrap:no-wrap;justify-content:space-between;">
    <div>
        ${landedShip.count}x ${landedShip.typeName} <br/>
        Minimale Beladung:
    </div>
    <div>
        <div>
            ${landedShip.maxEnergie > 0 ? templateMaxEnergyFn(landedShip) : ""}
        </div>
        <div style="display:flex;flex-direction:row;">
            ${landedShip.ammoCargo.length > 0 ? allAvailableMinAmmo(landedShip.ammoCargo) : ""}
        </div>


    </div>
    <div>
        <a href="./ds?module=schiff&amp;action=default&amp;ship=${landedShip.id}">
            <img class="schiffstypengrafik" src="${landedShip.picture}" style="height:35px;">
        </a>
    </div>
</div>
`

const allAvailableMinAmmo = availableAmmos => availableAmmos.map(list => { return templateAmmoFn(list) + "\n" }).join("");

const templateAmmoFn = ammo => `
    <span><img class="schiffstypengrafik" src="${ammo.picture}" style="height:25px;">${ammo.amount}</span>
`;

const templateFleetNameFn = ship => `
<span class="ng-scope ng-binding">
    ${ship.fleet.name}
</span>
`

const templateCanFlyFn = ship => `
    <a title="Schiff bewegen"  href="#">
        <img class="can-fly" id="s-${ship.id}" ng-src="./data/interface/move.svg" alt="" src="./data/interface/move.svg">
    </a>
`;

const templateHeatFn = ship => `
    <span title="Triebwerksüberhitzung" class="ng-scope ng-binding">
        <img ng-src="./data/interface/ueberhitzung.svg" alt="" src="./data/interface/ueberhitzung.svg">
        ${ship.ueberhitzung}
    </span>`;

const templateNameOwn = ship => `<a href="./ds?module=schiff&amp;action=default&amp;ship=${ship.id}">${ship.name}</a>`;

const templateNameForeign = ship => `<span>${ship.name}</span>`;

const templateMaxEnergyFn = ship =>
`
    <span title="Energie">
        <img alt="" src="./data/interface/energie.gif">
        ${ship.energie}/${ship.maxEnergie}
    </span>
`;

const templateGedocktFn = ship =>
`
    <span title="gedockte Schiffe">
        <img alt="" src="./data/interface/schiffe/${ship.race}/icon_container.gif">
        ${ship.gedockt}/${ship.maxGedockt}
    </span>
`;

const templateGelandetFn = ship =>
`
    <span title="gelandete Schiffe">
        <img alt="" src="./data/interface/schiffe/${ship.race}/icon_schiff.gif">
        ${ship.gelandet}/${ship.maxGelandet}
    </span>
`;

const templateTileFn = data => /*html*/
    `<div style="position:absolute;width:500px;height:500px;top:${500*data.y}px;left:${500*data.x}px">
         <img src="${data.url}")/>
     </div>`;


const templateScansector = data => /*html*/
    `<div id="scanship-${data.shipId}" class="scanrange scanrange${data.scanRange +1}" style="position: absolute; top: ${(((data.location.y - 1) * 25) + 12.5)}px; left: ${(((data.location.x - 1) * 25) + 12.5)}px; background-color: white; "></div>`;
    //`<div id="scanship-${data.shipId}" class="scanrange scanrange${data.scanRange +1}" style="background-color: white;grid-column-start:${data.location.x-data.scanRange};grid-column-end:${data.location.x+data.scanRange};grid-row-start:${data.location.y-data.scanRange};grid-row-end:${data.location.y+data.scanRange}; "></div>`;


//const templateScannedSector = data => `<div onclick="loadSectorData(${data.x}, ${data.y}, ${data.scanner})" style="width:25px;height:25px;position:absolute;top:${data.y*25-25}px;left:${data.x*25-25}px;${data.bg != null && data.bg.image != undefined ? 'background-image:url('+data.bg.image+')' : ''}">
const templateScannedSector = data => `<div class="clickable" onclick="loadSectorData(${data.x}, ${data.y}, ${data.scanner})" style="grid-column-start:${data.x};grid-column-end:${data.x};grid-row-start:${data.y};grid-row-end:${data.y};${data.bg != null && data.bg.image != undefined ? 'background-image:url('+data.bg.image+')' : ''}">
${data.fg!=null ? data.battle ? '<img class="fg '+ (data.roterAlarm ? 'roter-alarm' : '') +' battle" src="'+data.fg+'"/>' : '<img class="fg '+ (data.roterAlarm ? 'roter-alarm' : '') +'" src="'+data.fg+'"/>' : ""}
</div>`;

const templateKnownPlaces = data => `
    <ul>
        <li>Sprungpunkte
            ${data.jumpnodes != null && data.jumpnodes.length > 0 ? "<ul>" + allKnownJns(data.jumpnodes) + "</ul>": ""}
        </li>
        <li>Handelsposten
            ${data.posten != null && data.posten.length > 0 ? "<ul>" + allKnownTps(data.posten) + "</ul>": ""}
        </li>
        <li>Basen
            ${data.bases != null && data.bases.length > 0 ? "<ul>" + allKnownBases(data.bases) + "</ul>": ""}
        </li>
    </ul>
`;

const allKnownJns = jns => jns.map(list => { return templateKnownPlacesNode(list) + "\n" }).join("");
const allKnownTps = tps => tps.map(list => { return templateKnownPlacesNode(list) + "\n" }).join("");
const allKnownBases = bases => bases.map(list => { return templateKnownPlacesNode(list) + "\n" }).join("");

const templateKnownPlacesNode = node => `
    <li>
        <input name="x" type="hidden" value="${node.x}"/>
        <input name="y" type="hidden" value="${node.y}"/>
        <input class="single-goto-location clickable goto-link" type="button" value="${node.x}/${node.y} - ${node.name}">
    </li>
`;