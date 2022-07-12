

function BaseRenderer(){

	function RenderBuildingActions(data)
	{
		var container = document.querySelector(".buildingActions");
		var toBeRemoved = document.querySelectorAll(".buildingActions li.buildingAction");

		for(let i=0; i < toBeRemoved.length; i++)
		{
			toBeRemoved[i].remove();
		}

		for(let i=0; i < data.buildingActions.length; i++)
		{
			var parsedata = parseHTML(templatebuildingActions(data.buildingActions[i],data.id));
			parsedata.addEventListener("mouseout", function(){
				Base.noBuildingHighlight();
			});
			parsedata.addEventListener("mouseover", function(){
				Base.highlightBuilding('building'+data.buidingActions[i].buildingTypeId);
			})
			container.prepend(parsedata.firstElementChild);
			container.prepend(parsedata.firstChild);
		}
	}

	function RenderCargo(data){
		document.getElementById("Waren").querySelectorAll("tbody")[0].innerHTML="";
		document.getElementById("Munition").querySelectorAll("tbody")[0].innerHTML="";
		document.getElementById("Module").querySelectorAll("tbody")[0].innerHTML="";
		document.getElementById("Sonstiges").querySelectorAll("tbody")[0].innerHTML="";
		document.getElementById("cargo_uebersicht").innerHTML ='Verf\u00fcgbarer Cargo: '+ data.empty_cargo.empty.toLocaleString() + ' ('+data.empty_cargo.change.toLocaleString()+') / '+data.empty_cargo.max.toLocaleString();
		data.url = DS.getUrl();

		for(let i=0; i< data.cargo.length; i++)
		{
			data.cargo[i].url= data.url;
			let test = parseHTML(templateCargoFn(data.cargo[i]));
			document.getElementById(data.cargo[i].kategorie).querySelectorAll("tbody")[0].appendChild(parseHTML(templateCargoFn(data.cargo[i])).querySelector(".myTemplateIdentifier"));
		}
	}

	function RenderEnergy(data){
		document.getElementById("stored_energy").innerHTML = data.energy.gespeicherte_energie.toLocaleString();
		document.getElementById("energydiff").innerHTML = data.energy.energiebilanz.toLocaleString();
	}

	function RenderStats(data){
		//hier noch unschoen, dass die title sowohl hier als auch in der base.html stehen... sollte man, wenn man Musse hat, mal zentral, zB in den Controller auslagern
		const templateStatsFn = stats =>
			`<div id="statsBox" style="width:100%;">
				${RenderSingleStat("arbeiteranzeige", stats.arbeiter, 'Anteil arbeitender Bev&ouml;lkerung')}
				${RenderSingleStat("arbeitslosenanzeige", stats.arbeitslos, 'Anteil arbeitsloser Bev&ouml;lkerung')}
				${RenderSingleStat("wohnraumfreianzeige", stats.wohnraumfrei, 'Freier Wohnraum f&uuml;r neue Bev&ouml;lkerung. Bev&ouml;lkerung wandert zu.')}
				${RenderSingleStat("wohnraumfehltanzeige", stats.wohnraumfehlt, 'Fehlender Wohnraum f&uuml;r aktuelle Bev&ouml;lkerung. Bev&ouml;lkerung wandert ab!')}
			</div>`;
		let stats = document.getElementById("statsBox");

		stats.innerHTML = parseHTML(templateStatsFn(data.stats)).firstChild.innerHTML;
		document.getElementById("bevoelkerung").innerHTML = data.stats.einwohner.toLocaleString();
		document.getElementById("arbeiter").innerHTML = data.stats.arbeiter.toLocaleString();
		if(data.stats.arbeiteraenderung)
		{
			document.getElementById("arbeiter").classList.add("fadein");
		}
		document.getElementById("wohnraum").innerHTML = data.stats.wohnraum.toLocaleString();
		if(data.stats.wohnraumaenderung)
		{
			document.getElementById("wohnraum").classList.add("fadein");
		}
	}

	function RenderSingleStat(cssClass, amount, title)	{

		let result = "<div class=\"row\" style=\"margin-left:5px;margin-right:5px; min-height: 15px;\" title=\""+title+"\">";
		if(amount>0) {
			for (let i = 0; i < Math.floor(amount / 1000); i++) {
				result = result + '<div class="' + cssClass + ' arbeiteranzeige-voll" ></div>';
			}
		}
		if(amount % 1000 != 0)
		{
			result = result + '<div class="' + cssClass + '" style="width:' + ((amount % 1000) * 0.08) + 'px"></div>';
		}
		result = result + "</div>";
		return result;
	}

	function RenderBaulisteRessMangel(buildings){
		let allGebRess = document.querySelectorAll("button.btn-geb-info span [ds-item-id]");

		for(let index = 0; index < allGebRess.length; ++index)
		{
			let ressElement = allGebRess[index];
			if(ressElement.parentElement.classList.contains("negativ"))
			{
				ressElement.parentElement.classList.remove("negativ");
				ressElement.parentElement.classList.add("positiv");
			}
		}

		for(let index = 0; index < buildings.length; ++index)
		{
			let geb = buildings[index];
			let gebNode = document.querySelectorAll("button.geb" + geb.geb_id);

			if(gebNode != undefined && gebNode.length > 0)
			{
				for(let j=0; j < geb.mangel.length; j++)
				{
					gebNode[0].querySelector("[ds-item-id='i" + geb.mangel[j].ress_id + "|0|0']").parentElement.classList.remove("positiv");
					gebNode[0].querySelector("[ds-item-id='i" + geb.mangel[j].ress_id + "|0|0']").parentElement.classList.add("negativ");
				}
			}
		}
	}

	function ReplaceBuilding(data){
		data.url = DS.getUrl();

		let replace;
		if(data.geb_id == -1){
			replace = parseHTML(templateEmptyBuildingSpaceFn(data));
			let oldBuilding = document.querySelector("div.p"+data.field).closest(".tile");
			oldBuilding.innerHTML = replace.firstChild.innerHTML;

			oldBuilding.querySelector("div.bebaubar").addEventListener('click', function(e) {
				Base.BaueFeld(oldBuilding.querySelector("div.bebaubar").parentNode, data.field);
				e.preventDefault();
				console.log(data.field);
				return false;
			}, false);

		}
		else{
			replace = parseHTML(templateBuildingFn(data));
			let oldBuilding = document.querySelector("div.p"+data.field).closest(".tile");
			oldBuilding.innerHTML = replace.firstChild.innerHTML;

			oldBuilding.querySelector("a").addEventListener('click', function(e) {
				Base.showBuilding(data.field);
				e.preventDefault();
				console.log(data.field);
				return false;
			}, false);

		}
		setTimeout(function() {
			defade();
		}, 2000);
	}


	function RenderAllButBuildings(data){
		RenderCargo(data);
		RenderStats(data);
		RenderEnergy(data);
		RenderBaulisteRessMangel(data.buildings);
		RenderBuildingActions(data);
		DsTooltip.update($("body"));
	}

	function RenderNoSuccessBuildBuilding(json){
		RenderContentInBuildingBox(json.message);
	}

	function RenderContentInBuildingBox(rendercontent) {
		if( $('#buildingBox').size() == 0 ) {
			$('body').append('<div id="buildingBox" />');
			$('#buildingBox').dsBox({
				draggable:true,
				center:true
			});
		}

		var buildingBox = $('#buildingBox');
		buildingBox.dsBox('show');
		var content = buildingBox.find('.content');

		content.empty();
		content.append(rendercontent);
	}

	this.RenderCargo = RenderCargo;
	this.RenderStats = RenderStats;
	this.RenderSingleStat = RenderSingleStat;
	this.RenderBaulisteRessMangel = RenderBaulisteRessMangel;
	this.ReplaceBuilding = ReplaceBuilding;
	this.RenderAllButBuildings = RenderAllButBuildings;
	this.RenderNoSuccessBuildBuilding = RenderNoSuccessBuildBuilding;
	this.RenderContentInBuildingBox = RenderContentInBuildingBox;

}

