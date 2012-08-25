'use strict';

/* jasmine specs for controllers go here */
describe('NPC controllers', function() {
	var dsMock = {
		__expect : null,
		__repsonse : null,
		__success : null,
		instance : function() {
			var self = this;
			return function(params, options) {
				console.log("call: "+params+" and expect "+self.__expect);
				expect(self.__expect).toNotBe(null);
				expect(params).toEqual(self.__expect);
				self.__expect = null;
				return {
					success : function(callback) {
						self.__success = {callback:callback, response:self.__response};
					}
				};
			};
		},
		flush : function() {
			if( this.__success != null ) {
				this.__success.callback(this.__success.response);
			}
		},
		expect : function(params) {
			console.log("expect: "+params);
			this.__expect = params;
			this.__response = null;
			
			var self = this;
			return {
				respond : function(response) {
					self.__response = response;
				}
			};
		}
	}

	beforeEach(function(){
		this.addMatchers({
			toEqualData: function(expected) {
				return angular.equals(this.actual, expected);
			}
		});
	});


	beforeEach(module('ds.service.ds'));
	beforeEach(module('ds.npc'));

	angular.module('ds.service.ds').factory('ds', function() {
		return dsMock.instance();
	});

	describe('NpcLpController', function(){
		var scope, ctrl;
		
		var userResponse = {
				menu: {head:false, shop:false},
				user : {name:'testuser', id:9999, race:1, plainname:'testuser'},
				rang : 'Testrang',
				lpBeiNpc : 42,
				lpListe : [
					{
						id:1, 
						grund:'weil halt', 
						anmerkung:'foobar', 
						anzahlPunkte:4321, 
						zeitpunkt:12345678, 
						verliehenDurch:{name:'testnpc',id:-9999,race:2,plainname:'testnpc'}
					}
				]
			};
		
		function expectUserRequest() {
			dsMock.expect({module:'npc', action:'lpMenu', edituser:9999})
				.respond(userResponse);
		}

		beforeEach(inject(function($rootScope, $controller) {
			dsMock.expect({module:'npc', action:'lpMenu'})
				.respond({
					menu: {head:false, shop:false}
				});

			scope = $rootScope.$new();
		}));


		it('check standard xhr-Antwort bei Abfragen ohne User', function() {
			inject(function($controller) {
				ctrl = $controller('NpcLpController', {$scope: scope});
			});
			
			expect(scope.edituser).toEqual(null);
			dsMock.flush();

			expect(scope.edituser).toEqual(null);
			expect(scope.edituserPresent).toEqual(false);
			expect(scope.lpListe).toEqual(null);
			expect(scope.rang).toEqual(null);
			expect(scope.lpBeiNpc).toEqual(null);
			expect(scope.menu.head).toEqual(false);
			expect(scope.menu.head).toEqual(false);
		});
		
		it('check standard xhr-Antwort bei Abfragen mit User', function() {
			expectUserRequest();
			scope.editUserId = 9999;
			
			inject(function($controller) {
				ctrl = $controller('NpcLpController', {$scope: scope});
			});
			
			dsMock.flush();
			
			expect(scope.edituser).toEqual(userResponse.user);
			expect(scope.edituserPresent).toEqual(true);
			expect(scope.lpListe).toEqual(userResponse.lpListe);
			expect(scope.rang).toEqual('Testrang');
			expect(scope.lpBeiNpc).toEqual(42);
			expect(scope.menu.head).toEqual(false);
			expect(scope.menu.head).toEqual(false);
		});
		
		it('check lp loeschen', function() {
			expectUserRequest();
			scope.editUserId = 9999;
			
			inject(function($controller) {
				ctrl = $controller('NpcLpController', {$scope: scope});
			});
			
			dsMock.flush();
			
			dsMock.expect({module:'npc', action:'deleteLp', 'edituser':9999, 'lp':scope.lpListe[0].id})
				.respond({message:{type:'success'}});
			
			scope.lpLoeschen(scope.lpListe[0]);
			
			expectUserRequest();
			
			dsMock.flush(); // LP loeschen
			
			dsMock.flush(); // refresh
		});
	});
});
