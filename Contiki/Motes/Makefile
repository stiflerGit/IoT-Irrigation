
DEFINES+=PROJECT_CONF_H=\"project-conf.h\"

all: coap-server

SMALL=1

CONTIKI = /home/osboxes/contiki
CFLAGS += -DPROJECT_CONF_H=\"project-conf.h\"

REST_RESOURCES_DIR = ./resources
REST_RESOURCES_FILES = res-id.c res-type.c res-position.c res-battery.c res-temperature.c res-humidity.c res-soil_moisture.c

PROJECTDIRS += $(REST_RESOURCES_DIR)
PROJECT_SOURCEFILES += $(REST_RESOURCES_FILES)

APPS += er-coap
APPS += rest-engine

CONTIKI_WITH_IPV6 = 1
include $(CONTIKI)/Makefile.include