var Base = {
	renderer: new BaseRenderer(),

	AskField:function(field){
		let url = getUrl();
		jQuery.getJSON(url,{action:'update', col:Base.getBaseId(), field:field},function(response){Base.ResponseVerarbeitung(response)} );
	},

	SelectBuilding: function(element, id){
		if(this.selectedBuilding != id)
		{
			this.deselectBuilding();
			this.selectedBuilding = id;
			element.classList.add("active");
			Base.highlightBuilding('bebaubar');
		}
		else
		{
			this.deselectBuilding();
		}
	},

	deselectBuilding: function(){
		Base.noBuildingHighlight();
		let active = document.getElementsByClassName("active");
		if(active[0] != null){
			active[0].classList.remove("active");
		}
		this.selectedBuilding = -1;
	},

	selectedBuilding: -1,

	ResponseVerarbeitung: function(data)
	{
		if(data.success == true)

		{
			Base.renderer.ReplaceBuilding(data.gebaut);
			Base.renderer.RenderAllButBuildings(data);
		}
		else
		{
			Base.renderer.RenderNoSuccessBuildBuilding(data);
		}
		Base.noBuildingHighlight();
		if(this.selectedBuilding != -1) {
			Base.highlightBuilding('bebaubar');
		}
		DsTooltip.update($("body"));
	},

	UpdateAllButBuildings: function()
	{
		let url = getUrl();
		jQuery.getJSON(url,{action:'update', col:Base.getBaseId()},function(response){Base.renderer.RenderAllButBuildings(response)} );
	},

	BaueFeld: function(tileDiv, id){
		defade();
		if(this.selectedBuilding == -1)
		{
			return;
		}

		let url = getUrl();
		jQuery.getJSON(url,{action:'build', col:Base.getBaseId(), building:Base.selectedBuilding, field:id},function(resp){Base.ResponseVerarbeitung(resp)} );
	},

	showNameInput: function(model) {
		var el = $('#baseName');
		el.empty();

		var cnt = DS.render(
			"<form action='{{URL}}' method='post' style='display:inline'>"+
			"<input name='newname' type='text' size='15' maxlength='50' value='{{basename}}' />"+
			"<input name='col' type='hidden' value='{{baseid}}' />"+
			"<input name='module' type='hidden' value='base' />"+
			"<input name='action' type='hidden' value='changeName' />"+
			"&nbsp;<input type='submit' value='umbenennen' />"+
			"</form>",
			model
		);

		el.append(cnt);

		$('#changename').css('display','none');
	},

	highlightBuilding : function(buildingCls) {
		$('#baseMap').addClass('fade');
		$('#baseMap .'+buildingCls).closest('.tile').addClass('highlight');
	},
	noBuildingHighlight : function() {
		$('#baseMap').removeClass('fade');
		$('#baseMap .tile').removeClass('highlight');
	},
	changeName : function() {
		var baseName = document.getElementById("baseName");
		var baseNameForm = document.getElementById("baseNameForm");

		toggleElement(baseName, "inline");
		toggleElement(baseNameForm, "inline");

	},
	showBuilding : function(tileId) {
		new BuildingUi(this, tileId);
	},

	refreshAll : function() {
		location.reload();
	},
	__parseRefreshAll : function(resp) {
		var response = $(resp);
		var cnt = response.filter('#baseContent');
		$('#baseContent').replaceWith(cnt);
	},

	refreshBase : function() {

		RenderAllButBuildings(data);
	},

	getBaseId : function() {
		return $('#baseId').val();
	}
};

