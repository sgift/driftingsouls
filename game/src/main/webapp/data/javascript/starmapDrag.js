var Starmap = function(){

    //maxX = 25*400;
    //maxY = 25*400;
    var system;
    var target;
    var scanships = {};
    var fieldSize = 25;

    init();

    function elementWidth()
    {
        return parseInt(getComputedStyle(document.getElementById("starmap")).width) - fieldSize;
    }

    function elementHeight()
    {
        return parseInt(getComputedStyle(document.getElementById("starmap")).height) - fieldSize;
    }

    function maxX()
    {
        var width = system.width*fieldSize;
        return Math.max(-fieldSize, width-elementWidth());
    }
    function maxY()
    {
        var height = system.height*fieldSize;
        return Math.max(-fieldSize, height-elementHeight());
    }

    function registerScanship(scanship)
    {
        //var node = document.getElementById("scanfield-" + scanship.shipId);
        var scancircle = {x:scanship.location.x, y:scanship.location.y, r:scanship.scanRange+1, id:scanship.shipId };
        scanships[scanship.shipId] = scancircle;

    }

    function getCurrentViewRectangle()
    {
        var targ = document.getElementById("draggable");
        var left = Math.floor(parseInt(targ.style.left)/fieldSize);
        var top = Math.floor(parseInt(targ.style.top)/fieldSize);

        var viewRectangle = {x:left, y:-top, w:elementWidth()/fieldSize, h:elementHeight()/fieldSize}
        return viewRectangle;
    }

    var stopUnHiding = false;
    function unHidingOnMove()
    {
        var viewRectangle = getCurrentViewRectangle();

        for(const [key, value] of Object.entries(scanships))
        {
            function myfunction() {
                 if(stopUnHiding == true)
                    stopUnHiding = false;
                    return;
                }
            var isVisible = RectCircleColliding(value, viewRectangle);
            if(value.node == undefined)
            {
                value.node = document.getElementById("scanfield-" + key);
                if(value.node == null) continue;
            }

            if(!isVisible)
            {
                if(value.node.style.display != "none")
                {
                    value.node.style.display = "none";
                }
            }
            else
            {
                if(value.node.style.display == "none")
                {
                    value.node.style.display = "block";
                }
            }
        }
    }

    function RectCircleColliding(circle,rect){
        var distX = Math.abs(circle.x - rect.x-rect.w/2);
        var distY = Math.abs(circle.y - rect.y-rect.h/2);

        if (distX > (rect.w/2 + circle.r)) { return false; }
        if (distY > (rect.h/2 + circle.r)) { return false; }

        if (distX <= (rect.w/2)) { return true; }
        if (distY <= (rect.h/2)) { return true; }

        var dx=distX-rect.w/2;
        var dy=distY-rect.h/2;
        return (dx*dx+dy*dy<=(circle.r*circle.r));
    }

    function init()
    {
        //console.log("starmap init");
        target = document.getElementById("draggable");
        //console.log(target);
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

        stopUnHiding = true;

        // move div element

        var newX = coordX + e.clientX - offsetX
        var newY = coordY + e.clientY - offsetY;

        setPosition(newX, newY);
        return false;
    }

    function setPosition(newX, newY)
    {
        var targ = document.getElementById("draggable");
        targ.style.left = Math.min(fieldSize, Math.max(newX, -maxX())) + 'px';
        targ.style.top = Math.min(fieldSize, Math.max(newY, -maxY())) + 'px';

        var legendTargetsX = document.querySelectorAll(".scroll-x");
        var legendTargetsY = document.querySelectorAll(".scroll-y");

        legendTargetsX[0].style.left = parseInt(targ.style.left)-fieldSize + 'px';
        legendTargetsX[1].style.left = parseInt(targ.style.left)-fieldSize + 'px';

        legendTargetsY[0].style.top = parseInt(targ.style.top)-fieldSize + 'px';
        legendTargetsY[1].style.top = parseInt(targ.style.top)-fieldSize + 'px';

        stopUnHiding = false;
        unHidingOnMove();
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
        return {x: Math.floor(x/fieldSize)+1, y: Math.floor(y/fieldSize)+1};
    }

    async function stopDrag() {
        stopUnHiding = true;
        drag = false;
        for(const i=0; i<5;i++)
        {
            if(stopUnHiding == true)
            {
                await new Promise(r => setTimeout(r, 50));
            }
            else
            {
                unHidingOnMove();
                return;
            }
        }

        unHidingOnMove();
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
        scanships = {};
    }


    this.setSystem = setSystem;
    this.setCoordinates = setCoordinates;
    this.onclick = onclick;
    this.setMarkerToCoordinates = setMarkerToCoordinates;
    this.getLocationFromPixels = getLocationFromPixels;
    this.getSystem = getSystem;
    this.registerScanship = registerScanship;
};