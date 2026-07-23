#!/bin/bash
set -e

CONFIG_FILE=/usr/local/tomcat/webapps/ROOT/WEB-INF/cfg/config.xml

DB_URL="${DB_URL:-jdbc:mysql://db:3306/ds?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-ds}"
ABSOLUTE_PATH="${ABSOLUTE_PATH:-/usr/local/tomcat/webapps/ROOT/}"
LOXPATH="${LOXPATH:-/tmp/ds2-lox/}"

set_setting() {
	local name="$1"
	local value="$2"
	local xml_escaped_value sed_escaped_value
	# config.xml is XML, so literal & in e.g. a JDBC URL must become &amp;
	xml_escaped_value=$(printf '%s' "$value" | sed -e 's/&/\&amp;/g')
	# Then escape for sed's replacement-text syntax (& and \ are special there too)
	sed_escaped_value=$(printf '%s' "$xml_escaped_value" | sed -e 's/[\&]/\\&/g')
	sed -i "s#<setting type=\"string\" name=\"${name}\" value=\"[^\"]*\" />#<setting type=\"string\" name=\"${name}\" value=\"${sed_escaped_value}\" />#" "$CONFIG_FILE"
}

set_setting "db_url" "$DB_URL"
set_setting "db_user" "$DB_USER"
set_setting "db_password" "$DB_PASSWORD"
set_setting "ABSOLUTE_PATH" "$ABSOLUTE_PATH"
set_setting "LOXPATH" "$LOXPATH"

mkdir -p "$LOXPATH"

exec "$@"
