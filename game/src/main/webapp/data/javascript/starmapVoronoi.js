var maxMapX;
var minMapX;
var maxMapY;
var minMapY;
var maxR;
var minR;
var delta = 50;
var width = 700;
var height = 500;
var ringGradient = 0;

var defContainer = document.getElementById("defs");

function generateSystemMap(newData)
{
    ringGradient = 0;

	maxR = Math.max(...newData.map(o => o.radius));
    minR = Math.min(...newData.map(o => o.radius));

    console.log(maxR);
    console.log(minR);

	for (let i=0;i<newData.length;i++) {
            let system = newData[i];
			system.cr = Math.round(0.24 * system.radius + 14,2);
    }

    console.log(Math.max(...newData.map(o => o.cr)));
    console.log(Math.min(...newData.map(o => o.cr)));

    maxMapX = Math.max(...newData.map(o => o.mapX+o.cr));
    minMapX = Math.min(...newData.map(o => o.mapX-o.cr));
    maxMapY = Math.max(...newData.map(o => o.mapY+o.cr-20));
    minMapY = Math.min(...newData.map(o => o.mapY-o.cr-50));



	var container = document.getElementById("starsystemmap-svg");

    container.innerHTML = `<rect width="100%" height="100%" fill="transparent" />`;


    for (let i=0;i<newData.length;i++) {
            let system = newData[i];
            system.cx = Math.round((system.mapX*width/(maxMapX-minMapX))-minMapX+delta,2);
            system.cy = Math.round((system.mapY*height/(maxMapY-minMapY))-minMapY+delta,2);
			//system.cr = Math.round(system.radius/(maxR-minR)*60,2);
    }

    for (let i=0;i<newData.length;i++) {
        let system = newData[i];
        let svg = `<g id="${system.id}-container">`;
        svg += drawSystem(system);
        svg += drawSystemGrid(system);

        svg += "</g>";

        container.innerHTML += svg;

        if(system.jns == undefined) continue;
        for(let j=0;j<system.jns.length;j++)
        {
			container.innerHTML = container.innerHTML + drawConnection(system, newData.find(x => x.id == system.jns[j]));

        }
    }

	for (let i=0;i<newData.length;i++) {
        let system = newData[i];
        container.innerHTML = container.innerHTML + drawSystemName(system);
    }
}

function drawSystem(system)
{
    let strokewidth = 2;
    let gradientWidth = 3.6 + system.radius * 0.016;

    var gradientDef = `
        <radialGradient id="ringGradient${ringGradient}" >
            <stop offset="${Math.round((system.cr - gradientWidth)*100 / system.cr, 2)}%" stop-color="rgba(0, 100, 0, 0.3)" />
            <stop offset="100%" stop-color="rgba(0, 80, 0, 1)" />
        </radialGradient>`;

    if(system.color != undefined)
    {
        var gradientDef = `
        <radialGradient id="ringGradient${ringGradient}" >
            <stop offset="${Math.round((system.cr - gradientWidth)*100 / system.cr, 2)}%" stop-color="rgba(${system.color}, 0.3)" />
            <stop offset="100%" stop-color="rgba(${system.color}, 1)" />
        </radialGradient>`;
    }

    defContainer.innerHTML += gradientDef;

    let color = system.color != undefined ? system.color : "0, 68, 65";

    let innerGradientDef = `<radialGradient id="${system.id}innerGradient" >
		<stop offset="5%" stop-color="rgba(${color}, 1)" />
		<stop offset="45%" stop-color="rgba(${color}, 0.6)" />
		<stop offset="80%" stop-color="rgba(${color}, 0.4)" />
		<stop offset="100%" stop-color="rgba(${color}, 0.3)" />
	  </radialGradient>`;

      defContainer.innerHTML += innerGradientDef;

	var svg = `<circle class="systemRing" cx="${system.cx}" cy="${system.cy}" r="${system.cr+2}" fill="url(#ringGradient${ringGradient})" onmouseover="evt.target.setAttribute('r', '${system.cr +4}');" onmouseout="evt.target.setAttribute('r', '${system.cr +2}');"/>`;
    svg += `<circle class="" cx="${system.cx}" cy="${system.cy}" r="${system.cr - strokewidth}" fill="black"/>`;
    if(system.color != undefined) svg += `<circle class="" cx="${system.cx}" cy="${system.cy}" r="${Math.max(system.cr*0.8, system.cr-3)}" fill="url('#${system.id}innerGradient')"/>`;

    else svg += `<circle class="" cx="${system.cx}" cy="${system.cy}" r="${Math.max(system.cr*0.8, system.cr-3)}" fill="url('#defaultRingGradient')"/>`;

    ringGradient++;


    if(system.ships)
    {
      let image = `<image x="${system.cx + system.cr - 10}" y="${system.cy - system.cr - 10}" width="15" height="auto" xlink:href="./icon_schiff.gif" />`;
      svg += image;
    }
    if(system.group != undefined)
    {
        let image = `<image x="${system.cx - system.cr - 10}" y="${system.cy + system.cr - 10}" width="15" height="auto" xlink:href="./${system.group}" />`;
        svg += image;
    }

    return svg;
}

