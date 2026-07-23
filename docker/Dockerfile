FROM tomcat:9-jdk11

# The WAR must already be built (mvnw -pl game -am package -DskipTests) before
# building this image. We explode it at build time (rather than dropping the
# .war into webapps/ for Tomcat to explode at startup) so the entrypoint can
# rewrite WEB-INF/cfg/config.xml before Tomcat ever reads it.
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY game/target/*.war /tmp/app.war
RUN mkdir -p /usr/local/tomcat/webapps/ROOT && \
    cd /usr/local/tomcat/webapps/ROOT && \
    jar -xf /tmp/app.war && \
    rm /tmp/app.war

COPY docker/app/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

ENTRYPOINT ["/entrypoint.sh"]
CMD ["catalina.sh", "run"]
