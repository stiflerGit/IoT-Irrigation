/*
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

#define SAMPLING_TIME	60*CLOCK_SECOND
#define DIM_BUFFER_OUTPUT	20
#define COST_K_1	1.85
#define COST_K_2	0.85
#define COST_E	0.00115

// Input variable 
extern float mote_temperature;

// Error at step k 
float error_k = 0;
// Error at step k-1 
float error_k_1 = 0;
// Error at step k-2 
float error_k_2 = 0;

// Output at step k 
float output_k = 0; 
// Output at step k-1 
float output_k_1 = 0; 
// Output at step k-2 
float output_k_2 = 0; 


static void temperature_periodic_handler();
static void temperature_get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);


PERIODIC_RESOURCE(resource_temperature, "title=\"temperature\";rt=\"Text\"", temperature_get_handler, NULL, NULL, NULL, SAMPLING_TIME, temperature_periodic_handler);

static void temperature_get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	// Populat the buffer with the response payload 
	char message[DIM_BUFFER];
	int length;
	float tmp;

	tmp = (float)((float)output_k - (int)output_k);
	tmp = tmp * 100;
	sprintf(message, "{'temperature':'%d.%d'}", (int)output_k, (int)tmp);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_header_etag(response, (uint8_t *) &length, 1);
	REST.set_response_payload(response, buffer, length);
}


static void temperature_periodic_handler() {
	char buffer_output_k[DIM_BUFFER_OUTPUT];
	char buffer_output_k_1[DIM_BUFFER_OUTPUT];
	float tmp;
  
	memset(buffer_output_k, '\0', DIM_BUFFER_OUTPUT);
	memset(buffer_output_k_1, '\0', DIM_BUFFER_OUTPUT);
  
   	 // Calculating error at step k 
	error_k = mote_temperature - output_k;
  
    	// Output Function 
	output_k = COST_K_1*output_k_1 - COST_K_2*output_k_2 + COST_E*error_k;
  
	tmp = (float)((float)output_k - (int)output_k);
	tmp = tmp * 100;
	sprintf(buffer_output_k, "%d.%d", (int)output_k, (int)tmp);

	tmp = (float)((float)output_k_1 - (int)output_k);
	tmp = tmp * 100;
	sprintf(buffer_output_k_1, "%d.%d", (int)output_k_1, (int)tmp);

	if (strcmp(buffer_output_k, buffer_output_k_1) != 0)
		REST.notify_subscribers(&resource_temperature);

	// Updating  control variables 
	error_k_2 = error_k_1;
	error_k_1 = error_k;
	output_k_2 = output_k_1;
	output_k_1 = output_k;
}
*/

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


#define SAMPLING_TIME	60*3*CLOCK_SECOND
#define MU	18.5
#define SIGMA	0.5



/* Input variable */
extern float mote_temperature;


/* Output at step k */
float temperature_k = 0; 
/* Output at step k-1 */
float temperature_k_1 = 0; 


static void temperature_periodic_handler();
static void temperature_get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

PERIODIC_RESOURCE(resource_temperature, "title=\"temperature\";rt=\"Text\"", temperature_get_handler, NULL, NULL, NULL, SAMPLING_TIME, temperature_periodic_handler);


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


static void temperature_get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	/* Populat the buffer with the response payload */
	char message[DIM_BUFFER];
	int length;
	float tmp;
	
	tmp = (float)((float)temperature_k - (int)temperature_k);
	tmp = tmp * 100;
	sprintf(message, "{'temperature':'%d.%d'}", (int)temperature_k, (int)tmp);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_header_etag(response, (uint8_t *) &length, 1);
	REST.set_response_payload(response, buffer, length);
}


static void temperature_periodic_handler() {
	char buffer_temperature_k[DIM_BUFFER_OUTPUT];
	char buffer_temperature_k_1[DIM_BUFFER_OUTPUT];
	float tmp;

	memset(buffer_temperature_k, '\0', DIM_BUFFER_OUTPUT);
	memset(buffer_temperature_k_1, '\0', DIM_BUFFER_OUTPUT);

    	/* Output Function */
	temperature_k = randn(MU, SIGMA);

	tmp = (float)((float)temperature_k - (int)temperature_k);
	tmp = tmp * 100;
	sprintf(buffer_temperature_k, "%d.%d", (int)temperature_k, (int)tmp);

	tmp = (float)((float)temperature_k_1 - (int)temperature_k);
	tmp = tmp * 100;
	sprintf(buffer_temperature_k_1, "%d.%d", (int)temperature_k_1, (int)tmp);

	if (strcmp(buffer_temperature_k, buffer_temperature_k_1) != 0)
		REST.notify_subscribers(&resource_temperature);

	temperature_k_1 = temperature_k;
}

