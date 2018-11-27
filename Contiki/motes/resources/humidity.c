#include <stdlib.h>
#include <string.h>
#include <math.h>

#include "contiki.h"

#include "rest-engine.h"


extern int hours;

static float humidity = 25;


static void periodic_handler();
static void get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);


PERIODIC_RESOURCE(resource_humidity, "title=\"humidity\";rt=\"Text\";obs", get_handler, NULL, NULL, NULL, 15*CLOCK_SECOND, periodic_handler);


static void get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	char message[20];
	int length;

	char *sign	= (humidity < 0) ? "-" : "";
	float val	= (humidity < 0) ? -humidity : humidity;

	int int1	= val;					// Get the integer.
	float frac	= val - int1;			// Get fraction.
	int int2	= frac * 100;			// Turn into integer.

	// Print as parts, note that you need 0-padding for fractional bit.
	sprintf (message, "{'humidity':'%s%d.%d'}", sign, int1, int2);
	
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_response_payload(response, buffer, length);
}


static void periodic_handler() {

	if (hours < 12)
		humidity += 0.5;
	else
		humidity -= 0.5;

	REST.notify_subscribers(&resource_humidity);
}


