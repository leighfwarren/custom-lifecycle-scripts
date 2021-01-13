atex.onecms.register('ng-directive', 'pScriptList', [], function () {
    return ['ContentService', "$q", function (ContentService, $q) {
        return {
            replace: false,
            restrict: 'AE',

            scope: {
                'config': '=',
                'baseUrl': '@',
                'domainObject': '=',
                'domainObjects': '=',
                'widgetId': '@',
                'mode': '='
            },

            templateUrl: atex.onecms.baseUrl + '/template.html',

            controller: function ($scope) {
                $scope.domainObject = $scope.domainObject || $scope.domainObjects['data'];
                $scope.data = $scope.domainObject.getData();
                $scope.editable = $scope.config.editable !== false;

                let templateInfo = {
                    'id': 'atex.onecms.Template-com.atex.onecms.scripting.ScriptEditor'
                };

                ContentService.getContent(templateInfo).then(function (template) {
                    $scope.template = JSON.parse(template.data.aspects.contentData.data.data);
                });

                $scope.scripts = {};
                $scope.data.forEach(function (externalId) {
                    let transferInfo = {
                        'id': externalId
                    };

                    ContentService.getContent(transferInfo).then(function (obj) {
                        let conf = obj.data.aspects.contentData.data;
                        $scope.scripts[externalId] = {
                            "id": conf.id,
                            "name": conf.name,
                            "event": conf.event,
                            "scriptType": conf.scriptType,
                            "description": conf.description,
                            "script": conf.script,
                            "externalId": externalId
                        };
                    });
                });
            },

            link: function (scope) {
                scope.$watch('data', function(newValue, oldValue) {
                    if (newValue !== oldValue) {
                        scope.domainObject.setData(newValue);
                        scope.domainObject.changed();
                    }
                });

                scope.removeOpenScript = function() {
                    if (!scope.openScript) {
                        return;
                    }
                    scope.remove(`externalid/${scope.openScript.data.aspects.contentData.data.id}`);
                    scope.closeScript();
                };

                scope.remove = function (externalId) {
                    delete scope.scripts[externalId];
                    let dataIndex = scope.data.indexOf(externalId);
                    if (dataIndex !== -1) {
                        scope.data.splice(dataIndex, 1);
                    }

                    // If deleted script is the one being edited, close it.
                    if (scope.openScript && (`externalId/${scope.openScript.data.aspects.contentData.data.id}` === externalId)) {
                        scope.closeScript();
                    }

                    let removeRequest = {
                        id: externalId
                    };
                    ContentService.deleteContent(removeRequest).catch(function(e) {
                        console.error('couldn\'t remove content: ', e);
                    });

                    scope.domainObject.setData(scope.data);
                    scope.domainObject.changed();
                };

                scope.newScript = function () {
                    scope.article = atex.onecms.Content();

                    scope.article.data.aspects['p.InsertionInfo'] = {
                        data: {
                            _type: 'p.InsertionInfo',
                            securityParentId: 'externalid/dam.system.d'
                        }
                    };

                    scope.article.template = scope.template;
                    scope.editingScript = true;
                };

                scope.edit = function (externalId) {
                    scope.editingScript = false;
                    scope.getScriptConfiguration(externalId).then(function (script) {
                        scope.article = script;
                        scope.article.template = scope.template;
                        scope.editingScript = true;
                        scope.openScript = {
                            aspects: script.data.aspects,
                            operations: []
                        };
                    });
                };

                scope.domainChangeFinalizer = scope.domainObject.on('onecms:changed', function (event, modifierId) {
                    if (modifierId !== scope.widgetId) {
                        scope.data = scope.domainObject.getData();
                    }
                });

                scope.$on('$destroy', function () {
                    if (typeof scope.domainChangeFinalizer !== 'undefined') {
                        scope.domainChangeFinalizer();
                    }
                });

                scope.getScriptConfiguration = function (externalId) {
                    return $q(function (resolve) {
                        const transferInfo = {
                            'id': externalId
                        };

                        ContentService.getContent(transferInfo).then(function (configuration) {
                            resolve(configuration);
                        });
                    });
                };

                scope.updateScriptConfiguration = function (configuration) {
                    return $q(function (resolve) {
                        const updateInfo = {
                            id: configuration.id,
                            data: configuration.data,
                            revision: configuration.revision
                        };

                        ContentService.updateContent(updateInfo).then(function () {
                            resolve(true);
                        });
                    });
                };

                scope.initCallback = function () {
                };

                scope.changeStartCallback = function () {
                };

                scope.hideSpinner = function () {
                };

                scope.editCallback = function (data) {
                };


                scope.changeEndCallback = function (obj) {
                    scope.openScript = obj;
                };

                /**
                 * Persist a script to storage either by creating it if it doesn't
                 * exist, or by updating the current version of the document.
                 */
                scope.persistScript = function (script) {
                    if (!script) {
                        return;
                    }
                    if (script.aspects.contentData.data.id === '') {
                        scope.save(script);
                    } else {
                        let externalId = script.aspects['atex.Aliases'].data.aliases.externalId;
                        scope.update(externalId, script);
                    }
                };

                scope.closeScript = function() {
                    delete scope.openScript;
                    scope.editingScript = false;
                };

                /**
                 * Get a script name in a format safe for an ID, by making lowercase and
                 * replacing whitespace with hyphens.
                 * @param name The name to format, e.g. 'My Script'
                 * @returns {string} The formatted name e.g. my-script
                 */
                function formatScriptName(name) {
                    return name.trim().toLowerCase().replaceAll(' ', '-');
                }

                /**
                 * Create a new script in storage.
                 * @param script The script to store.
                 */
                scope.save = function (script) {
                    let scriptName = script.aspects.contentData.data.name;
                    let formattedName = formatScriptName(scriptName);
                    let scriptId = `com.atex.script.${formattedName}`;

                    let scriptContent = {...script};
                    scriptContent.aspects.contentData.data.inputTemplate = "p.LifecycleScript";
                    scriptContent.aspects.contentData.data.objectType = "LifecycleScript";
                    scriptContent.aspects.contentData.data._type = "com.atex.onecms.scripting.LifecycleScript";
                    scriptContent.aspects.contentData.data.id = formattedName;

                    let alias = {
                        "_type": "com.atex.onecms.content.SetAliasOperation",
                        "namespace": "externalId",
                        "alias": scriptId
                    };
                    scriptContent.operations.push(alias);

                    let transferInfo = {
                        data: scriptContent
                    };

                    ContentService.createContent(transferInfo).then(function () {
                        scope.data.push(scriptId);

                        let conf = scriptContent.aspects.contentData.data;
                        const externalId = `externalid/${scriptId}`;
                        scope.scripts[externalId] = {
                            "id": conf.id,
                            "name": conf.name,
                            "event": conf.event,
                            "scriptType": conf.scriptType,
                            "description": conf.description,
                            "script": conf.script,
                            "_type": "com.atex.onecms.scripting.LifecycleScript",
                            "externalId": externalId
                        };

                        scope.article = {};

                        scope.domainObject.setData(scope.data);
                        scope.domainObject.changed();
                    });
                };

                /**
                 * Update an existing script.
                 * @param id The external ID of the script.
                 * @param obj The script to update the current data with.
                 */
                scope.update = function (id, obj) {
                    scope.getScriptConfiguration(id).then(function (script) {
                        script.data.aspects.contentData.data = obj.aspects.contentData.data;
                        const updatedInfo = {
                            "id": script.data.aspects.contentData.data.id,
                            "name": script.data.aspects.contentData.data.name,
                            "event": script.data.aspects.contentData.data.event,
                            "scriptType": script.data.aspects.contentData.data.scriptType,
                            "description": script.data.aspects.contentData.data.description,
                            "script": script.data.aspects.contentData.data.script,
                            "externalId": `externalid/${id}`
                        };

                        scope.updateScriptConfiguration(script).then(function () {
                            scope.scripts[`externalid/${id}`] = updatedInfo;
                            scope.article = {};
                        });
                    });
                };
            }
        };
    }];
});