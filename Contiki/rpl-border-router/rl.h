#define MAX_ROUTES	4

struct route_t {
	char	str[64];
	int	next;
};

void rl_init();
struct route_t* rl_head();
struct route_t* rl_next();
struct route_t* rl_add();
struct route_t* rl_rm();
void rl_print();
