

const templatebuildingActions = (data,baseid) => /*html*/ `
    <li class="buildingAction" onmouseover="Base.highlightBuilding('building${data.buildingTypeId}')" onmouseout="Base.noBuildingHighlight()" data-on="true" data-off="true">
        ${data.name}
        <span>
            <a title="Gebäude deaktivieren" class="action deaktivieren" style="${data.deaktivierbar ? "" : "display:none;"}" href="/sds/base?action=changeBuildingStatus&amp;col=${baseid}&amp;buildingonoff=${data.buildingTypeId}&amp;act=0">
                <img alt="" src="./data/interface/nenergie.gif">
            </a>
            <a title="Gebäude aktivieren" class="action aktivieren" style="${data.aktivierbar ? "" : "display:none;"}" href="/sds/base?action=changeBuildingStatus&amp;col=${baseid}&amp;buildingonoff=${data.buildingTypeId}&amp;act=1">
                <img alt="" src="./data/interface/energie.gif">
            </a>
        </span>
    </li>`;


const templateBuildingFn = building =>
    `<div><div class="p${building.field} building${building.geb_id} ${building.offline} fadein">
        <a class="tooltip" onclick="Base.showBuilding(${building.field});return false;" href="${building.url}?module=building&amp;col=${building.kolonie}&amp;field=${building.field}">
            <span class="ttcontent">${building.name}</span>
            <img style="border:0px" src="${building.bildpfad}" alt="">
        </a>
    </div>`;

const templateEmptyBuildingSpaceFn = building =>
    `<div>
        <div class="p${building.field} bebaubar fadein" data-overlay="false" data-field="${building.field}" onclick="Base.BaueFeld(this.parentNode, this.getAttribute('data-field'))">
            <img style="border:0px" src="${building.ground}" alt="">								
        </div>
    </div>
    </div>`;

const templateCargoFn = ress  =>
    `<tr>
        <td>
            <img src="${ress.bildpfad}" alt="">
        </td>
        <td>
            <a class="tooltip schiffwaren " href="${ress.url}?module=iteminfo&amp;itemlist=${ress.ress_id}">
                ${ress.ress_name}
                <span class="ttcontent ttitem " ds-item-id="${ress.ress_id}"><img src="${ress.bildpfad}" alt="" align="left">
                    <span>
                        ${ress.ress_name}
                    </span>
                </span>
            </a>
        </td>
        <td>
            <a class="cargo1 schiffwaren tooltip ${ress.verbraucht? ` fadein`:``}" href="${ress.url}?module=iteminfo&amp;itemlist=${ress.ress_id}">
                ${ress.menge.toLocaleString()}
                <span class="ttcontent ttitem" ds-item-id="${ress.ress_id}">
                    <img src="${ress.bildpfad}" alt="" align="left">
                    <span>${ress.ress_name}</span>
                </span>
            </a> 
        </td>
        <td>
            ${ress.produktion != 0 ? `<a class="cargo2 ${ress.produktion > 0 ? "positiv" : "negativ"} tooltip ${ress.prodaenderung ? 'fadein':''}" href="${ress.url}?module=iteminfo&amp;itemlist=${ress.ress_id}">${ress.produktion.toLocaleString()}<span class="ttcontent ttitem" ds-item-id="${ress.ress_id}"><img src="${ress.bildpfad}" alt="" align="left"><span>{ress.ress_name}</span></span></a>`:``}
        </td>
    </tr>`;

const templateBuildingBoxFn = content =>
    `<div id="buildingBox" class="gfxbox popupbox ui-draggable ui-draggable-handle" style="inset: 462px auto auto 941px; display: block; width: 433px; height: 322px;">
		<div class="content">
			${content}
		</div>
		<button class="closebox" onclick="toggleElement(document.getElementById('buildingBox'))">schlie\u00dfen</button>
	</div>`;
