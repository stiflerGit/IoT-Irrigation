#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "contiki.h"
#include "rest-engine.h"
//#include "normal_distribution.h"

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


#define SAMPLING_TIME	60*5*CLOCK_SECOND
#define MU	2.5
#define SIGMA	0.5

/* Input variable */
extern float mote_humidity;

/* Output at step k */
float humidity_k = 0; 
/* Output at step k-1 */
float humidity_k_1 = 0; 


static void humidity_periodic_handler();
static void humidity_get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

PERIODIC_RESOURCE(resource_humidity, "title=\"humidity\";rt=\"Text\"", humidity_get_handler, NULL, NULL, NULL, SAMPLING_TIME, humidity_periodic_handler);

//------------------------------------------------------------------------------------------------------------//
static float randn(float mu, float sigma);

static float randn(float mu, float sigma) {
	float U1, U2, W, mult;
	static float X1, X2;
	static int call = 0;
	
	if (call == 1) {
		call = !call;
		return (mu + sigma * (float) X2);
	}
 
	do {
		U1 = -1 + ((float) rand () / RAND_MAX) * 2;
		U2 = -1 + ((float) rand () / RAND_MAX) * 2;
		W = pow (U1, 2) + pow (U2, 2);
	}
	while (W >= 1 || W == 0);
 
	mult = sqrt ((-2 * log (W)) / W);
	X1 = U1 * mult;
	X2 = U2 * mult;
 
	call = !call;
 
	return (mu + sigma * (float) X1);
}

//------------------------------------------------------------------------------------------------------------//

static void humidity_get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	/* Populat the buffer with the response payload */
	char message[DIM_BUFFER];
	int length;
	float tmp;
	
	tmp = (float)((float)humidity_k - (int)humidity_k);
	tmp = tmp * 100;
	sprintf(message, "{'humidity':'%d.%d'}", (int)humidity_k, (int)tmp);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_header_etag(response, (uint8_t *) &length, 1);
	REST.set_response_payload(response, buffer, length);
}


static void humidity_periodic_handler() {
	char buffer_humidity_k[DIM_BUFFER_OUTPUT];
	char buffer_humidity_k_1[DIM_BUFFER_OUTPUT];
	float tmp;
	
	memset(buffer_humidity_k, '\0', DIM_BUFFER_OUTPUT);
	memset(buffer_humidity_k_1, '\0', DIM_BUFFER_OUTPUT);
	
    	/* Output Function */
	humidity_k = randn(MU, SIGMA);
	
	tmp = (float)((float)humidity_k - (int)humidity_k);
	tmp = tmp * 100;
	sprintf(buffer_humidity_k, "%d.%d", (int)humidity_k, (int)tmp);

	tmp = (float)((float)humidity_k_1 - (int)humidity_k);
	tmp = tmp * 100;
	sprintf(buffer_humidity_k_1, "%d.%d", (int)humidity_k_1, (int)tmp);

	if (strcmp(buffer_humidity_k, buffer_humidity_k_1) != 0)
		REST.notify_subscribers(&resource_humidity);

	humidity_k_1 = humidity_k;
}


