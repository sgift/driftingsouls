function LoadSystem(systemId)
{
    var url = getUrl();
    jQuery.getJSON(url,{action:'get_system_data', system:systemId}, function(resp){renderBaseSystem(resp)});
    jQuery.getJSON(url, {action:'GET_SCANFIELDS', system:systemId}, function(resp){addScansectors(resp)});
    jQuery.getJSON(url, {action:'GET_SCANNED_FIELDS', system:systemId}, function(resp){addScannedFields(resp)});
}

function renderBaseSystem(data)
{
    var backgroundImages = "";
    var url = DS.getUrl() + "?module=map&action=tile&sys=" + data.system;
    for(y=0;y<data.width/20;y++)
    {
        backgroundImages += '<div  class="" style="display: inline-flex; flex-grow: 1; flex-direction: row;">'
        for(x=0;x<data.width/20;x++)
        {
            var tileUrl = url + "&tileX=" + x + "&tileY=" + y;
            backgroundImages += templateTileFn(tileUrl);
        }
        backgroundImages += "</div>"
    }

    var parent = document.getElementById("tiles");
    parent.innerHTML = "";
    parent.appendChild(parseHTML(backgroundImages));

    renderXLegend(data.width);
    renderYLegend(data.height);
}

function renderXLegend(width)
{
    var containers = document.querySelectorAll(".scroll-x");
    containers[0].innerHTML="";
    containers[1].innerHTML="";
    for(i=1;i<=width;i++)
    {
        containers[0].appendChild(parseHTML('<div style="width:25px;height:25px;line-height:20px;text-align:center;vertical-align:middle;">'+ i +'</div>'));
        containers[1].appendChild(parseHTML('<div style="width:25px;height:25px;line-height:20px;text-align:center;vertical-align:middle;">'+ i +'</div>'));
    }
}

function renderYLegend(height)
{
    var containers = document.querySelectorAll(".scroll-y");
    containers[0].innerHTML="";
    containers[1].innerHTML="";
    for(i=1;i<=height;i++)
    {
        containers[0].appendChild(parseHTML('<div style="width:25px;height:25px;line-height:20px;text-align:center;vertical-align:middle;">'+ i +'</div>'));
        containers[1].appendChild(parseHTML('<div style="width:25px;height:25px;line-height:20px;text-align:center;vertical-align:middle;">'+ i +'</div>'));
    }
}

function getUrl(){
	var url = DS.location.getCurrent();
	if( url.indexOf('?') > -1 )
	{
		url = url.substring(0,url.indexOf('?'));
	}
	if( url.indexOf('#') > -1 ) {
		url = url.substring(0,url.indexOf('#'));
	}
	if( url.indexOf('/ds',url.length-3) != -1 ) {
		url = url.substring(0,url.lastIndexOf('/'));
	}
	return url;
}

const templateTileFn = data => /*html*/
    `<div style="position:relative;width:500px;height:500px;">
         <img src="${data}")/>
     </div>`;


const templateScansector = data => /*html*/
    `<div class="scanrange scanrange${data.scanRange +1}" style="position: absolute; top: ${(((data.location.y - 1) * 25) + 12.5)}px; left: ${(((data.location.x - 1) * 25) + 12.5)}px; background-color: white; "></div>`;

const templateScannedSector = data => `<div style="width:25px;height:25px;position:absolute;top:${data.y*25-25}px;left:${data.x*25-25}px;${data.bg != null && data.bg.image != undefined ? 'background-image:url('+data.bg.image+')' : ''}">
${data.fg!=null ? '<img src="'+data.fg+'"/>' : ''}
</div>`;

function addScansectors(json) {
    var container = document.getElementById("scansectors");
    container.innerHTML = "";

    for (let index = 0; index < json.length; index++) {
        const element = json[index];
        container.appendChild(parseHTML(templateScansector(element)));
    }
}

function addScannedFields(json)
{
    var container = document.getElementById("scannedSectors");
        container.innerHTML = "";

        for (let index = 0; index < json.locations.length; index++) {
            const element = json.locations[index];
            container.appendChild(parseHTML(templateScannedSector(element)));
        }
}