function BuildingUi(base, tileId) {
	var __defaultBuildingHandler = {
		generateOutput : function(resp) {
			var tmpl =
				'<dl class="defaultBuilding">'+
				"<dt>Arbeiter ben&ouml;tigt:</dt>"+
				"<dd>"+
				'{{#worker}}'+
				'<img src="./data/interface/arbeiter.gif" alt="" title="Arbeiter" />{{count}} '+
				'{{/worker}}'+
				"</dd>"+
				'<dt>Verbraucht:</dt>'+
				"<dd>"+
				'{{#consumes}}'+
				'{{#cargo}}'+
				'<img src="{{image}}" alt="" title="{{plainname}}" />{{{cargo1}}} '+
				'{{/cargo}}'+
				'{{#energy}}'+
				'<img src="./data/interface/energie.gif" alt="" title="Energie" />{{count}} '+
				'{{/energy}}'+
				'{{/consumes}}'+
				'{{^consumes}}-{{/consumes}}'+
				"</dd>"+

				"<dt>Produziert:</dt>"+
				"<dd>"+
				'{{#produces}}'+
				'{{#cargo}}'+
				'<img src="{{image}}" alt="" title="{{plainname}}" />{{{cargo1}}} '+
				'{{/cargo}}'+
				'{{#energy}}'+
				'<img src="./data/interface/energie.gif" alt="" title="Energie" />{{count}} '+
				'{{/energy}}'+
				'{{/produces}}'+
				'{{^produces}}-{{/produces}}'+
				"</dd>"+
				'</dl>';

			return DS.render(tmpl, resp.buildingUI);
		}
	};

	function BuildingView() {
		function createViewBox() {
			if( $('#buildingBox').size() == 0 ) {
				$('body').append('<div id="buildingBox" />');
				$('#buildingBox').dsBox({
					draggable:true,
					center:true
				});
			}
		}

		function renderEmpty() {
			if( $('#buildingBox').size() == 0 ) {
				$('body').append('<div id="buildingBox" />');
				$('#buildingBox').dsBox({
					draggable:true,
					center:true
				});
			}

			var buildingBox = $('#buildingBox');
			buildingBox.dsBox('show');
			var content = buildingBox.find('.content');
			content.empty();
			content.append('Lade...');
		}

		function renderBuilding(model) {
			var content = $('#buildingBox .content');
			content.empty();

			var tmpl = '<div class="head">'+
				'<img src="./{{building.picture}}" alt="" /> {{building.name}}'+
				'</div>'+
				'<div class="message" />'+
				'<div class="status">Status: '+
				'{{#building.active}}<span class="aktiv">Aktiv</span>{{/building.active}}'+
				'{{^building.active}}<span class="inaktiv">Inaktiv</span>{{/building.active}}'+
				'</div>'+
				'{{{ui}}}'+
				'<ul class="aktionen">Aktionen: {{#building.deakable}}'+
				'{{#building.active}}'+
				'<li><a id="startStopBuilding" href="#">deaktivieren</a></li>'+
				'{{/building.active}}'+
				'{{^building.active}}'+
				'<li><a id="startStopBuilding" href="#">aktivieren</a></li>'+
				'{{/building.active}}'+
				'{{/building.deakable}}'+
				'{{^building.kommandozentrale}}'+
				'<li><a class="error" id="demoBuilding" href="#">demontieren</a></li>'+
				'{{/building.kommandozentrale}}'+
				'{{#building.kommandozentrale}}'+
				'<li><a class="error" id="demoBuilding" href="{{URL}}?module=building&action=demo&col={{col}}&field={{field}}&conf=ok">Asteroid aufgeben</a></li>'+
				'{{/building.kommandozentrale}}'+
				"</ul>";

			var buildingUi;
			// BUILDING
			if( model.building.type === 'DefaultBuilding' ) {
				buildingUi = __defaultBuildingHandler.generateOutput(model);
			}
			else {
				buildingUi = "Unbekannter Geb&auml;udetyp: "+model.building.type;
			}

			model.ui = buildingUi;

			var out = DS.render(tmpl, model);

			content.append(out);
			DsTooltip.update(content);
		}

		function doShutdown() {
			$('#startStopBuilding').text('aktivieren');
			$('#buildingBox .status .aktiv').remove();
			$('#buildingBox .status').append('<span class="inaktiv">Inaktiv</span>');
		}

		function doStartup() {
			$('#startStopBuilding').text('deaktivieren');
			$('#buildingBox .status .inaktiv').remove();
			$('#buildingBox .status').append('<span class="aktiv">Aktiv</span>');
		}

		function renderMessage(message) {
			$('#buildingBox .message')
				.empty()
				.append(message);
		}

		function renderAskDemo(model) {
			var content = $('#buildingBox .content');
			content.empty();

			var tmpl = '<div class="head">'+
				'<img src="./{{building.picture}}" alt="" /> {{building.name}}'+
				'</div>'+
				'<div class="message">Wollen sie dieses Geb&auml;ude wirklich demontieren?</div>'+
				'<ul class="confirm">'+
				'<li><a id="cancelDemo" href="#">abbrechen</a></li>'+
				'<li><a class="error" id="okDemo" href="#">demontieren</a></li>'+
				'</ul>';

			var out = DS.render(tmpl, model);

			content.append(out);
			DsTooltip.update(content);
		}

		function renderDemoResponse(demoModel) {
			var tmpl = '<div align="center">R&uumlckerstattung:</div><br />'+
				'{{#demoCargo}}'+
				'<img src="{{image}}" alt="" />{{{cargo1}}}'+
				'{{#spaceMissing}} - <span style="color:red">Nicht genug Platz f&uumlr alle Waren</span>{{/spaceMissing}}'+
				'<br />'+
				'{{/demoCargo}}'+
				'<br />'+
				'<hr noshade="noshade" size="1" style="color:#cccccc" /><br />'+
				'<div align="center"><span style="color:#ff0000">Das Geb&aumlude wurde demontiert</span></div>';

			var content = $('#buildingBox .content');
			content.empty();

			var out = DS.render(tmpl, demoModel);

			content.append(out);
			DsTooltip.update(content);
		}

		createViewBox();

		// Public Methods
		this.renderEmpty = renderEmpty;
		this.renderBuilding = renderBuilding;
		this.doShutdown = doShutdown;
		this.doStartup = doStartup;
		this.renderMessage = renderMessage;
		this.renderAskDemo = renderAskDemo;
		this.renderDemoResponse = renderDemoResponse;
	}

	var view = new BuildingView();

	function showAskDemo(model) {
		view.renderAskDemo(model);

		$('#cancelDemo').bind('click', function() {
			__parseBuildingResponse(model);
		});
		$('#okDemo').bind('click', function() {
			DS.getJSON(
				{module:'building', action:'demoAjax', 'col':model.col, 'field':model.field},
				parseDemoBuilding
			);
			return false;
		});
	}

	function parseDemoBuilding(demoModel) {
		view.renderDemoResponse(demoModel);

		Base.AskField(demoModel.field);
		Base.noBuildingHighlight();
	}

	function __parseBuildingResponse(model) {
		if( model.noJsonSupport ) {
			document.location.href=DS.getUrl()+'?module=building&action=default&col='+model.col+'&field='+model.field;
			return;
		}

		view.renderBuilding(model);

		if( model.building.kommandozentrale ) {
			$('#demoBuilding').bind('click.demoBuilding', function() {
				return confirm('Wollen sie den Asteroiden wirklich aufgeben?');
			});
		}
		else
		{
			$('#demoBuilding').bind('click.demoBuilding', function() {
				showAskDemo(model);
			});
		}

		__bindBuildingStartStop(model.col, model.field, model.building.active);
	}

	function __bindBuildingStartStop(col, field, active) {
		$('#startStopBuilding').unbind('click.startStop');

		if( active ) {
			$('#startStopBuilding').bind('click.startStop', function() {
				DS.getJSON(
					{module:'building', action:'shutdownAjax', 'col':col, 'field':field},
					__parseBuildingShutdown
				);
				return false;
			});
		}
		else {
			$('#startStopBuilding').bind('click.startStop', function() {
				DS.getJSON(
					{module:'building', action:'startAjax', 'col':col, 'field':field},
					__parseBuildingStart
				);
				return false;
			});
		}
	}

	function __parseBuildingShutdown(resp) {
		if( !resp.success ) {
			view.renderMessage(resp.message);

			return;
		}

		var buildingTile = document.querySelector('#baseMap .p' + resp.field);
		buildingTile.classList.add('offline');

		view.doShutdown();

		__bindBuildingStartStop(resp.col, resp.field, false);

		base.UpdateAllButBuildings();
	}

	function __parseBuildingStart(resp) {
		if( !resp.success ) {
			view.renderMessage(resp.message);

			return;
		}
		var buildingTile = document.querySelector('#baseMap .p' + resp.field);
		buildingTile.classList.remove('offline');

		view.doStartup();

		__bindBuildingStartStop(resp.col, resp.field, true);

		base.UpdateAllButBuildings();
	}

	view.renderEmpty();

	DS.getJSON({
		module:'building',
		action:'ajax',
		col:base.getBaseId(),
		field:tileId
	}, __parseBuildingResponse);
}

