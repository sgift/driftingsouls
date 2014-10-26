/*
	Automatisch generierte Datei. Zum generieren bitte JsServiceGenerator aufrufen.
*/
(function() {
	var angularModule = angular.module('ds.service.ajax', ['ds.service.ds']);
	// net.driftingsouls.ds2.server.modules.AdminController
	/**
	 * @class
	 **/
	var DsAjaxPromise_AdminControllerStub_ajaxAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:string)} callback
		 * @memberof DsAjaxPromise_AdminControllerStub_ajaxAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_AdminControllerStub_entityPluginOverviewAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.AdminController.EntityPluginOverviewViewModel)} callback
		 * @memberof DsAjaxPromise_AdminControllerStub_entityPluginOverviewAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_AdminControllerStub_tableDataAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.admin.editoren.JqGridTableDataViewModel)} callback
		 * @memberof DsAjaxPromise_AdminControllerStub_tableDataAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} AdminControllerStub
	 **/
	function AdminControllerStub(ds) {
		return {
			/**
			 * @memberof AdminControllerStub
			 * @param {string} namedplugin 
			 * @returns {DsAjaxPromise_AdminControllerStub_ajaxAction}
			 **/
			ajax : function(namedplugin) {
				var options={};
				options.module='admin';
				options.action='ajax';
				options.namedplugin=namedplugin;
				return ds(options);
			},
			/**
			 * @memberof AdminControllerStub
			 * @param {string} namedplugin 
			 * @param {number} page 
			 * @param {number} rows 
			 * @returns {DsAjaxPromise_AdminControllerStub_entityPluginOverviewAction}
			 **/
			entityPluginOverview : function(namedplugin,page,rows) {
				var options={};
				options.module='admin';
				options.action='entityPluginOverview';
				options.namedplugin=namedplugin;
				options.page=page;
				options.rows=rows;
				return ds(options);
			},
			/**
			 * @memberof AdminControllerStub
			 * @param {string} namedplugin 
			 * @param {number} page 
			 * @param {number} rows 
			 * @param {string} sidx 
			 * @param {string} sord 
			 * @returns {DsAjaxPromise_AdminControllerStub_tableDataAction}
			 **/
			tableData : function(namedplugin,page,rows,sidx,sord) {
				var options={};
				options.module='admin';
				options.action='tableData';
				options.namedplugin=namedplugin;
				options.page=page;
				options.rows=rows;
				options.sidx=sidx;
				options.sord=sord;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('AdminControllerStub', ['ds', AdminControllerStub]);
	// net.driftingsouls.ds2.server.modules.BaseController
	/**
	 * @class
	 **/
	var DsAjaxPromise_BaseControllerStub_ajaxAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.BaseController.AjaxViewModel)} callback
		 * @memberof DsAjaxPromise_BaseControllerStub_ajaxAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} BaseControllerStub
	 **/
	function BaseControllerStub(ds) {
		return {
			/**
			 * @memberof BaseControllerStub
			 * @param {object} base 
			 * @returns {DsAjaxPromise_BaseControllerStub_ajaxAction}
			 **/
			ajax : function(base) {
				var options={};
				options.module='base';
				options.action='ajax';
				options.col=base;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('BaseControllerStub', ['ds', BaseControllerStub]);
	// net.driftingsouls.ds2.server.modules.BuildingController
	/**
	 * @class
	 **/
	var DsAjaxPromise_BuildingControllerStub_startAjaxAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.BuildingController.BuildingActionViewModel)} callback
		 * @memberof DsAjaxPromise_BuildingControllerStub_startAjaxAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_BuildingControllerStub_shutdownAjaxAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.BuildingController.BuildingActionViewModel)} callback
		 * @memberof DsAjaxPromise_BuildingControllerStub_shutdownAjaxAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_BuildingControllerStub_demoAjaxAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.BuildingController.DemoViewModel)} callback
		 * @memberof DsAjaxPromise_BuildingControllerStub_demoAjaxAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_BuildingControllerStub_ajaxAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.BuildingController.AjaxViewModel)} callback
		 * @memberof DsAjaxPromise_BuildingControllerStub_ajaxAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} BuildingControllerStub
	 **/
	function BuildingControllerStub(ds) {
		return {
			/**
			 * @memberof BuildingControllerStub
			 * @param {object} base 
			 * @param {number} field 
			 * @returns {DsAjaxPromise_BuildingControllerStub_startAjaxAction}
			 **/
			startAjax : function(base,field) {
				var options={};
				options.module='building';
				options.action='startAjax';
				options.col=base;
				options.field=field;
				return ds(options);
			},
			/**
			 * @memberof BuildingControllerStub
			 * @param {object} base 
			 * @param {number} field 
			 * @returns {DsAjaxPromise_BuildingControllerStub_shutdownAjaxAction}
			 **/
			shutdownAjax : function(base,field) {
				var options={};
				options.module='building';
				options.action='shutdownAjax';
				options.col=base;
				options.field=field;
				return ds(options);
			},
			/**
			 * @memberof BuildingControllerStub
			 * @param {object} base 
			 * @param {number} field 
			 * @returns {DsAjaxPromise_BuildingControllerStub_demoAjaxAction}
			 **/
			demoAjax : function(base,field) {
				var options={};
				options.module='building';
				options.action='demoAjax';
				options.col=base;
				options.field=field;
				return ds(options);
			},
			/**
			 * @memberof BuildingControllerStub
			 * @param {object} base 
			 * @param {number} field 
			 * @returns {DsAjaxPromise_BuildingControllerStub_ajaxAction}
			 **/
			ajax : function(base,field) {
				var options={};
				options.module='building';
				options.action='ajax';
				options.col=base;
				options.field=field;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('BuildingControllerStub', ['ds', BuildingControllerStub]);
	// net.driftingsouls.ds2.server.modules.CommController
	/**
	 * @class
	 **/
	var DsAjaxPromise_CommControllerStub_moveAjaxAct = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:string)} callback
		 * @memberof DsAjaxPromise_CommControllerStub_moveAjaxAct
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} CommControllerStub
	 **/
	function CommControllerStub(ds) {
		return {
			/**
			 * @memberof CommControllerStub
			 * @param {object} moveto 
			 * @param {object} source 
			 * @param {object.<number,object>} ordnerMap 
			 * @param {object.<number,number>} pmMap 
			 * @returns {DsAjaxPromise_CommControllerStub_moveAjaxAct}
			 **/
			move : function(moveto,source,ordnerMap,pmMap) {
				var options={};
				options.module='comm';
				options.action='move';
				options.moveto=moveto;
				options.ordner=source;
				angular.forEach(ordnerMap, function(value,key) {
					options['ordner_'+key+'']=value;
				});
				angular.forEach(pmMap, function(value,key) {
					options['pm_'+key+'']=value;
				});
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('CommControllerStub', ['ds', CommControllerStub]);
	// net.driftingsouls.ds2.server.modules.ImpObjectsController
	/**
	 * @class
	 **/
	var DsAjaxPromise_ImpObjectsControllerStub_defaultAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.ImpObjectsController.JsonViewModel)} callback
		 * @memberof DsAjaxPromise_ImpObjectsControllerStub_defaultAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_ImpObjectsControllerStub_jsonAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.ImpObjectsController.JsonViewModel)} callback
		 * @memberof DsAjaxPromise_ImpObjectsControllerStub_jsonAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} ImpObjectsControllerStub
	 **/
	function ImpObjectsControllerStub(ds) {
		return {
			/**
			 * @memberof ImpObjectsControllerStub
			 * @param {object} system 
			 * @returns {DsAjaxPromise_ImpObjectsControllerStub_defaultAction}
			 **/
			defaultAction : function(system) {
				var options={};
				options.module='impobjects';
				options.action='default';
				options.system=system;
				return ds(options);
			},
			/**
			 * @memberof ImpObjectsControllerStub
			 * @param {object} system 
			 * @returns {DsAjaxPromise_ImpObjectsControllerStub_jsonAction}
			 **/
			json : function(system) {
				var options={};
				options.module='impobjects';
				options.action='json';
				options.system=system;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('ImpObjectsControllerStub', ['ds', ImpObjectsControllerStub]);
	// net.driftingsouls.ds2.server.modules.ItemInfoController
	/**
	 * @class
	 **/
	var DsAjaxPromise_ItemInfoControllerStub_ajaxAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.ItemInfoController.AjaxViewModel)} callback
		 * @memberof DsAjaxPromise_ItemInfoControllerStub_ajaxAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} ItemInfoControllerStub
	 **/
	function ItemInfoControllerStub(ds) {
		return {
			/**
			 * @memberof ItemInfoControllerStub
			 * @returns {DsAjaxPromise_ItemInfoControllerStub_ajaxAction}
			 **/
			ajax : function() {
				var options = {};
				options.module='iteminfo';
				options.action='ajax';
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('ItemInfoControllerStub', ['ds', ItemInfoControllerStub]);
	// net.driftingsouls.ds2.server.modules.MainController
	/**
	 * @class
	 **/
	var DsAjaxPromise_MainControllerStub_speicherNotizen = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_MainControllerStub_speicherNotizen
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_MainControllerStub_statusUpdateAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.MainController.Status)} callback
		 * @memberof DsAjaxPromise_MainControllerStub_statusUpdateAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_MainControllerStub_getHelpText = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:object)} callback
		 * @memberof DsAjaxPromise_MainControllerStub_getHelpText
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_MainControllerStub_loadVersionInfo = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.MainController.VersionInformation)} callback
		 * @memberof DsAjaxPromise_MainControllerStub_loadVersionInfo
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_MainControllerStub_loadLastCommits = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:string)} callback
		 * @memberof DsAjaxPromise_MainControllerStub_loadLastCommits
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} MainControllerStub
	 **/
	function MainControllerStub(ds) {
		return {
			/**
			 * @memberof MainControllerStub
			 * @param {string} notizen 
			 * @returns {DsAjaxPromise_MainControllerStub_speicherNotizen}
			 **/
			speicherNotizen : function(notizen) {
				var options={};
				options.module='main';
				options.action='speicherNotizen';
				options.notizen=notizen;
				return ds(options);
			},
			/**
			 * @memberof MainControllerStub
			 * @returns {DsAjaxPromise_MainControllerStub_statusUpdateAction}
			 **/
			statusUpdate : function() {
				var options = {};
				options.module='main';
				options.action='statusUpdate';
				return ds(options);
			},
			/**
			 * @memberof MainControllerStub
			 * @param {object} page 
			 * @returns {DsAjaxPromise_MainControllerStub_getHelpText}
			 **/
			getHelpText : function(page) {
				var options={};
				options.module='main';
				options.action='getHelpText';
				options.page=page;
				return ds(options);
			},
			/**
			 * @memberof MainControllerStub
			 * @returns {DsAjaxPromise_MainControllerStub_loadVersionInfo}
			 **/
			loadVersionInfo : function() {
				var options = {};
				options.module='main';
				options.action='loadVersionInfo';
				return ds(options);
			},
			/**
			 * @memberof MainControllerStub
			 * @returns {DsAjaxPromise_MainControllerStub_loadLastCommits}
			 **/
			loadLastCommits : function() {
				var options = {};
				options.module='main';
				options.action='loadLastCommits';
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('MainControllerStub', ['ds', MainControllerStub]);
	// net.driftingsouls.ds2.server.modules.MapController
	/**
	 * @class
	 **/
	var DsAjaxPromise_MapControllerStub_speichereSystemkarteAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_MapControllerStub_speichereSystemkarteAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_MapControllerStub_systemauswahlAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.MapController.SystemauswahlViewModel)} callback
		 * @memberof DsAjaxPromise_MapControllerStub_systemauswahlAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_MapControllerStub_mapAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.MapController.MapViewModel)} callback
		 * @memberof DsAjaxPromise_MapControllerStub_mapAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_MapControllerStub_sectorAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.MapController.SectorViewModel)} callback
		 * @memberof DsAjaxPromise_MapControllerStub_sectorAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} MapControllerStub
	 **/
	function MapControllerStub(ds) {
		return {
			/**
			 * @memberof MapControllerStub
			 * @param {object.<object,number>} xWerte 
			 * @param {object.<object,number>} yWerte 
			 * @returns {DsAjaxPromise_MapControllerStub_speichereSystemkarteAction}
			 **/
			speichereSystemkarte : function(xWerte,yWerte) {
				var options={};
				options.module='map';
				options.action='speichereSystemkarte';
				angular.forEach(xWerte, function(value,key) {
					options['sys'+key+'x']=value;
				});
				angular.forEach(yWerte, function(value,key) {
					options['sys'+key+'y']=value;
				});
				return ds(options);
			},
			/**
			 * @memberof MapControllerStub
			 * @param {object} sys 
			 * @returns {DsAjaxPromise_MapControllerStub_systemauswahlAction}
			 **/
			systemauswahl : function(sys) {
				var options={};
				options.module='map';
				options.action='systemauswahl';
				options.sys=sys;
				return ds(options);
			},
			/**
			 * @memberof MapControllerStub
			 * @param {object} sys 
			 * @param {number} xstart 
			 * @param {number} xend 
			 * @param {number} ystart 
			 * @param {number} yend 
			 * @param {boolean} admin 
			 * @returns {DsAjaxPromise_MapControllerStub_mapAction}
			 **/
			map : function(sys,xstart,xend,ystart,yend,admin) {
				var options={};
				options.module='map';
				options.action='map';
				options.sys=sys;
				options.xstart=xstart;
				options.xend=xend;
				options.ystart=ystart;
				options.yend=yend;
				options.admin=admin ? 1 : 0;
				return ds(options);
			},
			/**
			 * @memberof MapControllerStub
			 * @param {object} sys 
			 * @param {number} x 
			 * @param {number} y 
			 * @param {object} scanship 
			 * @param {boolean} admin 
			 * @returns {DsAjaxPromise_MapControllerStub_sectorAction}
			 **/
			sector : function(sys,x,y,scanship,admin) {
				var options={};
				options.module='map';
				options.action='sector';
				options.sys=sys;
				options.x=x;
				options.y=y;
				options.scanship=scanship;
				options.admin=admin ? 1 : 0;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('MapControllerStub', ['ds', MapControllerStub]);
	// net.driftingsouls.ds2.server.modules.NpcController
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_shopMenuAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.NpcController.ShopMenuViewModel)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_shopMenuAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_awardMedalAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_awardMedalAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_changeRankAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_changeRankAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_deleteLpAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_deleteLpAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_editLpAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_editLpAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_meldungBearbeitetAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_meldungBearbeitetAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_lpMenuAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.NpcController.LpMenuViewModel)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_lpMenuAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_raengeMenuAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.NpcController.RaengeMenuViewModel)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_raengeMenuAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_changeOrderLocationAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_changeOrderLocationAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_orderShipsAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_orderShipsAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_orderAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_orderAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_NpcControllerStub_orderMenuAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.NpcController.OrderMenuViewModel)} callback
		 * @memberof DsAjaxPromise_NpcControllerStub_orderMenuAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} NpcControllerStub
	 **/
	function NpcControllerStub(ds) {
		return {
			/**
			 * @memberof NpcControllerStub
			 * @returns {DsAjaxPromise_NpcControllerStub_shopMenuAction}
			 **/
			shopMenu : function() {
				var options = {};
				options.module='npc';
				options.action='shopMenu';
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {string} edituserID 
			 * @param {object} medal 
			 * @param {string} reason 
			 * @returns {DsAjaxPromise_NpcControllerStub_awardMedalAction}
			 **/
			awardMedal : function(edituserID,medal,reason) {
				var options={};
				options.module='npc';
				options.action='awardMedal';
				options.edituser=edituserID;
				options.medal=medal;
				options.reason=reason;
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {string} edituserID 
			 * @param {number} rank 
			 * @returns {DsAjaxPromise_NpcControllerStub_changeRankAction}
			 **/
			changeRank : function(edituserID,rank) {
				var options={};
				options.module='npc';
				options.action='changeRank';
				options.edituser=edituserID;
				options.rank=rank;
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {string} edituserID 
			 * @param {number} lpId 
			 * @returns {DsAjaxPromise_NpcControllerStub_deleteLpAction}
			 **/
			deleteLp : function(edituserID,lpId) {
				var options={};
				options.module='npc';
				options.action='deleteLp';
				options.edituser=edituserID;
				options.lp=lpId;
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {string} edituserID 
			 * @param {string} grund 
			 * @param {string} anmerkungen 
			 * @param {number} punkte 
			 * @param {boolean} pm 
			 * @returns {DsAjaxPromise_NpcControllerStub_editLpAction}
			 **/
			editLp : function(edituserID,grund,anmerkungen,punkte,pm) {
				var options={};
				options.module='npc';
				options.action='editLp';
				options.edituser=edituserID;
				options.grund=grund;
				options.anmerkungen=anmerkungen;
				options.punkte=punkte;
				options.pm=pm ? 1 : 0;
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {object} meldung 
			 * @returns {DsAjaxPromise_NpcControllerStub_meldungBearbeitetAction}
			 **/
			meldungBearbeitet : function(meldung) {
				var options={};
				options.module='npc';
				options.action='meldungBearbeitet';
				options.meldung=meldung;
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {string} edituserID 
			 * @param {boolean} alleMeldungen 
			 * @returns {DsAjaxPromise_NpcControllerStub_lpMenuAction}
			 **/
			lpMenu : function(edituserID,alleMeldungen) {
				var options={};
				options.module='npc';
				options.action='lpMenu';
				options.edituser=edituserID;
				options.alleMeldungen=alleMeldungen ? 1 : 0;
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {string} edituserID 
			 * @returns {DsAjaxPromise_NpcControllerStub_raengeMenuAction}
			 **/
			raengeMenu : function(edituserID) {
				var options={};
				options.module='npc';
				options.action='raengeMenu';
				options.edituser=edituserID;
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {string} lieferposition 
			 * @returns {DsAjaxPromise_NpcControllerStub_changeOrderLocationAction}
			 **/
			changeOrderLocation : function(lieferposition) {
				var options={};
				options.module='npc';
				options.action='changeOrderLocation';
				options.lieferposition=lieferposition;
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {boolean} flagDisableIff 
			 * @param {boolean} flagHandelsposten 
			 * @param {boolean} flagNichtKaperbar 
			 * @param {object.<object,number>} shipCounts 
			 * @returns {DsAjaxPromise_NpcControllerStub_orderShipsAction}
			 **/
			orderShips : function(flagDisableIff,flagHandelsposten,flagNichtKaperbar,shipCounts) {
				var options={};
				options.module='npc';
				options.action='orderShips';
				options.shipflag_disableiff=flagDisableIff ? 1 : 0;
				options.shipflag_handelsposten=flagHandelsposten ? 1 : 0;
				options.shipflag_nichtkaperbar=flagNichtKaperbar ? 1 : 0;
				angular.forEach(shipCounts, function(value,key) {
					options['ship'+key+'_count']=value;
				});
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @param {object} order 
			 * @param {number} count 
			 * @returns {DsAjaxPromise_NpcControllerStub_orderAction}
			 **/
			order : function(order,count) {
				var options={};
				options.module='npc';
				options.action='order';
				options.order=order;
				options.count=count;
				return ds(options);
			},
			/**
			 * @memberof NpcControllerStub
			 * @returns {DsAjaxPromise_NpcControllerStub_orderMenuAction}
			 **/
			orderMenu : function() {
				var options = {};
				options.module='npc';
				options.action='orderMenu';
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('NpcControllerStub', ['ds', NpcControllerStub]);
	// net.driftingsouls.ds2.server.modules.OptionsController
	/**
	 * @class
	 **/
	var DsAjaxPromise_OptionsControllerStub_generierePersonenNamenBeispiele = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.OptionsController.GenerierePersonenNamenBeispieleViewModel)} callback
		 * @memberof DsAjaxPromise_OptionsControllerStub_generierePersonenNamenBeispiele
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_OptionsControllerStub_generiereSchiffsNamenBeispiele = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.OptionsController.GeneriereSchiffsNamenBeispieleViewModel)} callback
		 * @memberof DsAjaxPromise_OptionsControllerStub_generiereSchiffsNamenBeispiele
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} OptionsControllerStub
	 **/
	function OptionsControllerStub(ds) {
		return {
			/**
			 * @memberof OptionsControllerStub
			 * @param {object} generator 
			 * @returns {DsAjaxPromise_OptionsControllerStub_generierePersonenNamenBeispiele}
			 **/
			generierePersonenNamenBeispiele : function(generator) {
				var options={};
				options.module='options';
				options.action='generierePersonenNamenBeispiele';
				options.generator=generator;
				return ds(options);
			},
			/**
			 * @memberof OptionsControllerStub
			 * @param {object} schiffsKlassenNamenGenerator 
			 * @param {object} schiffsNamenGenerator 
			 * @returns {DsAjaxPromise_OptionsControllerStub_generiereSchiffsNamenBeispiele}
			 **/
			generiereSchiffsNamenBeispiele : function(schiffsKlassenNamenGenerator,schiffsNamenGenerator) {
				var options={};
				options.module='options';
				options.action='generiereSchiffsNamenBeispiele';
				options.schiffsKlassenNamenGenerator=schiffsKlassenNamenGenerator;
				options.schiffsNamenGenerator=schiffsNamenGenerator;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('OptionsControllerStub', ['ds', OptionsControllerStub]);
	// net.driftingsouls.ds2.server.modules.SchiffAjaxController
	/**
	 * @class
	 **/
	var DsAjaxPromise_SchiffAjaxControllerStub_alarmAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.framework.ViewMessage)} callback
		 * @memberof DsAjaxPromise_SchiffAjaxControllerStub_alarmAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_SchiffAjaxControllerStub_springenAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:object)} callback
		 * @memberof DsAjaxPromise_SchiffAjaxControllerStub_springenAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_SchiffAjaxControllerStub_springenViaSchiffAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:object)} callback
		 * @memberof DsAjaxPromise_SchiffAjaxControllerStub_springenViaSchiffAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_SchiffAjaxControllerStub_fliegeSchiffAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:object)} callback
		 * @memberof DsAjaxPromise_SchiffAjaxControllerStub_fliegeSchiffAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} SchiffAjaxControllerStub
	 **/
	function SchiffAjaxControllerStub(ds) {
		return {
			/**
			 * @memberof SchiffAjaxControllerStub
			 * @param {object} schiff 
			 * @param {object} alarm 
			 * @returns {DsAjaxPromise_SchiffAjaxControllerStub_alarmAction}
			 **/
			alarm : function(schiff,alarm) {
				var options={};
				options.module='schiffAjax';
				options.action='alarm';
				options.schiff=schiff;
				options.alarm=alarm;
				return ds(options);
			},
			/**
			 * @memberof SchiffAjaxControllerStub
			 * @param {object} schiff 
			 * @param {object} sprungpunkt 
			 * @returns {DsAjaxPromise_SchiffAjaxControllerStub_springenAction}
			 **/
			springen : function(schiff,sprungpunkt) {
				var options={};
				options.module='schiffAjax';
				options.action='springen';
				options.schiff=schiff;
				options.sprungpunkt=sprungpunkt;
				return ds(options);
			},
			/**
			 * @memberof SchiffAjaxControllerStub
			 * @param {object} schiff 
			 * @param {object} sprungpunktSchiff 
			 * @returns {DsAjaxPromise_SchiffAjaxControllerStub_springenViaSchiffAction}
			 **/
			springenViaSchiff : function(schiff,sprungpunktSchiff) {
				var options={};
				options.module='schiffAjax';
				options.action='springenViaSchiff';
				options.schiff=schiff;
				options.sprungpunktSchiff=sprungpunktSchiff;
				return ds(options);
			},
			/**
			 * @memberof SchiffAjaxControllerStub
			 * @param {object} schiff 
			 * @param {number} x 
			 * @param {number} y 
			 * @returns {DsAjaxPromise_SchiffAjaxControllerStub_fliegeSchiffAction}
			 **/
			fliegeSchiff : function(schiff,x,y) {
				var options={};
				options.module='schiffAjax';
				options.action='fliegeSchiff';
				options.schiff=schiff;
				options.x=x;
				options.y=y;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('SchiffAjaxControllerStub', ['ds', SchiffAjaxControllerStub]);
	// net.driftingsouls.ds2.server.modules.SearchController
	/**
	 * @class
	 **/
	var DsAjaxPromise_SearchControllerStub_defaultAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.SearchController.SearchViewModel)} callback
		 * @memberof DsAjaxPromise_SearchControllerStub_defaultAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class
	 **/
	var DsAjaxPromise_SearchControllerStub_searchAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.SearchController.SearchViewModel)} callback
		 * @memberof DsAjaxPromise_SearchControllerStub_searchAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} SearchControllerStub
	 **/
	function SearchControllerStub(ds) {
		return {
			/**
			 * @memberof SearchControllerStub
			 * @param {string} search 
			 * @param {string} only 
			 * @param {number} max 
			 * @returns {DsAjaxPromise_SearchControllerStub_defaultAction}
			 **/
			defaultAction : function(search,only,max) {
				var options={};
				options.module='search';
				options.action='default';
				options.search=search;
				options.only=only;
				options.max=max;
				return ds(options);
			},
			/**
			 * @memberof SearchControllerStub
			 * @param {string} search 
			 * @param {string} only 
			 * @param {number} max 
			 * @returns {DsAjaxPromise_SearchControllerStub_searchAction}
			 **/
			search : function(search,only,max) {
				var options={};
				options.module='search';
				options.action='search';
				options.search=search;
				options.only=only;
				options.max=max;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('SearchControllerStub', ['ds', SearchControllerStub]);
	// net.driftingsouls.ds2.server.modules.StatsController
	/**
	 * @class
	 **/
	var DsAjaxPromise_StatsControllerStub_ajaxAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.stats.AjaxStatistic.DataViewModel)} callback
		 * @memberof DsAjaxPromise_StatsControllerStub_ajaxAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} StatsControllerStub
	 **/
	function StatsControllerStub(ds) {
		return {
			/**
			 * @memberof StatsControllerStub
			 * @param {number} stat 
			 * @param {number} show 
			 * @returns {DsAjaxPromise_StatsControllerStub_ajaxAction}
			 **/
			ajax : function(stat,show) {
				var options={};
				options.module='stats';
				options.action='ajax';
				options.stat=stat;
				options.show=show;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('StatsControllerStub', ['ds', StatsControllerStub]);
	// net.driftingsouls.ds2.server.modules.TechListeController
	/**
	 * @class
	 **/
	var DsAjaxPromise_TechListeControllerStub_defaultAction = {
		/**
		 * @name success
		 * @function
		 * @param {function(data:ds.viewmodel.modules.TechListeController.TechListeViewModel)} callback
		 * @memberof DsAjaxPromise_TechListeControllerStub_defaultAction
		 **/
		success : function(callback) {}
	};
	/**
	 * @class {object} TechListeControllerStub
	 **/
	function TechListeControllerStub(ds) {
		return {
			/**
			 * @memberof TechListeControllerStub
			 * @param {number} rasse 
			 * @returns {DsAjaxPromise_TechListeControllerStub_defaultAction}
			 **/
			defaultAction : function(rasse) {
				var options={};
				options.module='techliste';
				options.action='default';
				options.rasse=rasse;
				return ds(options);
			}		
		}
	}
	angularModule = angularModule.factory('TechListeControllerStub', ['ds', TechListeControllerStub]);
	/**
	 * @namespace ds
	 **/
	/**
	 * @namespace ds.viewmodel
	 **/
	/**
	 * @namespace ds.viewmodel.modules
	 **/
	/**
	 * @namespace ds.viewmodel.modules.ImpObjectsController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.ImpObjectsController.JsonViewModel
	 * @property {ds.viewmodel.modules.ImpObjectsController.JsonViewModel.SystemViewModel} system
	 * @property {object} jumpnodes
	 * @property {object} posten
	 * @property {object} bases
	 **/
	/**
	 * @namespace ds.viewmodel.modules.MainController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.MainController.VersionInformation
	 * @property {string} build
	 * @property {string} commit
	 * @property {string} buildTime
	 * @property {string} buildUrl
	 **/
	/**
	 * @namespace ds.viewmodel.modules.AdminController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.AdminController.EntityPluginOverviewViewModel
	 * @property {ds.viewmodel.modules.admin.editoren.JqGridViewModel} table
	 * @property {ds.viewmodel.modules.admin.editoren.EntitySelectionViewModel} entitySelection
	 **/
	/**
	 * @namespace ds.viewmodel.modules.OptionsController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.OptionsController.GenerierePersonenNamenBeispieleViewModel
	 * @property {object} namen
	 **/
	/**
	 * @namespace ds.viewmodel.modules.ItemInfoController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.ItemInfoController.AjaxViewModel
	 * @property {object} items
	 **/
	/**
	 * @namespace ds.viewmodel.modules.NpcController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.NpcController.OrderMenuViewModel
	 * @property {number} npcpunkte
	 * @property {string} aktuelleLieferposition
	 * @property {object} offiziere
	 * @property {object} ships
	 * @property {object} lieferpositionen
	 * @property {object} menu
	 **/
	/**
	 * @namespace ds.viewmodel.modules.BuildingController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.BuildingController.BuildingActionViewModel
	 * @property {number} col
	 * @property {number} field
	 * @property {ds.viewmodel.modules.viewmodels.GebaeudeAufBasisViewModel} building
	 * @property {boolean} success
	 * @property {string} message
	 **/
	/**
	 * @namespace ds.viewmodel.modules.BaseController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.BaseController.AjaxViewModel
	 * @property {number} col
	 * @property {ds.viewmodel.modules.BaseController.AjaxViewModel.BaseViewModel} base
	 * @property {object} karte
	 * @property {object} gebaeudeStatus
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.NpcController.LpMenuViewModel
	 * @property {boolean} alleMeldungen
	 * @property {object} meldungen
	 * @property {ds.viewmodel.modules.viewmodels.UserViewModel} user
	 * @property {object} lpListe
	 * @property {string} rang
	 * @property {number} lpBeiNpc
	 * @property {object} menu
	 **/
	/**
	 * @namespace ds.viewmodel.modules.stats
	 **/
	/**
	 * @namespace ds.viewmodel.modules.stats.AjaxStatistic
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.stats.AjaxStatistic.DataViewModel
	 * @property {ds.viewmodel.modules.stats.AjaxStatistic.DataViewModel.KeyViewModel} key
	 * @property {object} data
	 **/
	/**
	 * @namespace ds.viewmodel.modules.MapController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.MapController.SectorViewModel
	 * @property {number} subraumspaltenCount
	 * @property {boolean} roterAlarm
	 * @property {object} users
	 * @property {object} bases
	 * @property {object} jumpnodes
	 * @property {object} battles
	 * @property {ds.viewmodel.modules.MapController.SectorViewModel.NebelViewModel} nebel
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.NpcController.RaengeMenuViewModel
	 * @property {ds.viewmodel.modules.viewmodels.UserViewModel} user
	 * @property {number} aktiverRang
	 * @property {object} raenge
	 * @property {object} medals
	 * @property {object} menu
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.BuildingController.AjaxViewModel
	 * @property {number} col
	 * @property {number} field
	 * @property {boolean} noJsonSupport
	 * @property {ds.viewmodel.bases.Building.BuildingUiViewModel} buildingUI
	 * @property {ds.viewmodel.modules.viewmodels.GebaeudeAufBasisViewModel} building
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.BuildingController.DemoViewModel
	 * @property {number} col
	 * @property {number} field
	 * @property {object} demoCargo
	 * @property {boolean} success
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.MainController.Status
	 * @property {boolean} pm
	 * @property {boolean} comNet
	 * @property {string} version
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.MapController.MapViewModel
	 * @property {ds.viewmodel.modules.MapController.MapViewModel.SystemViewModel} system
	 * @property {ds.viewmodel.modules.MapController.MapViewModel.SizeViewModel} size
	 * @property {object} locations
	 **/
	/**
	 * @namespace ds.viewmodel.framework
	 **/
	/**
	 * @typedef {object} ds.viewmodel.framework.ViewMessage
	 * @property {ds.viewmodel.framework.ViewMessage.ViewMessageDetails} message
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.NpcController.ShopMenuViewModel
	 * @property {object} transporter
	 * @property {object} menu
	 **/
	/**
	 * @namespace ds.viewmodel.modules.SearchController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.SearchController.SearchViewModel
	 * @property {object} bases
	 * @property {object} ships
	 * @property {object} users
	 * @property {boolean} maxObjects
	 **/
	/**
	 * @namespace ds.viewmodel.modules.admin
	 **/
	/**
	 * @namespace ds.viewmodel.modules.admin.editoren
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.admin.editoren.JqGridTableDataViewModel
	 * @property {number} page
	 * @property {number} total
	 * @property {number} records
	 * @property {object} rows
	 **/
	/**
	 * @namespace ds.viewmodel.modules.TechListeController
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.TechListeController.TechListeViewModel
	 * @property {object} auswaehlbareRassen
	 * @property {string} rassenName
	 * @property {object} erforscht
	 * @property {object} erforschbar
	 * @property {object} nichtErforscht
	 * @property {object} unsichtbar
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.MapController.SystemauswahlViewModel
	 * @property {number} system
	 * @property {boolean} adminSichtVerfuegbar
	 * @property {boolean} systemkarteEditierbar
	 * @property {object} systeme
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.OptionsController.GeneriereSchiffsNamenBeispieleViewModel
	 **/
	/**
	 * @namespace ds.viewmodel.modules.viewmodels
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.viewmodels.UserViewModel
	 * @property {number} race
	 * @property {number} id
	 * @property {string} name
	 * @property {string} plainname
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.admin.editoren.JqGridViewModel
	 * @property {string} url
	 * @property {string} datatype
	 * @property {string} mtype
	 * @property {object} colNames
	 * @property {object} colModel
	 * @property {string} pager
	 * @property {number} rowNum
	 * @property {object} rowList
	 * @property {string} sortname
	 * @property {string} sortorder
	 * @property {boolean} viewrecords
	 * @property {boolean} autoencode
	 * @property {boolean} gridview
	 * @property {string} caption
	 * @property {string} height
	 * @property {boolean} autowidth
	 * @property {boolean} shrinkToFit
	 * @property {boolean} forceFit
	 **/
	/**
	 * @namespace ds.viewmodel.framework.ViewMessage
	 **/
	/**
	 * @typedef {object} ds.viewmodel.framework.ViewMessage.ViewMessageDetails
	 * @property {string} description
	 * @property {string} type
	 * @property {boolean} redirect
	 * @property {string} cls
	 **/
	/**
	 * @namespace ds.viewmodel.modules.MapController.SectorViewModel
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.MapController.SectorViewModel.NebelViewModel
	 * @property {number} type
	 * @property {string} image
	 **/
	/**
	 * @namespace ds.viewmodel.modules.BaseController.AjaxViewModel
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.BaseController.AjaxViewModel.BaseViewModel
	 * @property {number} id
	 * @property {string} name
	 * @property {number} x
	 * @property {number} y
	 * @property {number} system
	 * @property {boolean} feeding
	 * @property {number} width
	 * @property {boolean} loading
	 * @property {number} cargoBilanz
	 * @property {number} cargoFrei
	 * @property {number} energyProduced
	 * @property {number} energy
	 * @property {number} bewohner
	 * @property {number} arbeiter
	 * @property {number} arbeiterErforderlich
	 * @property {number} wohnraum
	 * @property {object} cargo
	 * @property {object} einheiten
	 * @property {ds.viewmodel.modules.BaseController.AjaxViewModel.CoreViewModel} core
	 **/
	/**
	 * @namespace ds.viewmodel.modules.ImpObjectsController.JsonViewModel
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.ImpObjectsController.JsonViewModel.SystemViewModel
	 * @property {string} name
	 * @property {number} id
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.admin.editoren.EntitySelectionViewModel
	 * @property {boolean} allowSelection
	 * @property {boolean} allowAdd
	 * @property {object} input
	 **/
	/**
	 * @namespace ds.viewmodel.bases
	 **/
	/**
	 * @namespace ds.viewmodel.bases.Building
	 **/
	/**
	 * @typedef {object} ds.viewmodel.bases.Building.BuildingUiViewModel
	 * @property {ds.viewmodel.bases.Building.BuildingUiViewModel.CPViewModel} consumes
	 * @property {ds.viewmodel.bases.Building.BuildingUiViewModel.CPViewModel} produces
	 **/
	/**
	 * @namespace ds.viewmodel.modules.MapController.MapViewModel
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.MapController.MapViewModel.SystemViewModel
	 * @property {number} id
	 * @property {number} width
	 * @property {number} height
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.viewmodels.GebaeudeAufBasisViewModel
	 * @property {number} id
	 * @property {string} name
	 * @property {string} picture
	 * @property {boolean} active
	 * @property {boolean} deakable
	 * @property {boolean} kommandozentrale
	 * @property {string} type
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.MapController.MapViewModel.SizeViewModel
	 * @property {number} minx
	 * @property {number} miny
	 * @property {number} maxx
	 * @property {number} maxy
	 **/
	/**
	 * @namespace ds.viewmodel.modules.stats.AjaxStatistic.DataViewModel
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.stats.AjaxStatistic.DataViewModel.KeyViewModel
	 * @property {number} id
	 * @property {string} name
	 **/
	/**
	 * @typedef {object} ds.viewmodel.modules.BaseController.AjaxViewModel.CoreViewModel
	 * @property {number} id
	 * @property {string} name
	 * @property {boolean} active
	 **/
	/**
	 * @namespace ds.viewmodel.bases.Building.BuildingUiViewModel
	 **/
	/**
	 * @typedef {object} ds.viewmodel.bases.Building.BuildingUiViewModel.CPViewModel
	 * @property {object} cargo
	 * @property {ds.viewmodel.bases.Building.BuildingUiViewModel.EnergyViewModel} energy
	 **/
	/**
	 * @typedef {object} ds.viewmodel.bases.Building.BuildingUiViewModel.EnergyViewModel
	 * @property {number} count
	 **/
})();
