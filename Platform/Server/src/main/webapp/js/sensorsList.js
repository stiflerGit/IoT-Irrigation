angular.module('Application', ['ngWebSocket'])

.controller("Controller", function($scope, $websocket) {

    $scope.motes = [];

    $scope.currentMote = null;

    $scope.hideManagement = true;

    var ws = $websocket('ws://127.0.0.1:8000/');


    ws.onMessage(function(message) {

        var object = JSON.parse(message.data);

        if (object["action"] == "POST") {
            $scope.motes.push(new Mote(object["data"]["name"]));
            for (var key in object["data"]) {
                if (key != "mutex" && key != "updated") {
                    if (object["data"].hasOwnProperty(key)) {
                        if (key == "position") {
                            for (var key2 in object["data"][key]) {
                                if (object["data"][key].hasOwnProperty(key2))
                                    updateMote($scope.motes[$scope.motes.length - 1], key2, object["data"][key][key2]);
                            }
                        }
                        updateMote($scope.motes[$scope.motes.length - 1], key, object["data"][key]);
                    }
                }
            }
        }

        else if (object["action"] == "PUT") {

            var idx = findMote($scope.motes, object["data"]["name"]);

            if (idx != -1) {
                for (var key in object["data"]["updated"]) {
                    if (object["data"]["updated"].hasOwnProperty(key)) {
                        if (object["data"]["updated"][key] == true) {
                            if (key == "position") {
                                for (var key2 in object["data"][key]) {
                                    if (object["data"][key].hasOwnProperty(key2))
                                        updateMote($scope.motes[idx], key2, object["data"][key][key2]);
                                }
                            }
                            updateMote($scope.motes[idx], key, object["data"][key]);
                        }
                    }
                }
            }
        }

    
        else if (object["action"] == "DELETE") {
            var idx = findMote($scope.motes, object["data"]["name"]);
            $scope.motes.splice(idx, 1);
            if ($scope.currentMote == $scope.motes[idx]) {
                $scope.currentMote = null;
                $scope.hideManagement = true;
            }
        }

    });


    ws.onClose(function(){
        ws.close();
    });


    function send(mote) {
        var obj = {action:"PUT",data:JSON.parse(JSON.stringify($scope.motes[i]))};
        ws.send(JSON.stringify(obj));

    }

    $scope.selectMote = function(mote) {
        $scope.currentMote = mote;
        $scope.hideManagement = false;
    }


    $scope.setType = function(t) {
        for (var i = 0; i < $scope.motes.length; i++) {
            if ($scope.motes[i].nome == $scope.currentMote.nome)
                break;
        }

        $scope.motes[i].tipo = t;
        $scope.motes[i].updated.tipo = true;
        var obj = {action:"PUT",data:JSON.parse(JSON.stringify($scope.motes[i]))};
        ws.send(JSON.stringify(obj));

        $scope.motes[i].updated.tipo = false;
        
        // $scope.currentMote = null;
        // $scope.hideManagement = true;
    }


    $scope.setIrrigation = function(t) {
        for (var i = 0; i < $scope.motes.length; i++) {
            if ($scope.motes[i].nome == $scope.currentMote.nome)
                break;
        }

        $scope.motes[i].irrigation = t;
        $scope.motes[i].updated.irrigation = true;

        var obj = {action:"PUT",data:JSON.parse(JSON.stringify($scope.motes[i]))};
        ws.send(JSON.stringify(obj));

        $scope.motes[i].updated.irrigation = false;
        
        // $scope.currentMote = null;
        // $scope.hideManagement = true;
    }


    $scope.setParams = function(t1, t2) {
        for (var i = 0; i < $scope.motes.length; i++) {
            if ($scope.motes[i].nome == $scope.currentMote.nome)
                break;
        }
        
        $scope.motes[i].tipo = t1;
        $scope.motes[i].updated.tipo = true;
        $scope.motes[i].irrigation = t2;
        $scope.motes[i].updated.irrigation = true;
        
        var obj = {action:"PUT",data:JSON.parse(JSON.stringify($scope.motes[i]))};
        ws.send(JSON.stringify(obj));

        $scope.motes[i].updated.irrigation = false;
        $scope.motes[i].updated.tipo = false;

        // $scope.currentMote = null;
        // $scope.hideManagement = true;

    }



});

