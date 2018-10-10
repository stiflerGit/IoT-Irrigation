#include "contiki.h"
#include <stdlib.h>
#include <string.h>
#include "rest-engine.h"
#include "node-id.h"


#define DEBUG 1
#if DEBUG
#define PRINTF(...) printf(__VA_ARGS__)
#define PRINT6ADDR(addr) PRINTF("[%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x]", ((uint8_t *)addr)[0], ((uint8_t *)addr)[1], ((uint8_t *)addr)[2], ((uint8_t *)addr)[3], ((uint8_t *)addr)[4], ((uint8_t *)addr)[5], ((uint8_t *)addr)[6], ((uint8_t *)addr)[7], ((uint8_t *)addr)[8], ((uint8_t *)addr)[9], ((uint8_t *)addr)[10], ((uint8_t *)addr)[11], ((uint8_t *)addr)[12], ((uint8_t *)addr)[13], ((uint8_t *)addr)[14], ((uint8_t *)addr)[15])
#define PRINTLLADDR(lladdr) PRINTF("[%02x:%02x:%02x:%02x:%02x:%02x]",(lladdr)->addr[0], (lladdr)->addr[1], (lladdr)->addr[2], (lladdr)->addr[3],(lladdr)->addr[4], (lladdr)->addr[5])
#else
#define PRINTF(...)
#define PRINT6ADDR(addr)
#define PRINTLLADDR(addr)
#endif

extern float mote_lat;
extern float mote_lng;

static void position_get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void position_put_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(resource_position, "title=\"position\";rt=\"Text\"", position_get_handler, NULL, position_put_handler, NULL);

static void position_get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	/* Populat the buffer with the response payload*/
	char message[DIM_BUFFER];
	int length;
	float tmp_lat, tmp_lng;
	unsigned int mb_lat, mb_lng;

	tmp_lat = (float)((float)mote_lat - (int)mote_lat);
	mb_lat = tmp_lat * 10000;
	tmp_lng = (float)((float)mote_lng - (int)mote_lng);
	mb_lng = tmp_lng * 10000;

	sprintf(message, "{'lat':'%u.%u','lng':'%u.%u'}", (unsigned int)mote_lat, (unsigned int)mb_lat, (unsigned int)mote_lng, (unsigned int)mb_lng);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_header_etag(response, (uint8_t *) &length, 1);
	REST.set_response_payload(response, buffer, length);
}

static void position_put_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	int len;
	float new_value;
	const char *val = NULL;
  
	len = REST.get_post_variable(request, "lat", &val);
     
	if (len > 0) {
		new_value = (float)(atof(val));
		mote_lat = new_value;
		REST.set_response_status(response, REST.status.CREATED);
	} else {
		len = REST.get_post_variable(request, "lng", &val);
		if (len > 0) {
			new_value = (float)(atof(val));
			mote_lng = new_value;
			REST.set_response_status(response, REST.status.CREATED);
		} else {
			REST.set_response_status(response, REST.status.BAD_REQUEST);
		}
	}
}
