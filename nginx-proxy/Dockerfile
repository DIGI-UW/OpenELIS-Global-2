FROM nginx:1.15-alpine

USER root
RUN apk update; \ 
    apk upgrade; \
    apk add curl; \
    apk add bash
    
ADD healthcheck.sh /healthcheck.sh
RUN chown nginx:nginx /healthcheck.sh; \
    chmod 770 /healthcheck.sh

ADD docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]

