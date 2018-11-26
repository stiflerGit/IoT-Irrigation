#!/bin/bash

# ----- Cooja ----- #
cd ~/git/contiki/tools/cooja && gnome-terminal -x sh -c "ant run; bash"
#wait $!	# Can also be stored in a variable as pid=$!