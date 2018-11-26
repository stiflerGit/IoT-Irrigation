
#include <stdlib.h>
#include <string.h>

#include "contiki.h"

#include "rest-engine.h"

#include "node-id.h"		// To get node id


static char type[16] = "Banane";


static void get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void post_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);


RESOURCE(resource_type, "title=\"type\";rt=\"Text\"", get_handler, post_handler, NULL, NULL);


static void get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	char message[REST_MAX_CHUNK_SIZE];
	int length;

	sprintf(message, "{'id':'%d','type':'%s'}", node_id, type);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_response_payload(response, buffer, length);
}


static void post_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {

	const char *val = NULL;
	int length;

	length = REST.get_post_variable(request, "type", &val);
	
	if(length > 0) {
		strcpy(type, val);
		REST.set_response_status(response, REST.status.CREATED);
	} else {
		REST.set_response_status(response, REST.status.BAD_REQUEST);
	}
}
