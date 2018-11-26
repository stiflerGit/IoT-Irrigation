
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "rest-engine.h"


static int position[2];
static int serial_data[2];

static void get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void event_handler();


void receive_serial(char *data);


EVENT_RESOURCE(resource_gps, "title=\"gps\";rt=\"Text\";obs", get_handler, NULL, NULL, NULL, event_handler);


static void get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	char message[20];
	int length;

	sprintf(message, "{'lat':'%d','lng':'%d'}", position[0], position[1]);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_response_payload(response, buffer, length);
}


static void event_handler() {
	position[0] = serial_data[0];
	position[1] = serial_data[1];
	REST.notify_subscribers(&resource_gps);
}


void receive_serial(char *data) {
	const char s[2] = " ";
	char *token;

	printf("Receive serial data\n");

	token = strtok(data, s);
	serial_data[0] = atoi(token);

	token = strtok(NULL, "");
	serial_data[1] = atoi(token);
		
	if (position[0] != serial_data[0] || position[1] != serial_data[1])
		resource_gps.trigger();
}
