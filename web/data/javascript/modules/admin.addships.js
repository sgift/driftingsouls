var Admin_AddShips = {
	shipSelectChange:function( newval ) {
		if( typeof shipdata === "undefined" ) {
			return;
		}
		if( shipdata[newval][0] > 0 ) {
			document.getElementById("tbl_jaeger").style.visibility = "visible";
			document.getElementById("tbl_jaeger_lowid").style.visibility = "visible";
		}
		else {
			document.getElementById("tbl_jaeger").style.visibility = "hidden";
			document.getElementById("tbl_jaeger_lowid").style.visibility = "hidden";
		}

		for( i=0; i < ammodata.length; i++ ) {
			var element = document.getElementById("select_ammo_"+ammodata[i]);
			element.style.visibility = "hidden";
			element.style.top = "0px";
		}

		if( shipdata[newval][1].length > 0 ) {
			height = 0;
			document.getElementById("tbl_ammo").style.visibility = "visible";
			for( i=0; i < shipdata[newval][1].length; i++ ) {
				element = document.getElementById("select_ammo_"+shipdata[newval][1][i]);
				element.style.visibility = "visible";
				element.style.top = height+"px";
				height += 25;
			}
			document.getElementById("tbl_ammo_div").style.height = height+"px";
		}
		else {
			document.getElementById("tbl_ammo").style.visibility = "hidden";
		}
	},

	jaegerSelectChange : function( newval ) {
		for( i=0; i < ammodata.length; i++ ) {
			var element = document.getElementById("select_jaeger_ammo_"+ammodata[i]);
			element.style.visibility = "hidden";
			element.style.top = "0px";
		}

		if( shipdata[newval][1].length > 0 ) {
			height = 0;
			document.getElementById("tbl_jaeger_ammo").style.visibility = "visible";
			for( i=0; i < shipdata[newval][1].length; i++ ) {
				element = document.getElementById("select_jaeger_ammo_"+shipdata[newval][1][i]);
				element.style.visibility = "visible";
				element.style.top = height+"px";
				height += 25;
			}
			document.getElementById("tbl_jaeger_ammo_div").style.height = height+"px";
		}
		else {
			document.getElementById("tbl_jaeger_ammo").style.visibility = "hidden";
		}
	}
};