
var motes = sim.getMotes();
log.log("Number of motes = " + motes.length + "\n");

while (true) {
	YIELD();
	if (msg.equals("gps")) {
		var x = mote.getInterfaces().getPosition().getXCoordinate();
		var y = mote.getInterfaces().getPosition().getYCoordinate();
		write(mote, x + " " + y + "\r");
	}
}