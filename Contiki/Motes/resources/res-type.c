#include "contiki.h"
#include <stdlib.h>
#include <string.h>
#include "rest-engine.h"

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


#define TYPE_DIM	10


extern char mote_type[TYPE_DIM];

static void type_get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void type_put_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(resource_type, "title=\"type\";rt=\"Text\"", type_get_handler, NULL, type_put_handler, NULL);

static void type_get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	/* Populat the buffer with the response payload */
	char message[REST_MAX_CHUNK_SIZE];
	int length;

	sprintf(message, "{'type':'%s'}", mote_type);
  
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_header_etag(response, (uint8_t *) &length, 1);
	REST.set_response_payload(response, buffer, length);
}


static void type_put_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
  
	int len, i;
	const char *val = NULL;
	
	len = REST.get_post_variable(request, "value", &val);
	memset(mote_type,'\0', TYPE_DIM);
	
	if (len > 0) {
		PRINTF("new value");
		for(i=0;i<len;i++) {
			PRINTF("%c", val[i]);
			mote_type[i] = val[i];
		}
		REST.set_response_status(response, REST.status.CREATED);
	} else {
		REST.set_response_status(response, REST.status.BAD_REQUEST);
	}
}
