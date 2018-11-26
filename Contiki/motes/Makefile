
DEFINES+=PROJECT_CONF_H=\"project-conf.h\"

all: coap-server

SMALL=1

CONTIKI = /home/universita/git/contiki/
CFLAGS += -DPROJECT_CONF_H=\"project-conf.h\"

REST_RESOURCES_DIR = ./resources
REST_RESOURCES_FILES = type.c battery.c gps.c temperature.c humidity.c

PROJECTDIRS += $(REST_RESOURCES_DIR)
PROJECT_SOURCEFILES += $(REST_RESOURCES_FILES)

APPS += er-coap
APPS += rest-engine

CONTIKI_WITH_IPV6 = 1
include $(CONTIKI)/Makefile.include