/*
Neue Thymeleaf GUI Funktionen
 */
function toggleBaumenu(){
	var baumenu = document.getElementById("baumenu-asteroid");
	//var baumenuSwitch = document.getElementById("baumenu-switch");
	var aktionen = document.getElementById("aktionen-asteroid");


	var buttonParent = document.getElementById("verwaltung-bauen");

	test = buttonParent.querySelectorAll("button");

	if(test[0].classList.contains("aktiv"))
	{
		test[0].classList.remove("aktiv");
		test[1].classList.add("aktiv");
	}
	else
	{
		test[1].classList.remove("aktiv");
		test[0].classList.add("aktiv");
	}

	if(aktionen.style.display === "none")
	{
		Base.deselectBuilding();
	}

	toggleElement(baumenu);
	toggleElement(aktionen);
	defade();
}

function toggleLagermenu(){
	var cargo = document.getElementById("cargo-asteroid");
	var einheiten = document.getElementById("einheiten-asteroid");
	var buttonParent = document.getElementById("lager-einheiten");

	test = buttonParent.querySelectorAll("button");

	if(test[0].classList.contains("aktiv"))
	{
		test[0].classList.remove("aktiv");
		test[1].classList.add("aktiv");
	}
	else
	{
		test[1].classList.remove("aktiv");
		test[0].classList.add("aktiv");
	}

	toggleElement(einheiten);
	toggleElement(cargo);
	defade();
}
var test;
function toggleElement(element, display="block") {
	if (element.style.display === "none") {
		element.style.display = display;
	} else {
		element.style.display = "none";
	}
}

function defade(){
	var elemente = document.getElementsByClassName("fadein");
	while(elemente[0]) {
		elemente[0].classList.remove("fadein");
	};
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

function tabWechsel(element, categoryName) {
	var i;
	var x = element.closest('.gfxbox').querySelectorAll('.tab-element');
	for (i = 0; i < x.length; i++) {
		x[i].style.display = "none";
	}
	document.getElementById(categoryName).style.display = "block";
	Base.deselectBuilding();
}

