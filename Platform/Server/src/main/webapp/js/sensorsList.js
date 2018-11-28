/*
window.onbeforeunload = function() {
    $websocket.onclose = function () {}; // disable onclose handler first
    $websocket.close();
};
*/

angular.module('Application', ['ngWebSocket'])

.controller("Controller", function($scope, $websocket) {

    $scope.motes = [];

    $scope.currentMote = null;

    $scope.websocket = $websocket('ws://127.0.0.1:8000/motes/');


    $scope.websocket.onMessage(function(message) {

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
                            console.log(key + ": " + object["data"][key]);
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


    $scope.websocket.onClose(function(){
        $scope.websocket.close();
    });


    $scope.updateActuator = function(res, val) {
        var mote = new Mote($scope.currentMote.nome);
        if (Array.isArray(res)) {
            for (var i = 0; i < res.length; i++)
                updateMote(mote, res[i], val[i]);
        } else {
            updateMote(mote, res, val);
        }

        send(mote, $scope.websocket);
        //$scope.currentMote = null;
    }


    $scope.selectMote = function(mote) {
        $scope.currentMote = mote;
    }

});