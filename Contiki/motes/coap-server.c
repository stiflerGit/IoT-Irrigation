
#include <stdio.h>
#include <stdlib.h>

#include "contiki.h"
#include "contiki-lib.h"
#include "contiki-net.h"
#include "rest-engine.h"

#include "dev/serial-line.h"
#include "dev/uart0.h"

extern resource_t resource_name;
extern resource_t resource_battery;
extern resource_t resource_gps;
extern resource_t resource_temperature;
extern resource_t resource_humidity;

extern resource_t resource_irrigation;
extern resource_t resource_type;

// Used to receive serial data (position)
extern void receive_serial(char *data);

int hours = 0;


PROCESS(coap_server, "CoAP Server");

AUTOSTART_PROCESSES(&coap_server);

PROCESS_THREAD(coap_server, ev, data) {

	static struct etimer battery_timer;
	static struct etimer gps_timer;

	PROCESS_BEGIN();

	uart0_init(BAUD2UBR(115200));
	uart0_set_input(serial_line_input_byte);
	serial_line_init();

	//PROCESS_PAUSE();

	rest_init_engine();

	rest_activate_resource(&resource_name, "sensor/name");
	rest_activate_resource(&resource_gps, "sensor/gps");
	rest_activate_resource(&resource_battery, "sensor/battery");
	rest_activate_resource(&resource_temperature, "sensor/temperature");
	rest_activate_resource(&resource_humidity, "sensor/humidity");
	rest_activate_resource(&resource_type, "actuator/type");
	rest_activate_resource(&resource_irrigation, "actuator/irrigation");

	etimer_set(&gps_timer, CLOCK_SECOND*30);
	etimer_set(&battery_timer, CLOCK_SECOND*25);


	while(1) {

		PROCESS_WAIT_EVENT();

		if (etimer_expired(&battery_timer)) {
			resource_battery.trigger();
			etimer_reset(&battery_timer);
		}
		if (etimer_expired(&gps_timer)) {
			printf("\ngps\n");
			PROCESS_WAIT_EVENT();
			if (ev == serial_line_event_message) {
				receive_serial((char *) data);
				etimer_reset(&gps_timer);
			}
		}
	}

	PROCESS_END();

}