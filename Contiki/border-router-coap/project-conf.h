
#ifndef PROJECT_ROUTER_CONF_H_
#define PROJECT_ROUTER_CONF_H_

#ifndef UIP_FALLBACK_INTERFACE
#define UIP_FALLBACK_INTERFACE			rpl_interface
#endif

#ifndef RPL_CONF_DEFAULT_LIFETIME
#define RPL_CONF_DEFAULT_LIFETIME		3
#endif

#ifndef NETSTACK_CONF_RDC
#define NETSTACK_CONF_RDC				nullrdc_driver
#endif

#ifndef NETSTACK_CONF_MAC
#define NETSTACK_CONF_MAC				csma_driver
#endif

/* Disabling TCP on CoAP nodes. */
#ifndef UIP_CONF_TCP
#define UIP_CONF_TCP					0
#endif

/* IP buffer size must match all other hops, in particular the border router. */
#ifndef UIP_CONF_BUFFER_SIZE
#define UIP_CONF_BUFFER_SIZE			256
#endif

/* Increase rpl-border-router IP-buffer when using more than 64. */
#ifndef REST_MAX_CHUNK_SIZE
#define REST_MAX_CHUNK_SIZE				64
#endif

/* Multiplies with chunk size, be aware of memory constraints. */
#ifndef COAP_MAX_OPEN_TRANSACTIONS
#define COAP_MAX_OPEN_TRANSACTIONS		4
#endif

/* Must be <= open transactions, default is COAP_MAX_OPEN_TRANSACTIONS-1. */
#ifndef COAP_MAX_OBSERVERS
#define COAP_MAX_OBSERVERS				2
#endif

/* Filtering .well-known/core per query can be disabled to save space. */
#ifndef COAP_LINK_FORMAT_FILTERING
#define COAP_LINK_FORMAT_FILTERING		0
#endif

#ifndef COAP_PROXY_OPTION_PROCESSING
#define COAP_PROXY_OPTION_PROCESSING	0
#endif

#ifndef NBR_TABLE_CONF_MAX_NEIGHBORS
#define NBR_TABLE_CONF_MAX_NEIGHBORS	4
#endif

#ifndef UIP_CONF_MAX_ROUTES
#define UIP_CONF_MAX_ROUTES				4
#endif

#endif /* PROJECT_ROUTER_CONF_H_ */

/*
#ifndef random
#define random(x) (double)rand() / (double)((unsigned)x + 1)
#ifndef randomize
#define randomize srand(17)//(unsigned)time(NULL))

#ifndef UIP_CONF_RECEIVE_WINDOW
#define UIP_CONF_RECEIVE_WINDOW			60
#endif

#ifndef RPL_CONF_WITH_DAO_ACK
#define RPL_CONF_WITH_DAO_ACK			0
#endif
*/