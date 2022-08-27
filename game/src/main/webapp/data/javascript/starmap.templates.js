const templateUserFn = user => `
    <div class="user-toggle-boundary">
        <div class="user-toggle" style="display:flex;flex-direction:row;justify-content:space-between;">
            <span><span class="signum">+</span>${user.name}</span>
            <span>${getCount(user)}</span>
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

const templateJumpnodesFn = jns => `
    <div ui-if="sektor.jumpnodes.length>0" class="ng-scope">
        <ul class="jumpnodes">
            ${templateAllJumpnodes(jns)}
            <!-- ngRepeat: jumpnode in sektor.jumpnodes -->
        </ul>
    </div>
`;

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
    <div class="shiptype">
        ${shiptype.count}x ${shiptype.name}
        <a href="./ds?module=schiffinfo&amp;ship=${shiptype.id}">
            <img class="schiffstypengrafik" src="${shiptype.picture}">
        </a>
    </div>
    <table id="${shiptype.userId}-${shiptype.id}" style="display:none;">
        ${templateAllShipsFn(shiptype.ships)}
    </table>
</li>
`;

 const templateAllShiptypesFn = data => /*html*/
     data.map(list => { return templateShiptypeFn(list) + "\n" }).join("");



  const templateAllShipsFn = data => /*html*/
      data.map(list => { return templateShipFn(list) + "\n" }).join("");


const templateShipFn = ship => `
<tr>
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
        <img alt="" src="./data/interface/schiffe/${ship.ownerrace}/icon_schiff.gif">
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
const templateScannedSector = data => `<div onclick="loadSectorData(${data.x}, ${data.y}, ${data.scanner})" style="grid-column-start:${data.x};grid-column-end:${data.x};grid-row-start:${data.y};grid-row-end:${data.y};${data.bg != null && data.bg.image != undefined ? 'background-image:url('+data.bg.image+')' : ''}">
${data.fg!=null ? '<img src="'+data.fg+'"/>' : ''}
</div>`;