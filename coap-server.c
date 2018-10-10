#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "contiki-net.h"
#include "rest-engine.h"
#include "node-id.h"	// Necessary to get node_id


#define DEBUG 1
#if DEBUG
#include <stdio.h>
#define PRINTF(...) printf(__VA_ARGS__)
#define PRINT6ADDR(addr) PRINTF("[%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x]", ((uint8_t *)addr)[0], ((uint8_t *)addr)[1], ((uint8_t *)addr)[2], ((uint8_t *)addr)[3], ((uint8_t *)addr)[4], ((uint8_t *)addr)[5], ((uint8_t *)addr)[6], ((uint8_t *)addr)[7], ((uint8_t *)addr)[8], ((uint8_t *)addr)[9], ((uint8_t *)addr)[10], ((uint8_t *)addr)[11], ((uint8_t *)addr)[12], ((uint8_t *)addr)[13], ((uint8_t *)addr)[14], ((uint8_t *)addr)[15])
#define PRINTLLADDR(lladdr) PRINTF("[%02x:%02x:%02x:%02x:%02x:%02x]", (lladdr)->addr[0], (lladdr)->addr[1], (lladdr)->addr[2], (lladdr)->addr[3], (lladdr)->addr[4], (lladdr)->addr[5])
#else
#define PRINTF(...)
#define PRINT6ADDR(addr)
#define PRINTLLADDR(addr)
#endif

#define TYPE_DIM	10

extern resource_t resource_id, resource_type, resource_position;
extern resource_t resource_battery;
extern resource_t resource_temperature, resource_humidity, resource_soil_moisture;

/**
 * @brief Coordinate to locate the sensor
 * 
 * Each sensor is in a field. Such a struct can help to locate the sensor.
 */
struct coordinate {
	float latitude;
	float longitude;
};

static struct coordinate locations[] = { 
	{43.7210, 10.3898},
	{43.7178, 10.3827},
	{43.7229, 10.3965},
	{43.7093, 10.3985},
	{43.7165, 10.4022}
};	/**< A set of coordinates to simulate the sensors location  .*/


int	mote_id;		/**< Each sensor is identified by a unique ID. */
float	mote_lat, mote_lng;	/**< Latitude and Longitude of the sensor. */
float	mote_temperature;	/**< Last misured temperature. */
float	mote_humidity;		/**< Last misure humidity*/
float	mote_soil_moisture;	/**< Humidity of the soil(it depends on the kind of the soil).*/
int	mote_battery;		/**< Actual percentage of the battery. */
char	mote_type[TYPE_DIM];	/**< Type of field cultivation (i.e. Strawberry, Bananas, etc.). */

char type[] = "Banane";		/**< Type of cultivation, for simulation. */

unsigned int battery_tick = 0;	/**< Counter for battery consumption simulation. */

void init_mote() {
	int i, dim;
	dim = sizeof(type)/sizeof(char);
	memset(mote_type,'\0', TYPE_DIM);
	for(i=0; i<dim; i++)
		mote_type[i] = type[i];
	mote_id = node_id;
	mote_lat = locations[mote_id].latitude;
	mote_lng = locations[mote_id].longitude;
	mote_temperature = 22;
	mote_humidity = 13;
	mote_soil_moisture = 70;
	mote_battery = 100;
	mote_alarm = 0;
}

static struct ctimer ct;	/**< Counter. for callback timer*/

static void ctimer_callback(void *ptr) {
	// Mote battery consumption every 10 ticks
	if(mote_battery > 1) {
		battery_tick++;
	
		if(battery_tick == 10) {
			if(mote_battery > 50)
				mote_battery -= 2;
			else
				mote_battery -= 1;
			battery_tick = 0;
		
		}
	}
	// simulation of the cultivation field features
	if(mote_temperature > 18.5 && mote_humidity < 2.5)
		mote_soil_moisture -= 0.9;
	else if(mote_temperature < 18.5 && mote_humidity > 2.5)
		mote_soil_moisture -= 0.1;
	else if(mote_temperature > 18.5 && mote_humidity > 2.5)
		mote_soil_moisture -= 0.3;
	else if(mote_temperature < 18.5 && mote_humidity < 2.5)
		mote_soil_moisture -= 0.5;
	ctimer_restart(&ct);
}


PROCESS(server, "CoAP Server");
AUTOSTART_PROCESSES(&server);

PROCESS_THREAD(server, ev, data) {
	PROCESS_BEGIN();

	PROCESS_PAUSE();

	init_mote();
	rest_init_engine();

	rest_activate_resource(&resource_id, "id");
	rest_activate_resource(&resource_type, "type");
	rest_activate_resource(&resource_position, "position");
	rest_activate_resource(&resource_battery, "battery");
	rest_activate_resource(&resource_temperature, "temperature");
	rest_activate_resource(&resource_humidity, "humidity");
	rest_activate_resource(&resource_soil_moisture, "soil_moisture");
	rest_activate_resource(&resource_alarm, "alarm");

	ctimer_set(&ct, 60*10*CLOCK_SECOND, ctimer_callback, NULL);
	while(1) {
		PROCESS_WAIT_EVENT();
	}
	PROCESS_END();
}
