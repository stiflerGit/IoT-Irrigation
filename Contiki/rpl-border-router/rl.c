#include "rl.h"

#include <stdio.h>
#include <string.h>
#include <assert.h>

#ifndef NULL
#define NULL ((void *)0)
#endif

static struct route_t	routes[MAX_ROUTES];
static int		free;
static int		head;
static int		len;

/**
 * @brief       initialize the routes list
 */
void rl_init()
{
int     i;

        for(i=0; i<MAX_ROUTES-1; i++) {
                routes[i].next = i+1;
        }
        routes[MAX_ROUTES-1].next = -1;
        free = 0;
        head = -1;
        len = 0;
}

/**
 * @brief	Return head of the route list
 */
struct route_t* rl_head()
{
	if(head != -1)
		return &routes[head];
	return NULL;
}

/**
 * @brief	returns the next route
 * 
 * @param[in]	r	route
 */
struct route_t* rl_next(struct route_t* r)
{
	assert(r != NULL);
	if(r->next == -1)
		return NULL;
	return &(routes[r->next]);
}

/**
 * @brief       add a new route to the list
 *
 * @param[in]   route   IPv6 Address to add
 * @return              index in the list where the new root has been inserted,
 *                      -1 if the list was full.
 */
struct route_t* rl_add(const char *route)
{
int  x;

        if(free != -1) {
                assert(route != NULL);
                assert(strcmp(route, "") != 0);
                strcpy(routes[free].str, route);
                x = head;
                head = free;
                free = routes[free].next;
                routes[head].next = x;
                len++;
		return &routes[head];
        }
        return NULL;
}

/**
 * @brief       remove a route from the list
 *
 * @param[in]   route   IPv6 address of the route to remove
 * @return              index in the list where the route has been founded and removed
 *                      -1 if the route was not present in the list
 */
struct route_t* rl_rm(const char *route)
{
int     i;
int     prev;

        prev = -1;
        for(i = head; i != -1 && strcmp(routes[i].str, route) != 0; i = routes[i].next) {
                prev = i;
        }
        if(i != -1){
                // useless
                strcpy(routes[i].str, "");
                if(i == head)
                        head = routes[head].next;
                else
                        routes[prev].next = routes[i].next;
                routes[i].next = free;
                free = i;
                len--;
        }
        return &routes[i];
}

void rl_print()
{
int     i;
char    str[256];

        for(i = 0; i < MAX_ROUTES; i++){
                sprintf(str, "%d : ", i);
                strcat(str, routes[i].str);
                strcat(str, "\t");
                printf(str);
        }
        printf("\n");
        for(i = 0; i < MAX_ROUTES; i++){
                sprintf(str, "%d : %d \t", i, routes[i].next);
                printf(str);;
        }
        printf("\n");
        sprintf(str, "rfree : %d\n", free);
        printf(str);
        sprintf(str, "rhead : %d\n", head);
        printf(str);
}
