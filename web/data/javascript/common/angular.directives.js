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
})
.directive('dsPopup', function(PopupService) {
	return {
		restrict : 'A',
		link : function postLink(scope, element, attrs) {
			var popupOptions = {
				title : attrs.dsPopupTitle
			};
			element.bind("click", function() {
				PopupService.load("data/cltemplates/"+attrs.dsPopup, scope, popupOptions);
			});
		}
	};
})
.factory('PopupService', function($http, $compile) {
	var popupService = {
		popupElement : null
	};

	popupService.getPopup = function(create) {
		if( popupService.popupElement == null && create) {
			popupService.popupElement = $('<div class="modal hide gfxbox"></div>');
			popupService.popupElement.appendTo('body');
		}

		return popupService.popupElement;
	}

	popupService.load = function(url, scope, options) {
		$http.get(url).success(
			function(data) {
				var popup = popupService.getPopup(true);
				popup.html(data);
				$compile(popup)(scope);
				$(popup).dialog(options);
			});
	}

	popupService.close = function() {
		var popup = popupService.getPopup(false);
		if( popup != null ) {
			$(popup).dialog('hide');
		}
	}

	return popupService;
});
