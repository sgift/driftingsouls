function LoadSystem(systemId)
{
    var url = getUrl();
    jQuery.getJSON(url,{action:'get_system_data', system:systemId}, function(resp)
    {
        renderBaseSystem(resp);
        jQuery.getJSON(url, {action:'GET_SCANFIELDS', system:systemId}, function(resp){addScansectors(resp)});
        jQuery.getJSON(url, {action:'GET_SCANNED_FIELDS', system:systemId}, function(resp){addScannedFields(resp)});
    });
}

function LoadTestSystem()
{
jQuery.getJSON(getUrl,{action:'get_system_data', system:80}, function(resp){renderBaseSystem(resp);});
addScansectors(testScanranges);
addScannedFields(testScannedFields);
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
}

function addScannedFields(json)
{
    var container = document.getElementById("scannedSectors");
        while (container.firstChild) {
            container.removeChild(container.lastChild);
          }

        for (let index = 0; index < json.locations.length; index++) {
            const element = json.locations[index];

            if(element.fg == null && (element.bg == null || element.bg.image == null)) continue;

            var scanfield = document.getElementById("scanfield-" + element.scanner);

            if(scanfield == null)
            {
                if(element.scanner == -1 || element.scanner == 0)
                {
                    var rockScanship = getRockScannerIndex(element.x, element.y);
                    scanfield = document.getElementById("scanfield-" + rockScanship.shipId);
                    if(scanfield == null) container.appendChild(parseHTML('<div id="scanfield-' + rockScanship.shipId + '" style="position:absolute;inset:0px;display:block;"></div>'));
                    scanfield = document.getElementById("scanfield-" + rockScanship.shipId);

                    starmap.registerScanship(rockScanship);
                }
                else{
                    container.appendChild(parseHTML('<div id="scanfield-' + element.scanner + '" style="position:absolute;inset:0px;display:block;"></div>'));
                    scanfield = document.getElementById("scanfield-" + element.scanner);
                }

            }

            scanfield.appendChild(parseHTML(templateScannedSector(element)));
        }
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

    jQuery.getJSON(DS.getURL(),{FORMAT:'JSON', module:'map', action:'sector', sys:system.system, x:x, y:y, scanship:scanship, admin:system.admin}, function(resp){renderSectorData(resp)});
}

function renderSectorData(data)
{
    var container = document.getElementById("sektoranzeige");
    container.innerHTML = "";

    for(let index =0; index < data.users.length; index++)
    {
        container.appendChild(parseHTML(templateUserFn(data.users[index])));
    }
}
