var Starmap = function(){

    //maxX = 25*400;
    //maxY = 25*400;
    var system;
    var target;

    init();

    function elementWidth()
    {
        return parseInt(getComputedStyle(document.getElementById("starmap")).width) -25;
    }

    function elementHeight()
    {
        return parseInt(getComputedStyle(document.getElementById("starmap")).height) -25;
    }

    function maxX()
    {
        var width = system.width*25;
        return Math.max(-25, width-elementWidth());
    }
    function maxY()
    {
        var height = system.height*25;
        return Math.max(-25, height-elementHeight());
    }

    function init()
    {
        console.log("starmap init");
        target = document.getElementById("draggable");
        console.log(target);
        document.querySelector("#starmap-mouse-event-target").addEventListener("click", (e) => onclick(e));
    }

    document.body.addEventListener("mousedown", function (e) {
        if (e.target &&
            e.target.classList.contains("dragme")) {
            startDrag(e);
            // handle event here
        }
    });

    document.body.addEventListener("mouseup", function (e) {
        if (e.target &&
            e.target.classList.contains("dragme")) {
            stopDrag();
            // handle event here
        }
    });

    function startDrag(e) {
        // determine event object
        if (!e) {
            var e = window.event;
        }

        // IE uses srcElement, others use target
        var targ = document.getElementById("draggable");

        if (targ.className != 'dragme') {
            return
        };
        // calculate event X, Y coordinates
        offsetX = e.clientX;
        offsetY = e.clientY;

        // assign default values for top and left properties
        if (!targ.style.left) {
            targ.style.left = '25px'
        };
        if (!targ.style.top) {
            targ.style.top = '25px'
        };

        // calculate integer values for top and left
        // properties
        coordX = parseInt(targ.style.left);
        coordY = parseInt(targ.style.top);
        drag = true;

        // move div element
        document.onmousemove = dragDiv;
    }

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

        setPosition(newX, newY);
        return false;
    }

    function setPosition(newX, newY)
    {
        var targ = document.getElementById("draggable");
        targ.style.left = Math.min(25, Math.max(newX, -maxX())) + 'px';
        targ.style.top = Math.min(25, Math.max(newY, -maxY())) + 'px';

        var legendTargetsX = document.querySelectorAll(".scroll-x");
        var legendTargetsY = document.querySelectorAll(".scroll-y");

        legendTargetsX[0].style.left = parseInt(targ.style.left)-25 + 'px';
        legendTargetsX[1].style.left = parseInt(targ.style.left)-25 + 'px';

        legendTargetsY[0].style.top = parseInt(targ.style.top)-25 + 'px';
        legendTargetsY[1].style.top = parseInt(targ.style.top)-25 + 'px';
    }

    function getPixelByCoordinates(x)
    {
        return (x-1)*25;
    }

    function setCoordinates(x, y)
    {
        setPosition(-getPixelByCoordinates(x) + elementWidth()/2, -getPixelByCoordinates(y) + elementHeight() / 2);
        setMarkerToCoordinates(x, y);
    }

    function setMarkerToCoordinates(x, y)
    {
        var marker = document.getElementById("position-marker");
        marker.style.display = "block";
        marker.style.left = getPixelByCoordinates(x) + 'px';
        marker.style.top = getPixelByCoordinates(y) + 'px';
    }

    function onclick(event)
    {
        var y = (event.offsetY - parseInt(document.querySelector("#draggable").style.top));
        var x = (event.offsetX - parseInt(document.querySelector("#draggable").style.left));

        var location = getLocationFromPixels(x, y);
        setMarkerToCoordinates(location.x, location.y);
    }

    function getLocationFromPixels(x, y)
    {
        return {x: Math.floor(x/25)+1, y: Math.floor(y/25)+1};
    }

    function stopDrag() {
        drag = false;
    }

    function setSystem(newSystem)
    {
        system = newSystem;
    }

    window.onload = function () {
        document.onmousedown = startDrag;
        document.onmouseup = stopDrag;
    }
    function getSystem()
    {
        return system;
    }


    this.setSystem = setSystem;
    this.setCoordinates = setCoordinates;
    this.onclick = onclick;
    this.setMarkerToCoordinates = setMarkerToCoordinates;
    this.getLocationFromPixels = getLocationFromPixels;
    this.getSystem = getSystem;
};