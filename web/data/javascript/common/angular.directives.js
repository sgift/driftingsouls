'use strict';

angular.module('jquery-ui.directives', [])
.directive('jqueryUiAutocomplete', function() {
	return function(scope, element, attrs) {
		var elemName = attrs.name;
		var expression = attrs.jqueryUiAutocomplete;
		scope.$watch(expression, function(value) {
			var items = this.$eval(expression);
			element
				.autocomplete({
					source : items,
					html : false
				})
				.blur(function() {
					scope[elemName] = element.val();
				});
		});
	};
});

angular.module('ds.directives', [])
.directive('dsAutocomplete', function() {
	return function(scope, element, attrs) {
		var elemName = attrs.name;
		var items = null;
		if( attrs.dsAutocomplete == "users" ) {
			items = DsAutoComplete.users;
		}
		
		element
			.autocomplete({
				source : items,
				html : true
			})
			.blur(function() {
				scope[elemName] = element.val();
			});
	};
});
