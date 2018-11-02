'use strict';

angular.module('ds.service.ds', [])
.factory('ds', ['$http', 'dsMsg',function($http, dsMsg){
	return function(params, options) {
		if( typeof options === 'undefined' ) {
			options = {};
		}
		if( typeof options.processMessages === 'undefined' ) {
			options.processMessages = true;
		}
		var base = DS.getUrl()+"?FORMAT=JSON";
		angular.forEach(params, function(value,key) {
			if( typeof value !== 'undefined' && value != null) {
				base += "&"+key+"="+value;
			}
		});
		var async = $http.get(base);
		
		if( options.processMessages ) {
			async.success(function(result) {
				dsMsg(result);
			});
		}
		
		return async;
	};
}])
.factory('dsMsg', ['$location', function($location) {
	return function(messageContainer) {
		if( typeof messageContainer.errors !== 'undefined' && 
				messageContainer.errors.length > 0 ) {
			angular.forEach(messageContainer.errors, function(error) {
				toastr.error(error.description);
			});
			return false;
		}
		else if( typeof messageContainer.message !== 'undefined' &&
				typeof messageContainer.message.type !== 'undefined' ) {
			var msg = messageContainer.message;
			
			if( msg.type === 'success' ) {
				toastr.success(messageContainer.message.description);
			}
			else if( msg.type === 'error' ) {
				toastr.error(messageContainer.message.description);
				if( typeof msg.redirect !== 'undefined' && msg.redirect ) {
					if( parent ) {
						parent.location.href=DS.getUrl();
					}
					else {
						location.href=DS.getUrl();
					}
				}
				return false;
			}
			else if( msg.type === 'success' ) {
				toastr.warning(messageContainer.message.description);
			}
		}
		return true;
	}
}]);