'use strict';
atex.onecms.register('ng-directive', 'pCodeMirrorEditor', ['codemirror', 'javascript'], function(CodeMirror) {
    return ['$window', '$sce', '$timeout', function($window, $sce, $timeout) {
        let editorConfig = {};
        return {
            replace: false,
            restrict: 'AE',

            scope: {
                'config': '=',
                'baseUrl': '@',
                'domainObject': '=',
                'widgetId': '@',
                'mode': '@'
            },

            templateUrl: atex.onecms.baseUrl + '/template.html',

            controller: function($scope) {
                editorConfig = $scope.config.codeMirror;
                $scope.data = $scope.domainObject.getData();
            },

            link: function(scope, element) {
                let editorConfig = scope.config.codeMirror;
                $timeout(function() {
                    let editor = CodeMirror.fromTextArea(element.find('textarea')[0], editorConfig);
                    editor.on('change', function(codemirrorInstance) {
                        if (codemirrorInstance.getValue() !== scope.domainObject.getData()) {
                            scope.$apply(function() {
                                scope.domainObject.setData(codemirrorInstance.getValue());
                                scope.domainObject.changed();
                            });
                        }
                    });
                }, 100);
            },
        };
    }];
});