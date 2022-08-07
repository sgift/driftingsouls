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
        return Math.max(0, width-elementWidth());
    }
    function maxY()
    {
        var height = system.height*25;
        return Math.max(0, height-elementHeight());
    }

    function init()
    {
        target = document.getElementById("draggable");

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
        var targ = document.getElementById("draggable");
        // move div element

        var newX = coordX + e.clientX - offsetX
        var newY = coordY + e.clientY - offsetY;

        console.log(newX);
        console.log(newY); // -260

        targ.style.left = Math.min(25, Math.max(newX, -maxX())) + 'px';
        targ.style.top = Math.min(25, Math.max(newY, -maxY())) + 'px';

        var legendTargetsX = document.querySelectorAll(".scroll-x");
        var legendTargetsY = document.querySelectorAll(".scroll-y");

        legendTargetsX[0].style.left = parseInt(targ.style.left)-25 + 'px';
        legendTargetsX[1].style.left = parseInt(targ.style.left)-25 + 'px';

        legendTargetsY[0].style.top = parseInt(targ.style.top)-25 + 'px';
        legendTargetsY[1].style.top = parseInt(targ.style.top)-25 + 'px';

        return false;
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

    this.setSystem = setSystem;
};