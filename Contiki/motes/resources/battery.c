
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "rest-engine.h"


static int mote_battery = 100;


static void get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void post_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void event_handler();


EVENT_RESOURCE(resource_battery, "title=\"battery\";rt=\"Text\";obs", get_handler, post_handler, NULL, NULL, event_handler);


static void get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	char message[20];
	int length;

	sprintf(message, "{'battery':'%d'}", mote_battery);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_response_payload(response, buffer, length);
}


static void post_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	int new_value, length;
	const char *value = NULL;

	length = REST.get_post_variable(request, "value", &value);

	if (length > 0) {
		new_value = atoi(value);
		mote_battery = new_value;
		REST.set_response_status(response, REST.status.CREATED);
	} else {
		REST.set_response_status(response, REST.status.BAD_REQUEST);
	}
}


static void event_handler() {
	mote_battery--;
	REST.notify_subscribers(&resource_battery);
}