function drawConnection(systemOne, systemTwo)
{
	if(systemOne.id > systemTwo.id) return "";

    var dirX = systemTwo.cx - systemOne.cx;
    var dirY = systemTwo.cy - systemOne.cy;
	var minDist = 7;

    var length = Math.sqrt((dirX*dirX) + (dirY*dirY));

    dirX = dirX / length;
    dirY = dirY / length;

    let color = "rgb(50, 50, 50)";
    if(systemOne.color == systemTwo.color) color = "rgb(" + systemOne.color + ")";


    return `<line style="stroke:${color}" class="systemConnection" x1="${Math.round(systemOne.cx + (dirX * (systemOne.cr+minDist)), 2)}" y1="${Math.round(systemOne.cy + (dirY * (systemOne.cr+minDist)), 2)}" x2="${Math.round(systemTwo.cx - dirX * (systemTwo.cr+minDist), 2)}" y2="${Math.round(systemTwo.cy - dirY * (systemTwo.cr+minDist), 2)}" />`;

}

function drawSystemName(system)
{
	let y = system.cy;

	if(system.cr < 400) y += system.cr + 15;

	return `<text x="${system.cx}" y="${y}" class="system-text" fill="lightblue" text-anchor="middle" font-size="0.8em">${system.name}</text>`;
}

function drawSystemGrid(system)
{
    let r = system.cr - 5;


    var deltaX1 = Math.cos(30*180/Math.PI) * Math.cos(0) * r;
    var deltaX2 = Math.cos(60*180/Math.PI) * Math.cos(0) * r;
    var deltaX3 = Math.cos(90*180/Math.PI) * Math.cos(0) * r;

    var svg = "";//`<path d="M ${system.cx} ${system.cy - system.cr} C ${system.cx} ${system.cy}, ${system.cx} ${system.cy}, ${system.cx} ${system.cy + system.cr}" class="spherelines" fill="transparent"/>`;
    svg += `<path d="M ${system.cx} ${system.cy - r} A ${r} ${r} 0 0 0 ${system.cx} ${system.cy + r}" class="spherelines" />`;
    svg += `<path d="M ${system.cx} ${system.cy - r} A ${deltaX2} ${r} 0 0 0 ${system.cx} ${system.cy + r}" class="spherelines" />`;
    svg += `<path d="M ${system.cx} ${system.cy - r} A ${deltaX3} ${r} 0 0 0 ${system.cx} ${system.cy + r}" class="spherelines" />`;

    svg += `<path d="M ${system.cx} ${system.cy - r} A ${r} ${r} 0 0 1 ${system.cx} ${system.cy + r}" class="spherelines" />`;
    svg += `<path d="M ${system.cx} ${system.cy - r} A ${deltaX2} ${r} 0 0 1 ${system.cx} ${system.cy + r}" class="spherelines" />`;
    svg += `<path d="M ${system.cx} ${system.cy - r} A ${deltaX3} ${r} 0 0 1 ${system.cx} ${system.cy + r}" class="spherelines" />`;

    svg += `<line x1="${system.cx - r*Math.cos(60*180/Math.PI)}" y1="${system.cy + r*Math.sin(60*180/Math.PI)}" x2="${system.cx + r*Math.cos(60*180/Math.PI)}" y2="${system.cy + r*Math.sin(60*180/Math.PI)}" class="spherelines" />`;
    svg += `<line x1="${system.cx - r*Math.cos(30*180/Math.PI)}" y1="${system.cy + r*Math.sin(30*180/Math.PI)}" x2="${system.cx + r*Math.cos(30*180/Math.PI)}" y2="${system.cy + r*Math.sin(30*180/Math.PI)}" class="spherelines" />`;
    svg += `<line x1="${system.cx - r}" y1="${system.cy}" x2="${system.cx + r}" y2="${system.cy}" class="spherelines" />`;
    svg += `<line x1="${system.cx - r*Math.cos(30*180/Math.PI)}" y1="${system.cy - r*Math.sin(30*180/Math.PI)}" x2="${system.cx + r*Math.cos(30*180/Math.PI)}" y2="${system.cy - r*Math.sin(30*180/Math.PI)}" class="spherelines" />`;
    svg += `<line x1="${system.cx - r*Math.cos(60*180/Math.PI)}" y1="${system.cy - r*Math.sin(60*180/Math.PI)}" x2="${system.cx + r*Math.cos(60*180/Math.PI)}" y2="${system.cy - r*Math.sin(60*180/Math.PI)}" class="spherelines" />`;

    //A 30 50 0 0 1 162.55 162.45

    //svg += `<path d="M ${system.cx} ${system.cy - system.cr} C ${system.cx + system.cr*0.5} ${system.cy - system.cr*0.5}, ${system.cx + system.cr*0.5} ${system.cy + system.cr*0.5}, ${system.cx} ${system.cy + system.cr}" stroke="white" fill="transparent"/>`;
    //svg += `<path d="M ${system.cx} ${system.cy - system.cr} C ${system.cx + system.cr*1.2} ${system.cy - system.cr*0.65}, ${system.cx + system.cr*1.2} ${system.cy + system.cr*0.65}, ${system.cx} ${system.cy + system.cr}" stroke="white" fill="transparent"/>`;

    //svg += `<path d="M ${system.cx} ${system.cy - system.cr} C ${system.cx - system.cr*0.5} ${system.cy - system.cr*0.5}, ${system.cx - system.cr*0.5} ${system.cy + system.cr*0.5}, ${system.cx} ${system.cy + system.cr}" stroke="white" fill="transparent"/>`;

    return svg;
}