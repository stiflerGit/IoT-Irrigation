
#include <stdlib.h>
#include <time.h>

#ifndef random
#define random(x) (double)rand() / (double)((unsigned)x + 1)
#endif

#ifndef randomize
#define randomize srand((unsigned)time(NULL))
#endif