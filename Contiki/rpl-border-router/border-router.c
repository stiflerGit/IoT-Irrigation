
#include "contiki.h"
#include "contiki-lib.h"
#include "contiki-net.h"

#include "net/ip/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/rpl/rpl.h"
#include "net/netstack.h"

#include "dev/slip.h"
#include "dev/button-sensor.h"

#include "rest-engine.h"

#include <stdlib.h>
#include <string.h>


/* Maximum number of address */
#define MAX_ADDR		4
/* Maximum length of the address plus the ":" separators */
#define MAX_ADDR_LENGTH		32 + 7
/* Number of chars for the address separators ("," and "'") */
#define SEP_LENGTH		(MAX_ADDR - 1) + (MAX_ADDR * 2)
/* Number of chars for the JSON key and parenthesis {'routes':['']} */
#define JSON_LENGTH		15

static uip_ipaddr_t 	prefix;
static uint8_t		prefix_set;

static char	msg[MAX_ADDR * MAX_ADDR_LENGTH + SEP_LENGTH + JSON_LENGTH + 1];
			/**< Message buffer for get response*/
static int32_t	last_byte = 0;	
			/**< last byte sent by the get blockwise transfer*/

PROCESS(border_router_process, "BR");
PROCESS(border_router_coap, "BR-CoAP");

AUTOSTART_PROCESSES(&border_router_process, &border_router_coap);

static void get_handler(void *request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void event_handler();

EVENT_RESOURCE(res_routes, "title=\"routes\";", get_handler, NULL, NULL, NULL, event_handler);

static char		addr[MAX_ADDR_LENGTH]; 
				/**< human-readable IPv6 address buffer */
static uip_ds6_route_t	*route;	
				/**< pointer to the route table*/

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
/* -------------------------------------------------------------------------- */
/* ---------------------------------- CoAP ---------------------------------- */ 
/* -------------------------------------------------------------------------- */
/**
 * @brief	translate an machine readable IPv6 address into an human
 *		human readable IPv6 addr.
 *
 * @param[in]	addr		a struct that use contiki for manage IPv6 addrs.
 * @param[out]	str_addr	output string
 */
static void ipaddr_get(const uip_ipaddr_t *addr, char *str_addr) {
	uint16_t	a;
	int		i, f;
	char		num[2];

	strcpy(str_addr, "");
	for (i = 0, f = 0; i < sizeof(uip_ipaddr_t); i += 2) {
		a = (addr->u8[i] << 8) + addr->u8[i + 1];
		if (a == 0 && f >= 0) {
			if (f++ == 0) 
				strcat(str_addr, "::");
		} else {
			if (f > 0) {
				f = -1;
			} else if (i > 0) {
				strcat(str_addr, ":");
			}
			sprintf(num, "%x", a);
			strcat(str_addr, num);
		}
	}
}

/* -------------------------------------------------------------------------- */
/**
 * @brief	build the JSON message for the Get response
 */
void build_message() {

	sprintf(msg, "{'routes':['");

	for (route = uip_ds6_route_head(); route != NULL; route = uip_ds6_route_next(route)) {
		if (route != uip_ds6_route_head())
			strcat(msg, "','");
		ipaddr_get(&route->ipaddr, addr);
		strcat(msg, addr);
	}
	strcat(msg, "']}");
}
/*---------------------------------------------------------------------------*/
/**
 * @brief	get body
 * 
 * It copies the message builded by buil_message in the response body, manages
 * coap blockwise transfer as well
 */
static void get_handler(void *request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
	int32_t		msg_length;
	int32_t		n_write = 0;

	if (last_byte == 0) 
		build_message();

	msg_length = strlen(msg);
	n_write = snprintf((char *)buffer, preferred_size + 1, "%s", msg + last_byte);

	if (n_write > preferred_size) 
		n_write = preferred_size;

	REST.set_header_content_type(response, REST.type.APPLICATION_JSON);
	REST.set_response_payload(response, buffer, n_write);

	last_byte += n_write;
	*offset += n_write;

	/* If the message is too long it has to be sent using CoAP's blockwise transfer. */
	if (last_byte >= msg_length) {
		last_byte = 0;
		*offset = -1;
	}
}
/*---------------------------------------------------------------------------*/
/**
 * @brief	notify all subscribers with a get response
 */
static void event_handler() {
	REST.notify_subscribers(&res_routes);
}
/* -------------------------------------------------------------------------- */
/**
 * @brief	updates the route table and eventually notifies subscribers
 */
static void route_callback(int event, uip_ipaddr_t *ipaddr, uip_ipaddr_t *nexthop, int num_routes) {

	if (event == UIP_DS6_NOTIFICATION_ROUTE_ADD) {
		// routes added automatically by BR
		res_routes.trigger();
	} else if (event == UIP_DS6_NOTIFICATION_ROUTE_RM) {
		// routes removed manually (BR doesn't remove automatically)
		route = uip_ds6_route_lookup(ipaddr);
		uip_ds6_route_rm(route);
		res_routes.trigger();
	}
}
/*---------------------------------------------------------------------------*/
PROCESS_THREAD(border_router_coap, ev, data) 
{
static struct uip_ds6_notification notification;

	PROCESS_BEGIN();
	// attach route callback
	uip_ds6_notification_add(&notification, route_callback);

	PROCESS_PAUSE();
	
	rest_init_engine();
	rest_activate_resource(&res_routes, "routes");

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

	SENSORS_ACTIVATE(button_sensor);

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

		if (ev == sensors_event && data == &button_sensor) {
			printf("Initiating global repair\n");
			rpl_repair_root(RPL_DEFAULT_INSTANCE);
		}
	}

	PROCESS_END();

}

