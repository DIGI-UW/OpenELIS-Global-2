FROM nginx:1.15-alpine

USER root
RUN apk update; \ 
    apk upgrade; \
    apk add curl; \
    apk add bash
    
ADD healthcheck.sh /healthcheck.sh
RUN chown nginx:nginx /healthcheck.sh; \
    chmod 770 /healthcheck.sh;  

