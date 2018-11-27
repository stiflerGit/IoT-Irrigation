
function Position(lat, lng) {
	this.lat = lat;
	this.lng = lng;
}


function Mote(name) {
	this.nome = name;
	this.tipo = "";
	this.position = new Position(0.0, 0.0);
	this.battery = 0.0;
	this.temperature = 0.0;
	this.humidity = 0.0;
	this.irrigation= 0.0;
	this.updated = {tipo: false, irrigation: false};
}


function findMote(motes, name) {
	
	for (var i = 0; i < motes.length; i++) {
		if (motes[i].nome == name) {
			return i;
		}
	}
	return -1;
}


function updateMote(mote, res, val) {
	if (res == "type") {
		mote.tipo = val;
		mote.updated.tipo = true;
	}
	else if (res == "irrigation") {
		mote.irrigation = val;
		mote.updated.irrigation = true;
	}
	else if (res == "lat")
		mote.position.lat = val;
	else if (res == "lng")
		mote.position.lng = val;
	else if (res == "battery")
		mote.battery = val;
	else if (res == "temperature")
		mote.temperature = val;
	else if (res == "humidity")
		mote.humidity = val;
}


function selectMote(current, mote) {
	current = mote;
}