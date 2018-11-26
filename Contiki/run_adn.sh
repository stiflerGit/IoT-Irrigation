#!/bin/bash

Project="Platform"

# ----- ADN IN ----- #
cd ~/eclipse-workspace/$Project/Server && terminator --new-tab -x sh -c "java -jar target/IN-0.0.1-SNAPSHOT-jar-with-dependencies.jar; bash" \
&& \

# ----- ADN MN ----- #
cd ~/eclipse-workspace/$Project/Gateway && gnome-terminal -x sh -c "java -jar target/MN-0.0.1-SNAPSHOT-jar-with-dependencies.jar; bash"