var starmap = new Starmap();

const templateChooseSystem = data => `<div class="ui-dialog ui-widget ui-widget-content ui-corner-all ui-front ui-draggable ui-resizable" tabindex="-1" role="dialog" aria-describedby="systemauswahl" aria-labelledby="ui-id-4" style="position: absolute; height: 184px; width: 400px; inset: 242px auto auto 937px; display: block;">
<div class="ui-dialog-titlebar ui-widget-header ui-corner-all ui-helper-clearfix ui-draggable-handle">
<span id="ui-id-4" class="ui-dialog-title">Kartenausschnitt</span>
<button type="button" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-icon-only ui-dialog-titlebar-close" role="button" title="Close">
<span class="ui-button-icon-primary ui-icon ui-icon-closethick"></span>
<span class="ui-button-text">Close</span>
</button>
</div>
<div id="systemauswahl" ds-popup="systemSelection" ds-popup-title="Kartenausschnitt" ds-popup-width="400" ng-controller="MapSystemauswahlController" class="ng-scope ui-dialog-content ui-widget-content gfxbox" style="width: auto; min-height: 100px; max-height: none; height: auto;">

<form ng-submit="sternenkarteLaden()" class="ng-pristine ng-valid">
    <table cellpadding="3">
        <tbody><tr>
            <td colspan="2">
                System<br>
                <select name="sys" ng-model="systemSelected" ng-options="sys.label for sys in systeme" class="ng-pristine ng-valid"><option value="0" selected="selected">Schatten (80) </option><option value="1">DynJN Testsystem (81) </option><option value="2">test (82) </option><option value="3">Arvas Testsystem1 (83) </option><option value="4">Rho (88) </option><option value="5">Eta Nebular (91) </option><option value="6">Prospekt 1 (92) </option><option value="7">prospekt 2 (93) </option><option value="8">prospekt 3 (94) </option><option value="9">Neu (50x50) 1 (95) </option><option value="10">Neu (50x50) 2 (96) </option><option value="11">Neu (75x75) 1 (97) </option><option value="12">Neu (75x75) 2 (98) </option><option value="13">Neu (100x100) 1 (99) </option><option value="14">Neu (100x100) 2 (100) </option><option value="15">Neu (100x100) 3 (101) </option><option value="16">Neu (100x100) 4 (102) </option></select>
            </td>
        </tr>
        <tr>
            <td>
                Position
            </td>
            <td>
                <input type="text" name="xstart" size="3" value="1" ng-model="locationX" class="ng-pristine ng-valid">
                /
                <input type="text" name="ystart" size="3" value="1" ng-model="locationY" class="ng-pristine ng-valid">
            </td>
        </tr>
        <tr ng-show="adminSichtVerfuegbar" style="">
            <td colspan="2">
                <input type="checkbox" ng-model="adminSicht" id="adminSicht" name="adminSicht" class="ng-pristine ng-valid"><label for="adminSicht">Admin-Sicht</label>
            </td>
        </tr>
    </tbody></table>
    <input type="submit" value="Sternenkarte laden">
</form>
</div>
<div class="ui-resizable-handle ui-resizable-n" style="z-index: 90;"></div>
<div class="ui-resizable-handle ui-resizable-e" style="z-index: 90;"></div>
<div class="ui-resizable-handle ui-resizable-s" style="z-index: 90;"></div>
<div class="ui-resizable-handle ui-resizable-w" style="z-index: 90;"></div>
<div class="ui-resizable-handle ui-resizable-se ui-icon ui-icon-gripsmall-diagonal-se" style="z-index: 90;"></div>
<div class="ui-resizable-handle ui-resizable-sw" style="z-index: 90;"></div>
<div class="ui-resizable-handle ui-resizable-ne" style="z-index: 90;"></div>
<div class="ui-resizable-handle ui-resizable-nw" style="z-index: 90;"></div>
</div>`