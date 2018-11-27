
#include <stdlib.h>
#include <string.h>

#include "contiki.h"

#include "rest-engine.h"

#include "node-id.h"

static void get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);


RESOURCE(resource_name, "title=\"name\";rt=\"Text\"", get_handler, NULL, NULL, NULL);


static void get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	char message[REST_MAX_CHUNK_SIZE];
	int length;

	sprintf(message, "{'name':'Mote_%d'}", node_id);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_response_payload(response, buffer, length);
}
