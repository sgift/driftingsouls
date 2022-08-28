var Starmap = function(){

    //maxX = 25*400;
    //maxY = 25*400;
    var system;
    var target = document.getElementById("draggable");

    var starmapCanvas = new StarmapCanvas(this);

    var scanableLocations = [];
    var scanships = {};
    var currentShip;
    var flight;

    function setSystem(newSystem)
    {
        system = newSystem;
        scanships = {};

    }

    function getSystem()
    {
        return system;
    }

    function registerLocation(location)
    {
        scanableLocations.push(location);
    }
    function registerScanship(scanship)
    {
        scanships[scanship.shipId] = {x:scanship.location.x, y:scanship.location.y, r:scanship.scanRange};
    }

    function executeActionAtLocation(location)
    {
        if(currentShip == null || currentShip.id == -1)
        {
            var locations = scanableLocations.filter(x => x.x == location.x && x.y == location.y);
            var location = locations[0];
            if(locations.length > 0)
            {
                loadSectorData(location.x, location.y, location.shipId);
            }
        }
        else
        {
            flight = {shipId: currentShip.id, location:location};
            starmapCanvas.renderFlightConfirmation(location, currentShip);
        }
    }

    function setCurrentShip(ship)
    {
        currentShip = ship;
        if(ship != null) starmapCanvas.renderShipChosen(ship);
    }
    function abortFlight()
    {
        setCurrentShip(null);
        starmapCanvas.derenderMapAction();
    }
    function confirmFlight()
    {
        //http://localhost:8080/ds/ds?FORMAT=JSON&module=schiffAjax&action=fliegeSchiff&schiff=1857304&x=31&y=33
        jQuery.getJSON(DS.getUrl(),{FORMAT:'JSON', module:'schiffAjax', action:'fliegeSchiff', schiff:flight.shipId, x:flight.location.x, y:flight.location.y})
        .done(function(resp){ReloadSystem();RenderLog(resp);});
        //jQuery.getJSON(DS.getUrl(),{FORMAT:'JSON', module:'schiffAjax', action:'fliegeSchiff', schiff:flight.shipId, x:flight.location.x, y:flight.location.y}, function(resp){ReloadSystem();});
        setCurrentShip(null);
        starmapCanvas.derenderMapAction();
    }

    function getCurrentShip()
    {
        return currentShip;
    }

    function getScanships()
    {
        return scanships;
    }

    this.setSystem = setSystem;
    this.getSystem = getSystem;

    this.setCoordinates = starmapCanvas.setCoordinates;

    this.setMarkerToCoordinates = starmapCanvas.setMarkerToCoordinates;
    this.getLocationFromPixels = starmapCanvas.getLocationFromPixels;
    this.elementWidth = starmapCanvas.elementWidth;
    this.elementHeight = starmapCanvas.elementHeight;
    this.executeActionAtLocation = executeActionAtLocation;
    this.setCurrentShip = setCurrentShip;
    this.registerLocation = registerLocation;
    this.registerScanship = registerScanship;
    this.getScanships = getScanships;
    this.getCurrentShip = getCurrentShip;
    this.abortFlight = abortFlight;
    this.confirmFlight = confirmFlight;
};

