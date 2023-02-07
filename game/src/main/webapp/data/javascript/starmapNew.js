function LoadSystem(formdata)
{
    let systemId = formdata.get('system');
    var url = getUrl();
    gotoLocation = {x:formdata.get('x'), y:formdata.get('y')};
    jQuery.getJSON(url,{action:'get_system_data', system:systemId}, function(resp)
    {
        renderBaseSystem(resp);
        jQuery.getJSON(url, {action:'GET_SCANFIELDS', system:systemId}, function(resp){addScansectors(resp);derenderLoader();});
        jQuery.getJSON(url, {action:'GET_SCANNED_FIELDS', system:systemId}, function(resp){addScannedFields(resp);derenderLoader();});
    });

}

function ReloadSystem()
{
    var url = getUrl();
    var system = starmap.getSystem();

    counter = 0;
    $("#starmaploader").dialog("open");

    if(system == null) return;

    var systemId = system.system;
    jQuery.getJSON(url, {action:'GET_SCANFIELDS', system:systemId}, function(resp){addScansectors(resp);derenderLoader();});
    jQuery.getJSON(url, {action:'GET_SCANNED_FIELDS', system:systemId}, function(resp){addScannedFields(resp); derenderLoader();});


}

var counter = 0;
var gotoLocation = null;
function derenderLoader()
{
    counter++;
    if(counter == 2) $("#starmaploader").dialog("close");
    if(gotoLocation != null)
    {
        starmap.setCoordinates(gotoLocation.x, gotoLocation.y);
        gotoLocation = null;
    }
}

