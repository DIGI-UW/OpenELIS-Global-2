#!/bin/bash

find /var/lib/openelis-global/data/pg_log/ -type f -mtime +7 | xargs rm
find /var/lib/openelis-global/logs/ -type f -mtime +30 | xargs rm
find /var/lib/openelis-global/tomcatLogs/ -type f -mtime +30 | xargs rm
