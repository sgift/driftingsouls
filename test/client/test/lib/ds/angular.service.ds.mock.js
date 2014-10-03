var dsMock = {
	__expect : null,
	__repsonse : null,
	__success : null,
	instance : function() {
		var self = this;
		return function(params, options) {
			console.log("call: "+params+" and expect "+self.__expect);
			expect(self.__expect).not.toBe(null);
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
};