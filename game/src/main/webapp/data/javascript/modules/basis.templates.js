

const templatebuildingActions = (data,baseid) => /*html*/ `
    <li class="buildingAction" data-on="true" data-off="true">
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
        <a class="tooltip" href="${building.url}?module=building&amp;col=${building.col}&amp;field=${building.field}">
            <span class="ttcontent">${building.geb_name}</span>
            <img style="border:0px" src="${building.bildpfad}" alt="">
        </a>
    </div>`;

const templateEmptyBuildingSpaceFn = building =>
    `<div>
        <div class="p${building.field} bebaubar fadein" data-overlay="false" data-field="${escape(building.field)}">
            <img style="border:0px" src="${building.ground}" alt="">
        </div>
    </div>
    </div>`;

const templateCargoFn = ress  =>
    `<table>
        <tbody>
            <tr class="myTemplateIdentifier">
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
                    ${ress.produktion != 0 ? `<a class="cargo2 ${ress.produktion > 0 ? "positiv" : "negativ"} tooltip ${ress.prodaenderung ? 'fadein':''}" href="${ress.url}?module=iteminfo&amp;itemlist=${ress.ress_id}">${ress.produktion.toLocaleString()}<span class="ttcontent ttitem" ds-item-id="${ress.ress_id}"><img src="${ress.bildpfad}" alt="" align="left"><span>${ress.ress_name}</span></span></a>`:``}
                </td>
            </tr>
        </tbody>
    </table>`;

