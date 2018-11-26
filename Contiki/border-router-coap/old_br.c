
#include "contiki.h"
//#include "contiki-lib.h"
//#include "contiki-net.h"

#include "net/ip/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/rpl/rpl.h"
#include "net/netstack.h"

#include "dev/slip.h"

//#include "er-coap.h"
#include "rest-engine.h"

#include <stdlib.h>
#include <string.h>


/* Maximum number of address */
#define MAX_ADDR		UIP_CONF_MAX_ROUTES

/* Maximum length of the address plus the ":" separators */
#define MAX_ADDR_LENGTH	32 + 7

/* Number of chars for the address separators ("," and "'") */
#define SEP_LENGTH		(MAX_ADDR - 1) + (MAX_ADDR * 2)

/* Number of chars for the JSON key and parenthesis {'routes':['']} */
#define JSON_LENGTH		15


uip_ipaddr_t	prefix;
uint8_t			prefix_set;

uip_ds6_route_t	*route;

char			address[MAX_ADDR_LENGTH];
char			message[MAX_ADDR * MAX_ADDR_LENGTH + SEP_LENGTH + JSON_LENGTH];

uint8_t			last_byte	= 0;
int				route_event	= -1;


PROCESS(border_router_process, "BorderRouter");
PROCESS(coap_process, "CoAP");


AUTOSTART_PROCESSES(&border_router_process, &coap_process);


/*---------------------------------------------------------------------------*/
void request_prefix(void) {
	/* mess up uip_buf with a dirty request... */
	uip_buf[0] = '?';
	uip_buf[1] = 'P';
	uip_len = 2;
	slip_send();
	uip_len = 0;
}


/*---------------------------------------------------------------------------*/
void set_prefix_64(uip_ipaddr_t *prefix_64) {
	rpl_dag_t *dag;
	uip_ipaddr_t ipaddr;
	memcpy(&prefix, prefix_64, 16);
	memcpy(&ipaddr, prefix_64, 16);
	prefix_set = 1;
	uip_ds6_set_addr_iid(&ipaddr, &uip_lladdr);
	uip_ds6_addr_add(&ipaddr, 0, ADDR_AUTOCONF);

	dag = rpl_set_root(RPL_DEFAULT_INSTANCE, &ipaddr);
	if(dag != NULL) {
		rpl_set_prefix(dag, &prefix, 64);
		printf("created a new RPL dag\n");
	}
}


/**
 * @brief	translate an machine readable IPv6 address into an human
 *			human readable IPv6 addr.
 *
 * @param[in]	addr	a struct that use contiki for manage IPv6 addrs.
 * @param[out]	buffer	output string
 */
void ipaddr_get(const uip_ipaddr_t *addr, char *buffer) {
	uint16_t	a;
	int			i, f;
	char		num[2];

	strcpy(buffer, "");

	for (i = 0, f = 0; i < sizeof(uip_ipaddr_t); i += 2) {
		a = (addr->u8[i] << 8) + addr->u8[i + 1];
		if (a == 0 && f >= 0) {
			if (f++ == 0) 
				strcat(buffer, "::");
		} else {
			if (f > 0) {
				f = -1;
			} else if (i > 0) {
				strcat(buffer, ":");
			}
			sprintf(num, "%x", a);
			strcat(buffer, num);
		}
	}
}


/*---------------------------------------------------------------------------*/
static void get_handler(void *request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(get_routes, "title=\"getRoutes\";rt=\"Text\"", get_handler, NULL, NULL, NULL);

static void get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {

	if (last_byte == 0) {

		sprintf(message, "{'routes':['");

		for (route = uip_ds6_route_head(); route != NULL; route = uip_ds6_route_next(route)) {
			if (route != uip_ds6_route_head())
				strcat(message, "','");
			ipaddr_get(&route->ipaddr, address);
			strcat(message, address);
		}
		strcat(message, "']}");
	}

	snprintf((char *)buffer, preferred_size + 1, message + last_byte);

	//coap_set_header_block2(void *packet, uint32_t num, uint8_t more, uint16_t size)
	coap_set_header_block2(response, (last_byte / preferred_size), (strlen(message) - last_byte > preferred_size), preferred_size);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON);

	REST.set_response_payload(response, buffer, MIN(strlen(message) - last_byte, preferred_size));

	last_byte	+= MIN(strlen(message) - last_byte, preferred_size);
	*offset		+= MIN(strlen(message) - last_byte, preferred_size);

	if (last_byte >= strlen(message)) {
		last_byte = 0;
		*offset = -1;
	}
}


