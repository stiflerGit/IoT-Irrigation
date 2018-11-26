/*
    Server to Client

 * update one or more fields of a mote
    {   "action" : "update" , 
        "motes": [mote_type]  
    }
===================================================
    mote_type definition:
    {"id": "moteid", "battery": batterylvl, ... }
    moteid is mandatory, others value are optional
===================================================
 * add one or more motes to the list of motes
    {   "action":"newMotes", 
        "motes": [mote_type]
    }
===================================================
* remove one or more motes from the list of motes
    {   "action":"rmMotes",
        "motes": ["moteid"]
    }
===================================================
* list of actually presents motes (in response
    to a get request)

    {   "action":"list",
        "motes": [mote_type]
    }
*/

/*
    Client to Server

 * request the list of motes
    {"action":"getList"}
===================================================
 * change some parater of a given mote
    {   "action":"update"
        "mote": mote_type} 
*/

var app = angular.module("myApp", ['ngWebSocket']);

app.constant("baseUrl", "ws://localhost:8000/motes");

app.controller("myCtrl", function ($scope, $websocket, baseUrl) {

	$scope.motes = [];

	$scope.currentMote = null;

	var ws = $websocket(baseUrl);

	ws.onMessage(function(message) {
        var data = angular.fromJson(message.data)
        var action = data["action"];
        var motesList = [];
        angular.forEach(data["motes"], function(mote){
            motesList.push(JSON.parse(mote));
        });
        console.log(data);
        if (action == "newMotes"){
            // add new motes body
            for (var i = 0; i <= message.motes.length; i++) {
                // $scope.motes.push(JSON.parse(data["oomotes[i]));
            }
        }else if (action == "rmMotes") {
            // remove motes body
            var arr = JSON.parse(message.data.motes);
            if (!arr.isArray()){
                console.log("ERROR MOTES NOT PRESENT IN RMOTES");
                console.log(arr);
            }
            for(moteid in arr) {
                var index = $scope.motes.indexOf(moteid);
                $scope.motes.splice(index, 1);
            }
        }else if (action == "list") {
            // list motes body
            $scope.motes = motesList;
        }else if (action == "update") {
            // update motes body
            angular.forEach(motesList, function(mote) {
                var toModify = getMote(mote);
                if (toModify != null){
                    toModify = mote;
                }
            });
        }   
    });

	var get = function() {
          	ws.send(angular.toJson({ action: 'get' }));
    };

    $scope.isMoteSelected = function () {
        if($scope.currentMote != null)
            return true
    };

    $scope.updateIrrigation = function(irrigationValue) {
        if(irrigationValue => 0 && irrigationValue <= 10) {
            var moteUpd = angular.copy($scope.currentMote);
            moteUpd["irrigation"] = $scope.irrigationValue;
            console.log(JSON.stringify({action:"update", mote: moteUpd}))
            ws.send(JSON.stringify({action:"update", mote: moteUpd}));
        }
    };

    $scope.setCurrentMote = function(mote) {
        $scope.currentMote = mote;
    }

    function getMote(moteid){
        angular.forEach($scope.motes, function(mote) {
            if(mote["id"] == moteid)
                return mote;
        });
        return null;
    };

    get();

});