function renderBaseSystem(data)
{
    starmap.setSystem(data);

    if(starmap.elementWidth() > data.width*25)
    {
        var temp = document.getElementById("draggable");
        temp.style.removeProperty("right");
        temp.style.width = data.width*25 + 'px';
    }

    var backgroundImages = "";
    var url = DS.getUrl() + "?module=map&action=tile&sys=" + data.system;
    for(y=0;y<data.width/20;y++)
    {
        //backgroundImages += '<div  class="" style="display: inline-flex; flex-grow: 1; flex-direction: row;">'
        for(x=0;x<data.width/20;x++)
        {
            var tile = {url:url + "&tileX=" + x + "&tileY=" + y, x:x, y:y};
            backgroundImages += templateTileFn(tile);
        }
        //backgroundImages += "</div>"
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



function addScansectors(json) {
    var container = document.getElementById("scansectors");
    const display = container.style.display;
    container.style.display = "none";
    container.style.width = starmap.getSystem().width*25 +'px';
    container.style.height = starmap.getSystem().height*25 +'px';

    while (container.firstChild) {
                container.removeChild(container.lastChild);
              }

    for (let index = 0; index < json.length; index++) {
        const element = json[index];
        container.appendChild(parseHTML(templateScansector(element)));

        var scanship = document.getElementById("scanship-" + element.shipId);
        starmap.registerScanship(element);
    }
    container.style.display = display;
}

function addScannedFields(json)
{
    var container = document.getElementById("scannedSectors");
    container.style.gridTemplateColumns = `repeat(${starmap.getSystem().width}, 25px)`;
    container.style.gridTemplateRows = `repeat(${starmap.getSystem().height}, 25px)`;
    const display = "grid";
    container.style.display = "none";
        while (container.firstChild) {
            container.removeChild(container.lastChild);
          }

        for (let index = 0; index < json.locations.length; index++) {
            const element = json.locations[index];

            starmap.registerLocation({x:element.x ,y:element.y ,shipId:element.scanner});

            if(element.fg == null && (element.bg == null || element.bg.image == null)) continue;

            var scanfield = document.getElementById("scanfield-" + element.scanner);

            if(scanfield == null)
            {
                if(element.scanner == -1 || element.scanner == 0)
                {
                    var rockScanship = getRockScannerIndex(element.x, element.y);
                    scanfield = document.getElementById("scanfield-" + rockScanship.shipId);
                    if(scanfield == null) container.appendChild(parseHTML('<div id="scanfield-' + rockScanship.shipId + '" style="position:absolute;top:0px;left:0px;display:contents;"></div>'));
                    scanfield = document.getElementById("scanfield-" + rockScanship.shipId);

                    //starmap.registerScanship(rockScanship);
                }
                else{
                    container.appendChild(parseHTML('<div id="scanfield-' + element.scanner + '" style="position:absolute;top:0px;left:0px;display:contents;"></div>'));
                    scanfield = document.getElementById("scanfield-" + element.scanner);
                }

            }

            scanfield.appendChild(parseHTML(templateScannedSector(element)));
        }
        container.style.display = display;
}

function getRockScannerIndex(x, y)
{
    var rockScannerWidth = 20; // equals roughly a square of 20x20

    var scanPosX = Math.ceil(x/rockScannerWidth) * rockScannerWidth - rockScannerWidth/2;
    var scanPosY = Math.ceil(y/rockScannerWidth) * rockScannerWidth - rockScannerWidth/2;

    var shipId = -Math.round((Math.ceil(x/rockScannerWidth)+Math.ceil(y/rockScannerWidth) * Math.ceil(starmap.getSystem().width / rockScannerWidth)));
    var result = {location:{x:scanPosX, y:scanPosY, system: starmap.getSystem().system}, shipId: shipId, ownerId:-1, scanRange: 16 };

    return result;
}

var starmap = new Starmap();

function loadSectorData(x, y, scanship)
{
    var system = starmap.getSystem();

    var header = document.getElementById("starmapSectorPopup").querySelector(".header");
    header.textContent = `Lade Sektor ${system.system}:${x}/${y}`;

    //starmap?action=GET_SECTOR_INFORMATION&system=sys&x=x&y=y&scanship=id
    $("#starmaploader").dialog("open");
    $("#starmapSectorPopup").dialog("close");
    jQuery.getJSON(getUrl(),{action:'GET_SECTOR_INFORMATION', system:system.system, x:x, y:y, scanship:scanship, admin:system.admin}, function(resp){renderSectorData(resp); $("#starmaploader").dialog("close");});
}

function loadStarSystemMap()
{
    jQuery.getJSON(getUrl(),{action:'GET_STARSYSTEM_MAP_DATA'},
    function(resp){
        generateSystemMap(resp);
        $("#starsystem-map").dialog("open");
        document.getElementById("starsystem-map").style.overflowY='hidden';
    });
}

function renderSectorData(data)
{

    var container = document.getElementById("starmapSectorPopup");
    var sektor = container.querySelector("#sektoranzeige");
    //var container = document.getElementById("sektoranzeige");
    sektor.innerHTML = "";

    var header = container.querySelector(".header");
    header.textContent = `Sektor ${data.system}:${data.x}/${data.y}`;

    if(data.roterAlarm > 0) sektor.appendChild(parseHTML(templateAlarmRedFn(data.roterAlarm)));
    if(data.nebel != undefined) sektor.appendChild(parseHTML(templateNebulaFn(data.nebel)));
    if(data.jumpnodes.length > 0) sektor.appendChild(parseHTML(templateJumpnodesFn(data.jumpnodes)));
    if(data.bases.length > 0) sektor.appendChild(parseHTML(templateBasesFn(data.bases)));
    if(data.battles.length>0) sektor.appendChild(parseHTML(templateBattlesFn(data.battles)));
    if(data.subraumspaltenCount.length>0) sektor.appendChild(parseHTML(templateSubraumspaltenFn(data.subraumspaltenCount)));


    for(let index =0; index < data.users.length; index++)
    {
        sektor.appendChild(parseHTML(templateUserFn(data.users[index])));
    }

    var userToggles = document.querySelectorAll(".user-toggle-boundary");
    for(var i=0;i<userToggles.length;i++)
    {
        var userToggle = userToggles[i];

        let trigger = userToggle.querySelector(".user-toggle");
        let switchable = userToggle.querySelector(".shipclasses");
        let signum = userToggle.querySelector(".signum");

        var eventFunction = () =>
        {
            if(switchable.style.display == "none")
            {
                switchable.style.removeProperty("display");
                signum.textContent = "-";
            }
            else
            {
                switchable.style.display = "none";
                signum.textContent = "+";
            }
        };

        trigger.addEventListener("click", eventFunction.bind(null, switchable, signum) );
    }

    sektor.style.display = "flex";
    $("#starmapSectorPopup").dialog("open");
    var userSectordatas = container.querySelectorAll(".user-sectordata");

    var test = (element) =>
            {
                if(element.style.display == "none")
                {
                    element.style.removeProperty("display");
                }
                else
                {
                    element.style.display = "none";
                }
            }

    for(var i=0;i<userSectordatas.length;i++)
    {
        var toggles = userSectordatas[i].querySelectorAll(".shiptypetoggle");
        for(var j=0;j<toggles.length;j++)
        {
            var temp = toggles[j].querySelector("table");



            toggles[j].querySelector(".shiptype").addEventListener("click", test.bind(null, temp));

            /*var ships = toggles[j].querySelectorAll(".can-fly");
            AddFlyEventToShips(ships);*/
        }
    }

    for(let i=0; i<data.users.length;i++)
    {
        for(let j=0;j<data.users[i].shiptypes.length;j++)
        {
            for(let k=0;k<data.users[i].shiptypes[j].ships.length;k++)
            {
                let ship = data.users[i].shiptypes[j].ships[k];
                if(!ship.isOwner) continue;

                var shiprow = document.querySelector("#user-" + data.users[i].id );

                var shipNode = document.querySelector("#s-" + ship.id);
                if(shipNode == null) continue;
                //console.log(shipNode);

                var eventFunction = (ship) =>
                {
                    starmap.setCurrentShip(ship);
                };

                shipNode.addEventListener("click", eventFunction.bind(null, ship));

                if(ship.landedShips.length > 0)
                {
                    var temp = document.getElementById("landed-on-" + ship.id);
                    console.log("#landed-on-" + ship.id);
                    document.getElementById("landed-toggle-" + ship.id).addEventListener("click", test.bind(null, temp));
                }

                var scanrangeToggleNode = shipNode.closest(".scanner");
                scanrangeToggleNode.addEventListener("mouseover", function(){showScanrange(ship);}.bind(null, ship));
                scanrangeToggleNode.addEventListener("mouseout", function(){removeScanrange();});

            }
        }
    }

    var shiptypeLinks = document.querySelectorAll(".shiptype-bind");

    for(let i=0;i<shiptypeLinks.length; i++)
    {
        let shiptypelink = shiptypeLinks[i];
        let typeId = shiptypelink.getAttribute("data-click");

        shiptypelink.addEventListener("click", function(){
            getShiptypeData(typeId)
         }.bind(null, typeId));
    }

    $("#starmaploader").dialog("close");
    document.getElementById("sektoranzeige").style.display = "flex";
}

function AddFlyEventToShips(ships)
{
    /*for(let i=0;i<ships.length;i++)
    {
        var ship = ships[i];
        var shipId = parseInt(ship.id.substring(2));

        ship.addEventListener("click", function(){starmap.setCurrentShip(shipId);}.bind(null, shipId));
    }*/
}

function toggleByDataTarget(element, display)
{
    if(display == null) display = "block";
    var target = document.getElementById(element.dataset.toggleTarget);

    target.style.display=display;
}

document.getElementById("sektoranzeige").style.flexDirection="column";

function RenderLog(message)
{
    var logContainer = document.querySelector("#kartenaktionslog .logentries");

    var message = `<div class="logentry ng-scope" >
                        ${message.log}
                     </div>`;

    logContainer.appendChild(parseHTML(message));
}

function getShiptypeData(type)
{
    console.log(type);
    ShiptypeBox.show(type);
    document.getElementById("shiptypeBox").closest(".ui-dialog").style.zIndex = 300;
}

var highlightScanrange;
function showScanrange(ship)
{
    console.log(ship);
    var marker = document.getElementById("scanrange-marker");
    highlightScanrange = "scanrange" + (ship.sensorRange+1);

    marker.classList.add(highlightScanrange);

    marker.style.top = ((ship.y -1) * 25) + 12.5 + "px";
    marker.style.left = ((ship.x - 1) *25) +12.5 +"px";

    marker.style.backgroundColor = "rgba(0, 255, 0, 0.15)";
    marker.style.removeProperty("display");
}

function removeScanrange()
{
    var marker = document.getElementById("scanrange-marker");
    marker.classList.remove(highlightScanrange);
    marker.style.display = "none";
}