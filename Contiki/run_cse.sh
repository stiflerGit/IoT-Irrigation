#!/bin/bash

# ----- IN ----- #
cd ~/git/org.eclipse.om2m/org.eclipse.om2m.site.in-cse/target/products/in-cse/linux/gtk/x86_64 && gnome-terminal -x sh -c "sudo chmod a+x start.sh;./start.sh; bash" \
&& \

# ----- MN ----- #
cd ~/git/org.eclipse.om2m/org.eclipse.om2m.site.mn-cse/target/products/mn-cse/linux/gtk/x86_64 && gnome-terminal -x sh -c "sudo chmod a+x start.sh;./start.sh; bash"