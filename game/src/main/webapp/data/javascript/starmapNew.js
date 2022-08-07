function LoadSystem(systemId)
{
    var url = getUrl();
    jQuery.getJSON(url,{action:'get_system_data', system:systemId}, function(resp){renderBaseSystem(resp)});
    jQuery.getJSON(url, {action:'GET_SCANFIELDS', system:systemId}, function(resp){addScansectors(resp)});
    jQuery.getJSON(url, {action:'GET_SCANNED_FIELDS', system:systemId}, function(resp){addScannedFields(resp)});
}

function renderBaseSystem(data)
{
    starmap.setSystem(data);
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