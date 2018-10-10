/*
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


#define SAMPLING_TIME	60*CLOCK_SECOND
#define MU	3.
#define SIGMA	0.5



// Input variable 
extern float mote_soil_moisture;


// Output at step k 
float soil_moisture_k = 0; 
// Output at step k-1 
float soil_moisture_k_1 = 0; 


static void soil_moisture_periodic_handler();
static void soil_moisture_get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

PERIODIC_RESOURCE(resource_soil_moisture, "title=\"soil_moisture\";rt=\"Text\"", soil_moisture_get_handler, NULL, NULL, NULL, SAMPLING_TIME, soil_moisture_periodic_handler);


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


static void soil_moisture_get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	// Populat the buffer with the response payload 
	char message[DIM_BUFFER];
	int length;
	float tmp;
	
	tmp = (float)((float)soil_moisture_k - (int)soil_moisture_k);
	tmp = tmp * 100;
	sprintf(message, "{'soil_moisture':'%d.%d'}", (int)soil_moisture_k, (int)tmp);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_header_etag(response, (uint8_t *) &length, 1);
	REST.set_response_payload(response, buffer, length);
}


static void soil_moisture_periodic_handler() {
	char buffer_soil_moisture_k[DIM_BUFFER_OUTPUT];
	char buffer_soil_moisture_k_1[DIM_BUFFER_OUTPUT];
	float tmp;

	memset(buffer_soil_moisture_k, '\0', DIM_BUFFER_OUTPUT);
	memset(buffer_soil_moisture_k_1, '\0', DIM_BUFFER_OUTPUT);

    	// Output Function 
	soil_moisture_k = randn(MU, SIGMA);

	tmp = (float)((float)soil_moisture_k - (int)soil_moisture_k);
	tmp = tmp * 100;
	sprintf(buffer_soil_moisture_k, "%d.%d", (int)soil_moisture_k, (int)tmp);

	tmp = (float)((float)soil_moisture_k_1 - (int)soil_moisture_k);
	tmp = tmp * 100;
	sprintf(buffer_soil_moisture_k_1, "%d.%d", (int)soil_moisture_k_1, (int)tmp);

	if (strcmp(buffer_soil_moisture_k, buffer_soil_moisture_k_1) != 0)
		REST.notify_subscribers(&resource_soil_moisture);

	soil_moisture_k_1 = soil_moisture_k;
}

*/

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



#define SAMPLING_TIME	60*5*CLOCK_SECOND
#define DIM_BUFFER_OUTPUT	20
#define COST_K_1	1.85
#define COST_K_2	0.85
#define COST_E	0.000115



// Input variable 
extern float mote_soil_moisture;
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


//static void soil_moisture_periodic_handler();
static void soil_moisture_get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void soil_moisture_put_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void soil_moisture_event_handler();
EVENT_RESOURCE(resource_soil_moisture, "title=\"soil_moisture\";rt=\"Text\"", soil_moisture_get_handler, NULL, soil_moisture_put_handler, NULL, soil_moisture_event_handler);


static void soil_moisture_get_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	// Populat the buffer with the response payload 
	char message[DIM_BUFFER];
	int length;
	float tmp;

	tmp = (float)((float)mote_soil_moisture - (int)mote_soil_moisture);
	tmp = tmp * 100;
	sprintf(message, "{'soil_moisture':'%d.%d'}", (int)mote_soil_moisture, (int)tmp);
	length = strlen(message);
	memcpy(buffer, message, length);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON); 
	REST.set_header_etag(response, (uint8_t *) &length, 1);
	REST.set_response_payload(response, buffer, length);
}


static void soil_moisture_put_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	int new_value, len;
	const char *val = NULL;

	len = REST.get_post_variable(request, "value", &val);

	if (len > 0) {
		new_value = atoi(val);
		PRINTF("new value %d\n", new_value);
		mote_soil_moisture = new_value;
		soil_moisture_event_handler();
		REST.set_response_status(response, REST.status.CREATED);
	} else {
		REST.set_response_status(response, REST.status.BAD_REQUEST);
	}
}

static void soil_moisture_event_handler() {
	REST.notify_subscribers(&resource_soil_moisture);
}

/*
static void soil_moisture_periodic_handler() {
	char buffer_output_k[DIM_BUFFER_OUTPUT];
	char buffer_output_k_1[DIM_BUFFER_OUTPUT];
	float tmp;

	memset(buffer_output_k, '\0', DIM_BUFFER_OUTPUT);
	memset(buffer_output_k_1, '\0', DIM_BUFFER_OUTPUT);
  
	 // Calculating error at step k 
	error_k = mote_soil_moisture - output_k;
	// Output Function 
	output_k = COST_K_1*output_k_1 - COST_K_2*output_k_2 - COST_E*error_k;

	tmp = (float)((float)output_k - (int)output_k);
	tmp = tmp * 100;
	sprintf(buffer_output_k, "%d.%d", (int)output_k, (int)tmp);

	tmp = (float)((float)output_k_1 - (int)output_k);
	tmp = tmp * 100;
	sprintf(buffer_output_k_1, "%d.%d", (int)output_k_1, (int)tmp);

	if (strcmp(buffer_output_k, buffer_output_k_1) != 0)
		REST.notify_subscribers(&resource_soil_moisture);

	// Updating  control variables 
	error_k_2 = error_k_1;
	error_k_1 = error_k;
	output_k_2 = output_k_1;
	output_k_1 = output_k;
}
*/
