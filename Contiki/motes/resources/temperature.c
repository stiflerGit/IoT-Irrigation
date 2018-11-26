#include <stdlib.h>
#include <string.h>
#include <math.h>

#include "contiki.h"

#include "rest-engine.h"

//#define SAMPLING_TIME	50*CLOCK_SECOND

extern int hours;

static float temperature = 35;

static void periodic_handler();
static void get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);


PERIODIC_RESOURCE(resource_temperature, "title=\"temperature\";rt=\"Text\";obs", get_handler, NULL, NULL, NULL, 1000*CLOCK_SECOND, periodic_handler);


static void get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	char message[20];
	int length;

	char *sign	= (temperature < 0) ? "-" : "";
	float val	= (temperature < 0) ? -temperature : temperature;

	int int1	= val;						// Get the integer.
	float frac	= val - int1;			// Get fraction.
	int int2	= frac * 100;				// Turn into integer.

	// Print as parts, note that you need 0-padding for fractional bit.
	sprintf (message, "{'temperature':'%s%d.%d'}", sign, int1, int2);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_response_payload(response, buffer, length);
}


static void periodic_handler() {
	hours = (hours < 23) ? hours + 1 : 0;

	//float tmp = random(RAND_MAX);

	if (hours < 12)
		temperature -= 0.5;
	else
		temperature += 0.5;

	REST.notify_subscribers(&resource_temperature);
}


