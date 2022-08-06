var Starmap = function(){

    //maxX = 25*400;
    //maxY = 25*400;

    document.body.addEventListener("mousedown", function (e) {
        if (e.target &&
            e.target.classList.contains("dragme")) {
            console.log("test");
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
        targ.style.left = Math.min(25,coordX + e.clientX - offsetX) + 'px';
        targ.style.top = Math.min(25,coordY + e.clientY - offsetY) + 'px';

        var legendTargetsX = document.querySelectorAll(".scroll-x");
        var legendTargetsY = document.querySelectorAll(".scroll-y");

        legendTargetsX[0].style.left=targ.style.left;
        legendTargetsX[1].style.left=targ.style.left;

        legendTargetsY[0].style.top=targ.style.top;
        legendTargetsY[1].style.top=targ.style.top;

        return false;
    }

    function stopDrag() {
        drag = false;
    }
    window.onload = function () {
        document.onmousedown = startDrag;
        document.onmouseup = stopDrag;
    }

};