var StarmapCanvas = function(starmap)
{
    var _starmap = starmap;
    var fieldSize = 25;

    var starmapElement;

    var target;
    var lastX=0;
    var lastY=0;
    var legendTargetsX;
    var legendTargetsY;

    var dimensions;
    var mouseDownPosition = {x:0, y:0};
    var isMouseClick = true;

    init();

    function init()
    {
        target = document.getElementById("draggable");
        document.querySelector("#starmap-mouse-event-target").addEventListener("click", (e) => onclick(e));
        starmapElement = document.getElementById("starmap");
        getElementDimensions();

        addEventListener('resize', getElementDimensions);

        legendTargetsX = document.querySelectorAll(".scroll-x");
        legendTargetsY = document.querySelectorAll(".scroll-y");
    }


    function getElementDimensions()
    {
        dimensions = {x:parseInt(getComputedStyle(starmapElement).width) - 2*fieldSize, y:parseInt(getComputedStyle(starmapElement).height) - 2*fieldSize};
    }

    function elementWidth()
    {
        return dimensions.x;
    }

    function elementHeight()
    {
        return dimensions.y;
    }

    function maxX()
    {
        var width = _starmap.getSystem().width*fieldSize;
        return Math.max(-fieldSize, width-elementWidth());
    }
    function maxY()
    {
        var height = _starmap.getSystem().height*fieldSize;
        return Math.max(-fieldSize, height-elementHeight());
    }

    document.body.addEventListener("mousedown", function (e) {
        if (e.target &&
            e.target.classList.contains("dragme")) {
            startDrag(e);
        }
    });

    document.body.addEventListener("touchstart", function (e) {
            if (e.target &&
                e.target.classList.contains("dragme")) {
                startDrag(e);
            }
        });

    document.onmouseup = stopDrag;
    document.addEventListener("touchend", stopDrag);


    function startDrag(e) {
        // determine event object
        if (!e) {
            var e = window.event;
        }

        // IE uses srcElement, others use target

        if (target.className != 'dragme') {
            return
        };
        // calculate event X, Y coordinates
        offsetX = e.clientX;
        offsetY = e.clientY;

        mouseDownPosition.x = lastX;
        mouseDownPosition.y = lastY;
        isMouseClick = true;

        coordX = lastX;
        coordY = lastY;
        drag = true;

        // move div element
        onmousemove = document.onmousemove;
        ontouchmove = document.ontouchmove
        document.onmousemove = dragDiv;
        document.addEventListener("touchmove", dragDiv);
    }
    var onmousemove;
    var ontouchmove;

    function dragDiv(e) {
        if (!drag) {
            return
        };
        if (!e) {
            var e = window.event
        };

        // move div element

        var newX = coordX + e.clientX - offsetX
        var newY = coordY + e.clientY - offsetY;

        if(Math.sqrt(Math.pow(mouseDownPosition.x-newX, 2) + Math.pow(newY-mouseDownPosition.y, 2)) > 5) isMouseClick = false;

        setPosition(newX, newY);

        return false;
    }


    function setPosition(newX, newY)
    {
        newX = Math.min(0, Math.max(newX, -maxX()));
        newY = Math.min(0, Math.max(newY, -maxY()));

        lastX = newX;
        lastY = newY;

        target.style.transform = "translate("+ newX + "px, " + newY + "px)";
        legendTargetsX[0].style.transform = "translate(" + newX + "px, 0px)";
        legendTargetsX[1].style.transform = "translate(" + newX + "px, 0px)";
        legendTargetsY[0].style.transform = "translate(0px, " + newY + "px)";
        legendTargetsY[1].style.transform = "translate(0px, " + newY + "px)";
    }

    function getPixelByCoordinates(x)
    {
        return (x-1)*fieldSize;
    }

    function setCoordinates(x, y)
    {
        setPosition(-getPixelByCoordinates(x) + elementWidth()/2, -getPixelByCoordinates(y) + elementHeight() / 2);
        setMarkerToCoordinates(x, y);
    }

    var marker;
    function setMarkerToCoordinates(x, y)
    {
        if(marker == null) marker = document.getElementById("position-marker");
        marker.style.display = "block";
        marker.style.left = getPixelByCoordinates(x) + 'px';
        marker.style.top = getPixelByCoordinates(y) + 'px';
    }

    function onclick(event)
    {
        if(!isMouseClick) return;
        var y = (event.offsetY - parseInt(target.style.top));
        var x = (event.offsetX - parseInt(target.style.left));

        var location = getLocationFromPixels(x, y);
        setMarkerToCoordinates(location.x, location.y);

        _starmap.executeActionAtLocation({x:location.x, y:location.y});
    }

    function getLocationFromPixels(x, y)
    {
        return {x: Math.floor((x-lastX)/fieldSize)+1, y: Math.floor((y-lastY)/fieldSize)+1};
    }

    function stopDrag() {
        drag = false;
        document.onmousemove = onmousemove;
        document.removeEventListener("touchmove", dragDiv);
    }

    function getCurrentViewRectangle()
    {
        var left = Math.floor(lastX/fieldSize);
        var top = Math.floor(lastY/fieldSize);
        var viewRectangle = {x:-left-2, y:-top-2, w:elementWidth()/fieldSize+4, h:elementHeight()/fieldSize+4}
        return viewRectangle;
    }

    function renderFlightConfirmation(location, ship)
    {
        var mapAction = document.getElementById("kartenaktion");
        mapAction.querySelector(".bestaetigung").style.visibility = "visible";
        //document.getElementById("kartenaktion").style.display = "block";
        var flightConfirmationText = mapAction.querySelector("#flightConfirmationText");
        var numberOfFields = Math.max(Math.abs(location.x-ship.x), Math.abs(location.y-ship.y))
        flightConfirmationText.textContent = `Soll das Schiff ${ship.name} wirklich nach ${location.x}/${location.y} (${numberOfFields} ${numberOfFields == 1 ? "Feld" : "Felder"}) fliegen?`;
        marker.style.borderColor = "#feb626";
    }

    function derenderMapAction()
    {
        document.getElementById("kartenaktion").style.display = "none";
        marker.style.borderColor = "blue"
    }

    function renderShipChosen(ship)
    {
        $("#starmapSectorPopup").dialog("close");
        var mapAction = document.getElementById("kartenaktion");
        mapAction.querySelector(".bestaetigung").style.visibility = "hidden";

        var flightConfirmationText = mapAction.querySelector("#flightConfirmationText");
        flightConfirmationText.textContent = `Bitte wähle die Zielposition für das Schiff ${ship.name} aus...`;

        mapAction.style.display = "block";
    }

    this.setCoordinates = setCoordinates;
    //this.onclick = onclick;
    this.setMarkerToCoordinates = setMarkerToCoordinates;
    this.getLocationFromPixels = getLocationFromPixels;
    this.elementWidth = elementWidth;
    this.elementHeight = elementHeight;
    this.getCurrentViewRectangle = getCurrentViewRectangle;
    this.renderFlightConfirmation = renderFlightConfirmation;
    this.derenderMapAction = derenderMapAction;
    this.renderShipChosen  = renderShipChosen;
}