/*---------------------------------------------------------------------------*/
static void event_get_handler(void *request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void event_handler();

EVENT_RESOURCE(evt_routes, "title=\"evtRoutes\";rt=\"Text\";obs", event_get_handler, NULL, NULL, NULL, event_handler);

static void route_callback(int event, uip_ipaddr_t *ipaddr, uip_ipaddr_t *nexthop, int num_routes) {

	route_event = event;
	if (event == UIP_DS6_NOTIFICATION_ROUTE_ADD || event == UIP_DS6_NOTIFICATION_ROUTE_RM) {
		ipaddr_get(ipaddr, address);
		evt_routes.trigger();
	}
}
/*
		//if (nexthop != NULL)
			//uip_ds6_route_add(ipaddr, 64, nexthop);
		ipaddr_get(ipaddr, address);
		printf("ADD: %s\n", address);
		evt_routes.trigger();
	} else if (event == UIP_DS6_NOTIFICATION_ROUTE_RM) {
		//route = uip_ds6_route_lookup(ipaddr);
		//uip_ds6_route_rm(route);
		ipaddr_get(ipaddr, address);
		printf("RM: %s\n", address);
		evt_routes.trigger();
	}
}
*/
static void event_get_handler(void *request, void *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {

	if (last_byte == 0) {
		if (route_event == -1) {
			sprintf(message, "No event");
			goto finished;
		}
		else if (route_event == UIP_DS6_NOTIFICATION_ROUTE_RM) {
			sprintf(message, "{'rm':'");
		}
		else if (route_event == UIP_DS6_NOTIFICATION_ROUTE_ADD) {
			sprintf(message, "{'add':'");
		}
		strcat(message, address);
		strcat(message, "'}");
	}

	finished:

	snprintf((char *)buffer, preferred_size + 1, message + last_byte);

	//coap_set_header_block2(void *packet, uint32_t num, uint8_t more, uint16_t size)
	//coap_set_header_block2(response, (last_byte / preferred_size), (strlen(message) - last_byte > preferred_size), preferred_size);

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON);

	REST.set_response_payload(response, buffer, MIN(strlen(message) - last_byte, preferred_size));

	last_byte	+= MIN(strlen(message) - last_byte, preferred_size);
	*offset		+= MIN(strlen(message) - last_byte, preferred_size);

	if (last_byte >= strlen(message)) {
		last_byte = 0;
		//route_event = -1;
		*offset = -1;
	}
}


static void event_handler() {
	REST.notify_subscribers(&evt_routes);
}

/*---------------------------------------------------------------------------*/
PROCESS_THREAD(coap_process, ev, data) {

	static struct uip_ds6_notification n;

	PROCESS_BEGIN();

	// attach route callback
	uip_ds6_notification_add(&n, route_callback);

	PROCESS_PAUSE();

	rest_init_engine();
	rest_activate_resource(&get_routes, "getRoutes");
	rest_activate_resource(&evt_routes, "evtRoutes");

	while(1) {

		PROCESS_WAIT_EVENT();

	}

	PROCESS_END();

}


/*---------------------------------------------------------------------------*/
PROCESS_THREAD(border_router_process, ev, data) {

	static struct etimer et;

	PROCESS_BEGIN();

	/* While waiting for the prefix to be sent through the SLIP connection, the future
	 * border router can join an existing DAG as a parent or child, or acquire a default
	 * router that will later take precedence over the SLIP fallback interface.
	 * Prevent that by turning the radio off until we are initialized as a DAG root.
	 */
	prefix_set = 0;
	NETSTACK_MAC.off(0);

	PROCESS_PAUSE();

	//SENSORS_ACTIVATE(button_sensor);

	printf("RPL-Border router started\n");
	#if 0
		NETSTACK_MAC.off(1);
	#endif

	/* Request prefix until it has been received */
	while(!prefix_set) {
		etimer_set(&et, CLOCK_SECOND);
		request_prefix();
		PROCESS_WAIT_EVENT_UNTIL(etimer_expired(&et));
	}


	NETSTACK_MAC.off(1);

	while(1) {
		PROCESS_YIELD();

		//if (ev == sensors_event && data == &button_sensor) {
			//printf("Initiating global repair\n");
			//rpl_repair_root(RPL_DEFAULT_INSTANCE);
		//}
	}

	PROCESS_END();

}