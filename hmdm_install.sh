#!/bin/bash
#
# Headwind MDM installer script
# Tested on Ubuntu Linux 18.04 - 24.04, Ubuntu 22.04 is recommended
#
REPOSITORY_BASE=https://h-mdm.com/files
CLIENT_VERSION=6.31
DEFAULT_SQL_HOST=localhost
DEFAULT_SQL_PORT=3306
DEFAULT_SQL_BASE=hmdm
DEFAULT_SQL_USER=hmdm
DEFAULT_SQL_PASS=
DEFAULT_LOCATION="/opt/hmdm"
DEFAULT_SCRIPT_LOCATION="/opt/hmdm"
TOMCAT_HOME=$(ls -d /var/lib/tomcat* | tail -n1)
TOMCAT_SERVICE=$(echo $TOMCAT_HOME | awk '{n=split($1,A,"/"); print A[n]}')
TOMCAT_ENGINE="Catalina"
TOMCAT_HOST="localhost"
DEFAULT_PROTOCOL=https
DEFAULT_BASE_DOMAIN=
DEFAULT_BASE_PATH="ROOT"
DEFAULT_PORT=""
TEMP_DIRECTORY="/tmp"
TEMP_SQL_FILE="$TEMP_DIRECTORY/hmdm_init.sql"
TOMCAT_USER=$(ls -ld $TOMCAT_HOME/webapps | awk '{print $3}')

ADMIN_EMAIL=
SMTP_HOST=
SMTP_PORT=
SMTP_SSL=0
SMTP_STARTTLS=0
SMTP_USERNAME=
SMTP_PASSWORD=
SMTP_FROM=

TURN_HOST=
TURN_PORT=3478
TURN_USER=
TURN_PASS=

# =============================================================================
# CLI ARGUMENT PARSING
# When --hostname is provided, the installer runs fully non-interactively.
# All interactive prompts are skipped; suitable for Ansible deployments.
#
# Usage (non-interactive):
#   ./hmdm_install.sh --hostname www.example.com \
#                     --db-host localhost --db-port 3306 \
#                     --db-name hmdm --db-user hmdm --db-pass MyPass \
#                     --admin-pass AdminPass
#
# Usage (interactive): ./hmdm_install.sh
# =============================================================================
CLI_HOSTNAME=""
CLI_DB_HOST=""
CLI_DB_PORT=""
CLI_DB_NAME=""
CLI_DB_USER=""
CLI_DB_PASS=""
CLI_ADMIN_PASS=""
CLI_TURN_HOST=""
CLI_TURN_PORT=""
CLI_TURN_USER=""
CLI_TURN_PASS=""
NONINTERACTIVE=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        --hostname)   CLI_HOSTNAME="$2";   shift 2 ;;
        --db-host)    CLI_DB_HOST="$2";    shift 2 ;;
        --db-port)    CLI_DB_PORT="$2";    shift 2 ;;
        --db-name)    CLI_DB_NAME="$2";    shift 2 ;;
        --db-user)    CLI_DB_USER="$2";    shift 2 ;;
        --db-pass)    CLI_DB_PASS="$2";    shift 2 ;;
        --admin-pass) CLI_ADMIN_PASS="$2"; shift 2 ;;
        --turn-host)  CLI_TURN_HOST="$2";  shift 2 ;;
        --turn-port)  CLI_TURN_PORT="$2";  shift 2 ;;
        --turn-user)  CLI_TURN_USER="$2";  shift 2 ;;
        --turn-pass)  CLI_TURN_PASS="$2";  shift 2 ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Without options: fully interactive installation."
            echo "With --hostname:  non-interactive, suitable for Ansible."
            echo ""
            echo "Options:"
            echo "  --hostname    Public domain or IP            (required for non-interactive)"
            echo "  --db-host     MySQL host                    (default: $DEFAULT_SQL_HOST)"
            echo "  --db-port     MySQL port                    (default: $DEFAULT_SQL_PORT)"
            echo "  --db-name     MySQL database                (default: $DEFAULT_SQL_BASE)"
            echo "  --db-user     MySQL username                (default: $DEFAULT_SQL_USER)"
            echo "  --db-pass     MySQL password"
            echo "  --admin-pass  HMDM admin panel password"
            echo "  --turn-host   STUN/TURN server hostname   (optional, for remote control)"
            echo "  --turn-port   STUN/TURN server port       (default: 3478)"
            echo "  --turn-user   TURN username               (optional)"
            echo "  --turn-pass   TURN password               (optional)"
            echo ""
            echo "Example:"
            echo "  $0 --hostname www.example.com --db-host localhost --db-port 3306 \\"
            echo "     --db-name hmdm --db-user hmdm --db-pass MyPass --admin-pass AdminPass \\"
            echo "     --turn-host turn.example.com --turn-port 3478 --turn-user myuser --turn-pass mypass"
            exit 0
            ;;
        *) echo "Unknown option: $1"; echo "Run $0 --help for usage."; exit 1 ;;
    esac
done

if [ -n "$CLI_HOSTNAME" ]; then
    NONINTERACTIVE=true
fi

install_soft() {
    if [ "$NONINTERACTIVE" != "true" ]; then
        read -e -p "Install missing package(s) automatically? (Y/n)?" -n 1 -r
        echo
        if [[ ! "$REPLY" =~ ^[Yy]$ ]]; then
            echo "Please run: apt install $1"
            exit 1
        fi
    fi
    apt update
    apt install -y aapt tomcat9 mysql-client vim
    TOMCAT_HOME=$(ls -d /var/lib/tomcat* | tail -n1)
    TOMCAT_USER=$(ls -ld $TOMCAT_HOME/webapps | awk '{print $3}')
}

# Use sandbox directory for tomcat 9
if [ "$TOMCAT_HOME" == "/var/lib/tomcat9" ]; then
    DEFAULT_LOCATION="/var/lib/tomcat9/work"
fi

# Check if we are root
CURRENTUSER=$(whoami)
if [[ "$EUID" -ne 0 ]]; then
    if [ "$NONINTERACTIVE" == "true" ]; then
        echo "ERROR: Non-interactive mode requires running as root."
        exit 1
    fi
    echo "It is recommended to run the installer script as root."
    read -p "Proceed as $CURRENTUSER (Y/n)? " -n 1 -r
    echo
    if [[ ! "$REPLY" =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Check if there's an install folder
if [ ! -d "./install" ]; then
    echo "Cannot find installation directory (install)"
    echo "Please cd to the installation directory before running script!"
    exit 1
fi

# Check if there's aapt tool installed
if ! which aapt > /dev/null; then
    echo "Android App Packaging Tool is not installed!"
    install_soft aapt
fi

# Check MySQL client installation
if ! which mysql > /dev/null; then
    echo "MySQL client is not installed!"
    install_soft mysql-client
    exit 1
fi

# Check if tomcat user exists
getent passwd $TOMCAT_USER > /dev/null
if [ "$?" -ne 0 ]; then
    # Try tomcat8
    TOMCAT_USER="tomcat8"
    getent passwd $TOMCAT_USER >/dev/null
    if [ "$?" -ne 0 ]; then
        echo "Tomcat is not installed! User tomcat not found."
        echo "If you're running Tomcat as different user,"
        echo "please edit this script and update the TOMCAT_USER variable."
        exit 1
    fi
fi

# Check for Tomcat version in Ubuntu 20.04
OUTDATED_TOMCAT=$(/usr/share/tomcat9/bin/version.sh 2>&1 | grep "Server number" | grep "9.0.31")
if [ ! -z "$OUTDATED_TOMCAT" ]; then
    echo "Tomcat $OUTDATED_TOMCAT requires updating!"
    read -e -p "Update it now [Y/n]?: " -i "Y" REPLY
    if [[ "$REPLY" =~ ^[Yy]$ ]]; then
        # This clause is only for tomcat 9 so do not bother for the major version
        VERSION=9.0.40
        wget https://archive.apache.org/dist/tomcat/tomcat-9/v9.0.40/bin/apache-tomcat-${VERSION}.tar.gz
        tar -zxf apache-tomcat-${VERSION}.tar.gz
        cd apache-tomcat-${VERSION}
        chmod a+x bin
        chmod a+x lib
        chmod -R a+r bin
        chmod -R a+r lib
        chmod a+x bin/*.sh
        mv /usr/share/tomcat9/bin /usr/share/tomcat9/bin~
        mv /usr/share/tomcat9/lib /usr/share/tomcat9/lib~
        cp -r bin /usr/share/tomcat9
        cp -r lib /usr/share/tomcat9
        service tomcat9 restart
	cd ..
	rm -rf apache-tomcat-${VERSION}
	rm -f apache-tomcat-${VERSION}.tar.gz
        apt-mark hold tomcat9
    fi
fi

# Search for the WAR
SERVER_WAR=./server/target/launcher.war
if [ ! -f $SERVER_WAR ]; then
    SERVER_WAR=$(ls hmdm*.war | tail -1)
fi
if [ ! -f $SERVER_WAR ]; then
    echo "FAILED to find the WAR file of Headwind MDM!"
    echo "Did you compile the project?"
    exit 1
fi

# Check the Tomcat base folder
if [ ! -d "$TOMCAT_HOME" ]; then
    read -e -p "Enter the Tomcat base directory: " TOMCAT_HOME
    if [ ! -d "$TOMCAT_HOME" ]; then
        echo "The directory $TOMCAT_HOME does not exist."
        echo "Headwind MDM installer requires this directory to install the WAR file!"
        exit 1
    fi
fi

#read -p "Are you installing an open-source version? (Y/n)? " -n 1 -r
#echo
#if [[ $REPLY =~ ^[Yy]$ ]]; then
    CLIENT_VARIANT="os"
#else
#    CLIENT_VARIANT="master"
#fi

CLIENT_APK="hmdm-$CLIENT_VERSION-$CLIENT_VARIANT.apk"

if [ "$NONINTERACTIVE" == "true" ]; then
    LANGUAGE="en"
else
    read -e -p "Please choose the installation language (en/ru) [en]: " -i "en" LANGUAGE
fi
echo

if [ "$NONINTERACTIVE" == "true" ]; then
    SQL_HOST="${CLI_DB_HOST:-$DEFAULT_SQL_HOST}"
    SQL_PORT="${CLI_DB_PORT:-$DEFAULT_SQL_PORT}"
    SQL_BASE="${CLI_DB_NAME:-$DEFAULT_SQL_BASE}"
    SQL_USER="${CLI_DB_USER:-$DEFAULT_SQL_USER}"
    SQL_PASS="$CLI_DB_PASS"
    echo "Using database: $SQL_USER@$SQL_HOST:$SQL_PORT/$SQL_BASE"
else
    echo "MySQL database setup"
    echo "===================="
    echo "Make sure you've installed MySQL and created the database."
    echo "If you didn't create a database yet, please click Ctrl-C to break,"
    echo "then execute the following commands:"
    echo "-------------------------"
    echo "mysql -u root -p"
    echo "CREATE USER 'hmdm'@'localhost' IDENTIFIED BY 'topsecret';"
    echo "CREATE DATABASE hmdm CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    echo "GRANT ALL PRIVILEGES ON hmdm.* TO 'hmdm'@'localhost';"
    echo "FLUSH PRIVILEGES;"
    echo "exit"
    echo "-------------------------"

    read -e -p "MySQL host [$DEFAULT_SQL_HOST]: " -i "$DEFAULT_SQL_HOST" SQL_HOST
    read -e -p "MySQL port [$DEFAULT_SQL_PORT]: " -i "$DEFAULT_SQL_PORT" SQL_PORT
    read -e -p "MySQL database [$DEFAULT_SQL_BASE]: " -i "$DEFAULT_SQL_BASE" SQL_BASE
    read -e -p "MySQL user [$DEFAULT_SQL_USER]: " -i "$DEFAULT_SQL_USER" SQL_USER
    read -e -p "MySQL password: " -i "$DEFAULT_SQL_PASS" SQL_PASS
fi

MYSQL_CMD="mysql -h $SQL_HOST -P $SQL_PORT -u $SQL_USER -p$SQL_PASS $SQL_BASE"

# Check the MySQL access
echo "SELECT 1;" | $MYSQL_CMD > /dev/null 2>&1
if [ "$?" -ne 0 ]; then
    echo "Failed to connect to $SQL_HOST:$SQL_PORT/$SQL_BASE as $SQL_USER!"
    echo "Please make sure you've created the database!"
    exit 1
fi

TABLE_EXISTS=$($MYSQL_CMD -N -B -e "SHOW TABLES;")
if [ ! -z "$TABLE_EXISTS" ]; then
    if [ "$NONINTERACTIVE" == "true" ]; then
        echo "Non-interactive mode: clearing existing database tables..."
        echo "SET FOREIGN_KEY_CHECKS=0; DROP TABLE IF EXISTS applicationfilestocopytemp, applications, applicationversions, applicationversionstemp, configurationapplicationparameters, configurationapplications, configurationapplicationsettings, configurationfiles, configurations, customers, DATABASECHANGELOG, DATABASECHANGELOGLOCK, deviceapplicationsettings, devicegroups, devices, devicestatuses, groups, icons, pendingpushes, pendingsignup, permissions, plugin_apuppet_data, plugin_apuppet_settings, plugin_audit_log, plugin_contacts_config, plugin_deviceinfo_deviceparams, plugin_deviceinfo_deviceparams_device, plugin_deviceinfo_deviceparams_gps, plugin_deviceinfo_deviceparams_mobile, plugin_deviceinfo_deviceparams_mobile2, plugin_deviceinfo_deviceparams_wifi, plugin_deviceinfo_settings, plugin_devicelocations_history, plugin_devicelocations_latest, plugin_devicelocations_settings, plugin_devicelog_log, plugin_devicelog_setting_rule_devices, plugin_devicelog_settings, plugin_devicelog_settings_rules, plugin_devicereset_status, plugin_knox_rules, plugin_messaging_messages, plugin_openvpn_defaults, plugin_photo_photo, plugin_photo_photo_places, plugin_photo_places, plugin_photo_settings, plugin_push_messages, plugin_push_schedule, plugin_urlfilter_lists, plugin_netfilter_rules, plugin_netfilter_settings, plugin_photos_files, plugin_photos_settings, plugin_remotecontrol_sessions, plugin_remotecontrol_settings, plugins, pluginsdisabled, pushmessages, settings, trialkey, uploadedfiles, usagestats, userconfigurationaccess, userdevicegroupsaccess, userhints, userhinttypes, userrolepermissions, userroles, userrolesettings, users; SET FOREIGN_KEY_CHECKS=1;" | $MYSQL_CMD
        echo "Database has been cleared."
    else
        echo "The database is already setup."
        echo "To re-deploy Headwind MDM, the database needs to be cleared."
        echo "Clear the database? ALL DATA WILL BE LOST!"
        read -e -p "Type \"erase\" to clear the database and continue setup: " RESPONSE
        if [ "$RESPONSE" == "erase" ]; then
            echo "SET FOREIGN_KEY_CHECKS=0; DROP TABLE IF EXISTS applicationfilestocopytemp, applications, applicationversions, applicationversionstemp, configurationapplicationparameters, configurationapplications, configurationapplicationsettings, configurationfiles, configurations, customers, DATABASECHANGELOG, DATABASECHANGELOGLOCK, deviceapplicationsettings, devicegroups, devices, devicestatuses, groups, icons, pendingpushes, pendingsignup, permissions, plugin_apuppet_data, plugin_apuppet_settings, plugin_audit_log, plugin_contacts_config, plugin_deviceinfo_deviceparams, plugin_deviceinfo_deviceparams_device, plugin_deviceinfo_deviceparams_gps, plugin_deviceinfo_deviceparams_mobile, plugin_deviceinfo_deviceparams_mobile2, plugin_deviceinfo_deviceparams_wifi, plugin_deviceinfo_settings, plugin_devicelocations_history, plugin_devicelocations_latest, plugin_devicelocations_settings, plugin_devicelog_log, plugin_devicelog_setting_rule_devices, plugin_devicelog_settings, plugin_devicelog_settings_rules, plugin_devicereset_status, plugin_knox_rules, plugin_messaging_messages, plugin_openvpn_defaults, plugin_photo_photo, plugin_photo_photo_places, plugin_photo_places, plugin_photo_settings, plugin_push_messages, plugin_push_schedule, plugin_urlfilter_lists, plugin_netfilter_rules, plugin_netfilter_settings, plugin_photos_files, plugin_photos_settings, plugin_remotecontrol_sessions, plugin_remotecontrol_settings, plugins, pluginsdisabled, pushmessages, settings, trialkey, uploadedfiles, usagestats, userconfigurationaccess, userdevicegroupsaccess, userhints, userhinttypes, userrolepermissions, userroles, userrolesettings, users; SET FOREIGN_KEY_CHECKS=1;" | $MYSQL_CMD
            echo "Database has been cleared."
        else
            echo "Headwind MDM installation aborted"
            exit 1
        fi
    fi
fi

echo "Creating currval function to simulate PG currval"
cat <<EOF > $TEMP_DIRECTORY/hmdm_create_functions.sql
DELIMITER //

DROP FUNCTION IF EXISTS currval;

CREATE FUNCTION currval(seq_name VARCHAR(255)) RETURNS BIGINT
DETERMINISTIC
BEGIN
    DECLARE last_id BIGINT DEFAULT 0;
    DECLARE tbl_name VARCHAR(255);

    SET tbl_name = REPLACE(seq_name, '_id_seq', '');

    IF EXISTS (SELECT 1
               FROM INFORMATION_SCHEMA.TABLES
               WHERE TABLE_SCHEMA = DATABASE()
                 AND TABLE_NAME = tbl_name) THEN
        SELECT AUTO_INCREMENT - 1
        INTO last_id
        FROM INFORMATION_SCHEMA.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = tbl_name;
    ELSE
        SET last_id = 0;
    END IF;

    RETURN last_id;
END//
DELIMITER ;

EOF

$MYSQL_CMD < $TEMP_DIRECTORY/hmdm_create_functions.sql
if [ "$?" -ne 0 ]; then
    echo "ERROR: failed to create currval() function!"
    exit 1
fi
rm -f $TEMP_DIRECTORY/hmdm_create_functions.sql

echo "SUCCESS: Currval() function created!"
echo "-------------------------"


echo
echo "File storage setup"
echo "=================="
echo "Please choose where the files uploaded to Headwind MDM will be stored"
echo "If the directory doesn't exist, it will be created"
echo "##### FOR TOMCAT 9, USE SANDBOXED DIR: /var/lib/tomcat9/work #####"
echo

if [ "$NONINTERACTIVE" == "true" ]; then
    LOCATION=$DEFAULT_LOCATION
    echo "Using storage directory: $LOCATION"
else
    read -e -p "Headwind MDM storage directory [$DEFAULT_LOCATION]: " -i "$DEFAULT_LOCATION" LOCATION
fi

# Create directories
if [ ! -d $LOCATION ]; then
    mkdir -p $LOCATION || exit 1
    chown $TOMCAT_USER:$TOMCAT_USER $LOCATION || exit 1
fi
if [ ! -d $LOCATION/files ]; then
    mkdir $LOCATION/files
    chown $TOMCAT_USER:$TOMCAT_USER $LOCATION/files || exit 1
fi
if [ ! -d $LOCATION/plugins ]; then
    mkdir $LOCATION/plugins
    chown $TOMCAT_USER:$TOMCAT_USER $LOCATION/plugins || exit 1
fi
if [ ! -d $LOCATION/logs ]; then
    mkdir $LOCATION/logs
    chown $TOMCAT_USER:$TOMCAT_USER $LOCATION/logs || exit 1
fi

INSTALL_FLAG_FILE="$LOCATION/hmdm_install_flag"

# Logger configuration
cat ./install/log4j_template.xml | sed "s|_BASE_DIRECTORY_|$LOCATION|g" > $LOCATION/log4j-hmdm.xml
chown $TOMCAT_USER:$TOMCAT_USER $LOCATION/log4j-hmdm.xml

echo
echo "Please choose the directory where supply scripts will be located."
echo
if [ "$NONINTERACTIVE" == "true" ]; then
    SCRIPT_LOCATION=$DEFAULT_SCRIPT_LOCATION
else
    read -e -p "Headwind MDM scripts directory [$DEFAULT_SCRIPT_LOCATION]: " -i "$DEFAULT_SCRIPT_LOCATION" SCRIPT_LOCATION
fi
if [ ! -d $SCRIPT_LOCATION ]; then
    mkdir -p $SCRIPT_LOCATION || exit 1
fi

echo
echo "Web application setup"
echo "====================="
echo "Headwind MDM requires access from Internet"
echo "Please assign a public domain name to this server"
echo

if [ "$NONINTERACTIVE" == "true" ]; then
    PROTOCOL="https"
    BASE_DOMAIN="$CLI_HOSTNAME"
    PORT=""
    BASE_PATH="/hmdm"
    echo "Using URL: https://$BASE_DOMAIN/hmdm"
else
    read -e -p "Protocol (http|https) [$DEFAULT_PROTOCOL]: " -i "$DEFAULT_PROTOCOL" PROTOCOL
    while [ -z $BASE_DOMAIN ]; do
        read -e -p "Domain name or public IP (e.g. example.com): " -i "$DEFAULT_BASE_DOMAIN" BASE_DOMAIN
        if [ -z $BASE_DOMAIN ]; then
            echo "Please enter a non-empty domain name"
        fi
    done
    read -e -p "Port (e.g. 8080, leave empty for default ports 80 or 443): " -i "$DEFAULT_PORT" PORT
    read -e -p "Project path on server (e.g. /hmdm) or ROOT: " -i "$DEFAULT_BASE_PATH" BASE_PATH
fi

# Nobody changes it!
# read -e -p "Tomcat virtual host [$TOMCAT_HOST]: " -i "$TOMCAT_HOST" TOMCAT_HOST

# HTTPS via LetsEncrypt
echo
echo "To enable password recovery function, Headwind MDM must be connected to SMTP."
echo "Password recovery is an optional but recommended feature."
if [ "$NONINTERACTIVE" != "true" ]; then
    read -e -p "Setup SMTP credentials [Y/n]?: " -i "Y" REPLY
    if [[ "$REPLY" =~ ^[Yy]$ ]]; then
        read -e -p "E-mail of the admin account: " ADMIN_EMAIL
        read -e -p "SMTP host (e.g. smtp.gmail.com): " SMTP_HOST
        read -e -p "SMTP port (e.g. 25, 465, or 587): " SMTP_PORT
        read -e -p "Use SSL (1 - use, 0 - not use): " -i "0" SMTP_SSL
        read -e -p "Use STARTTLS (1 - use, 0 - not use): " -i "0" SMTP_STARTTLS
        read -e -p "SMTP username (leave empty if no auth required): " SMTP_USERNAME
        read -e -p "SMTP password (leave empty if no auth required): " SMTP_PASSWORD
        read -e -p "Sender e-mail address: " SMTP_FROM
    fi
fi

# Remote control (STUN/TURN) setup
echo
echo "Remote control setup (WebRTC STUN/TURN server)"
echo "================================================"
echo "STUN/TURN host defaults to the server hostname ($BASE_DOMAIN)."
echo "Override with --turn-host if your coturn server is on a different host."
if [ "$NONINTERACTIVE" == "true" ]; then
    TURN_HOST="${CLI_TURN_HOST:-$BASE_DOMAIN}"
    TURN_PORT="${CLI_TURN_PORT:-3478}"
    TURN_USER="${CLI_TURN_USER}"
    TURN_PASS="${CLI_TURN_PASS}"
    echo "Remote control STUN/TURN: $TURN_HOST:$TURN_PORT (user: $TURN_USER)"
else
    read -e -p "Setup remote control STUN/TURN server [Y/n]?: " -i "Y" REPLY
    if [[ "$REPLY" =~ ^[Yy]$ ]]; then
        TURN_HOST="$BASE_DOMAIN"
        read -e -p "STUN/TURN server port [3478]: " -i "3478" TURN_PORT
        read -e -p "TURN username: " TURN_USER
        read -e -p "TURN password: " TURN_PASS
    fi
fi

TOMCAT_DEPLOY_PATH=$BASE_PATH
if [ "$BASE_PATH" == "ROOT" ]; then
    BASE_PATH=""
fi 

if [[ ! -z "$PORT" ]]; then
    BASE_HOST="$BASE_DOMAIN:$PORT"
else
    BASE_HOST="$BASE_DOMAIN"
fi

echo
echo "Ready to install!"
echo "Location on server: $LOCATION"
echo "URL: $PROTOCOL://$BASE_HOST$BASE_PATH"
if [ "$NONINTERACTIVE" != "true" ]; then
    read -p "Is this information correct [Y/n]? " -n 1 -r
    echo
    if [[ ! "$REPLY" =~ ^[Yy]$ ]]; then
        exit 1
    fi
fi

# Prepare the XML config
if [ ! -f ./install/context_template.xml ]; then
    echo "ERROR: Missing ./install/context_template.xml!"
    echo "The package seems to be corrupted!"
    exit 1
fi

# Removing old application if required
if [ -d $TOMCAT_HOME/webapps/$TOMCAT_DEPLOY_PATH ]; then
    echo "Stopping Tomcat to safely redeploy..."
    systemctl stop tomcat9
    sleep 3
    rm -rf $TOMCAT_HOME/webapps/$TOMCAT_DEPLOY_PATH > /dev/null 2>&1
    rm -f $TOMCAT_HOME/webapps/$TOMCAT_DEPLOY_PATH.war > /dev/null 2>&1
    echo "Old application removed"
fi

TOMCAT_CONFIG_PATH=$TOMCAT_HOME/conf/$TOMCAT_ENGINE/$TOMCAT_HOST
if [ ! -d $TOMCAT_CONFIG_PATH ]; then
    mkdir -p $TOMCAT_CONFIG_PATH || exit 1
    chown root:$TOMCAT_USER $TOMCAT_CONFIG_PATH
    chmod 755 $TOMCAT_CONFIG_PATH
fi
MQTT_SERVER_URI=$(hostname -I | awk '{print $1}')
cat ./install/context_template.xml | sed "s|_SQL_HOST_|$SQL_HOST|g; s|_SQL_PORT_|$SQL_PORT|g; s|_SQL_BASE_|$SQL_BASE|g; s|_SQL_USER_|$SQL_USER|g; s|_SQL_PASS_|$SQL_PASS|g; s|_BASE_DIRECTORY_|$LOCATION|g; s|_PROTOCOL_|$PROTOCOL|g; s|_BASE_HOST_|$BASE_HOST|g; s|_BASE_DOMAIN_|$BASE_DOMAIN|g; s|_BASE_PATH_|$BASE_PATH|g; s|_INSTALL_FLAG_|$INSTALL_FLAG_FILE|g; s|_SMTP_HOST_|$SMTP_HOST|g; s|_SMTP_PORT_|$SMTP_PORT|g;  s|_SMTP_SSL_|$SMTP_SSL|g; s|_SMTP_STARTTLS_|$SMTP_STARTTLS|g; s|_SMTP_USERNAME_|$SMTP_USERNAME|g; s|_SMTP_PASSWORD_|$SMTP_PASSWORD|g; s|_SMTP_FROM_|$SMTP_FROM|g; s|_MQTT_SERVER_URI_|$MQTT_SERVER_URI|g;" > $TOMCAT_CONFIG_PATH/$TOMCAT_DEPLOY_PATH.xml
if [ "$?" -ne 0 ]; then
    echo "Failed to create a Tomcat config file $TOMCAT_CONFIG_PATH/$TOMCAT_DEPLOY_PATH.xml!"
    exit 1
fi 
echo "Tomcat config file created: $TOMCAT_CONFIG_PATH/$TOMCAT_DEPLOY_PATH.xml"
chmod 644 $TOMCAT_CONFIG_PATH/$TOMCAT_DEPLOY_PATH.xml
cp $TOMCAT_CONFIG_PATH/$TOMCAT_DEPLOY_PATH.xml $TOMCAT_CONFIG_PATH/$TOMCAT_DEPLOY_PATH.xml~

echo "Deploying $SERVER_WAR to Tomcat: $TOMCAT_HOME/webapps/$TOMCAT_DEPLOY_PATH.war"
rm -f $INSTALL_FLAG_FILE > /dev/null 2>&1
cp $SERVER_WAR $TOMCAT_HOME/webapps/$TOMCAT_DEPLOY_PATH.war
chmod 644 $TOMCAT_HOME/webapps/$TOMCAT_DEPLOY_PATH.war

# Start Tomcat if it was stopped for redeployment
if ! systemctl is-active --quiet tomcat9; then
    echo "Starting Tomcat..."
    systemctl start tomcat9
    sleep 5
fi

# Waiting until the end of deployment
SUCCESSFUL_DEPLOY=0
for i in {1..120}; do
    if [ -f $INSTALL_FLAG_FILE ]; then
        if [[ $(< $INSTALL_FLAG_FILE) == "OK" ]]; then
            SUCCESSFUL_DEPLOY=1
        else
            SUCCESSFUL_DEPLOY=0
        fi
        break
    fi
    echo -n "."
    sleep 1
done
echo
rm -f $INSTALL_FLAG_FILE > /dev/null 2>&1
if [ $SUCCESSFUL_DEPLOY -ne 1 ]; then
    echo "ERROR: failed to deploy WAR file!"
    echo "Please check $TOMCAT_HOME/logs/catalina.out for details."
    exit 1
fi
echo "Deployment successful, initializing the database..."

# Initialize database
cat ./install/sql/hmdm_init.$LANGUAGE.sql | sed "s|_HMDM_BASE_|$LOCATION|g; s|_HMDM_VERSION_|$CLIENT_VERSION|g; s|_HMDM_APK_|$CLIENT_APK|g; s|_ADMIN_EMAIL_|$ADMIN_EMAIL|g;" > $TEMP_SQL_FILE
$MYSQL_CMD < $TEMP_SQL_FILE
if [ "$?" -ne 0 ]; then
    echo "ERROR: failed to execute SQL script!"
    echo "See $TEMP_SQL_FILE for details."
    exit 1
fi
rm -f $TEMP_SQL_FILE > /dev/null 2>&1

# Configure remote control STUN/TURN settings if provided
if [ -n "$TURN_HOST" ]; then
    STUN_URL="stun:${TURN_HOST}:${TURN_PORT}"
    TURN_URL="turn:${TURN_HOST}:${TURN_PORT}"
    echo "Configuring remote control settings (STUN: $STUN_URL, TURN: $TURN_URL)..."
    echo "INSERT INTO plugin_remotecontrol_settings (customerId, stunUrl, turnUrl, turnUsername, turnPassword) \
        VALUES (1, '${STUN_URL}', '${TURN_URL}', '${TURN_USER}', '${TURN_PASS}') \
        ON DUPLICATE KEY UPDATE stunUrl='${STUN_URL}', turnUrl='${TURN_URL}', turnUsername='${TURN_USER}', turnPassword='${TURN_PASS}';" \
        | $MYSQL_CMD
    if [ "$?" -eq 0 ]; then
        echo "Remote control settings configured successfully."
    else
        echo "WARNING: Failed to configure remote control settings."
        echo "You can configure them manually in the admin panel under Remote Control > Settings."
    fi
fi

echo
echo "======================================"
echo "Minimal installation of Headwind MDM has been done!"
echo "At this step, you can open in your web browser:"
echo "http://$BASE_DOMAIN:8080$BASE_PATH"
echo "Login: admin:admin"
echo "======================================"
echo

if [ "$NONINTERACTIVE" == "true" ]; then
    REPLY="n"
else
    read -e -p "Setup HTTPS via LetsEncrypt [Y/n]?: " -i "Y" REPLY
fi

if [[ "$REPLY" =~ ^[Yy]$ ]]; then
    if ! which certbot > /dev/null; then
        apt update
        apt install -y certbot
    fi
    sed "s/DOMAIN=your-domain.com/DOMAIN=$BASE_DOMAIN/" ./letsencrypt-ssl.sh > $SCRIPT_LOCATION/letsencrypt-ssl.sh
    chmod +x $SCRIPT_LOCATION/letsencrypt-ssl.sh
    $SCRIPT_LOCATION/letsencrypt-ssl.sh

    echo
    echo "======================================"
    echo "The installer can try to update Tomcat config automatically."
    echo "Use this feature with care, ONLY IF YOU DIDN'T TOUCH server.xml"
    echo "If Tomcat won't work after update, please revert the config back:"
    echo "cp $TOMCAT_HOME/conf/server.xml~ $TOMCAT_HOME/conf/server.xml"
    echo "======================================"
    echo
    read -e -p "Update Tomcat config automatically [Y/n]?: " -i "Y" REPLY
    if [[ "$REPLY" =~ ^[Yy]$ ]]; then
        cp $TOMCAT_HOME/conf/server.xml $TOMCAT_HOME/conf/server.xml~
	# EPIC MAGIC!!!
        sed -z -e "s^<\!\-\-\n    <Connector port=\"8443\" protocol=\"org.apache.coyote.http11.Http11NioProtocol\"^<Connector port=\"8443\" protocol=\"org.apache.coyote.http11.Http11NioProtocol\"^" -e "s^\-\->\n    <\!\-\- Define an SSL/TLS HTTP/1.1 Connector on port 8443 with HTTP/2^<\!\-\- Define an SSL/TLS HTTP/1.1 Connector on port 8443 with HTTP/2^" -e "s^certificateKeystoreFile=\"conf/localhost-rsa.jks\"^certificateKeystoreFile=\"/var/lib/tomcat9/ssl/$BASE_DOMAIN.jks\" certificateKeystorePassword=\"123456\"^" $TOMCAT_HOME/conf/server.xml~ > $TOMCAT_HOME/conf/server.xml
        CERTBOT_VERSION=`certbot --version | awk '{print $2}' | awk '{n=split($1,A,"."); print A[1]}'`
        if [ "$CERTBOT_VERSION" != "" ] && [ "$CERTBOT_VERSION" -ge "2" ]; then
        # In certbot 2, default encryption is ECDSA so we need to adjust it in Tomcat config
            cp $TOMCAT_HOME/conf/server.xml $TOMCAT_HOME/conf/server.xml.1
            sed -z -e "s^type=\"RSA\" />^type=\"EC\" />^" $TOMCAT_HOME/conf/server.xml.1 > $TOMCAT_HOME/conf/server.xml
            rm -f $TOMCAT_HOME/conf/server.xml.1
        fi
        service $TOMCAT_SERVICE restart
    fi

    echo
    echo "======================================"
    echo "Secure installation of Headwind MDM has been done!"
    echo "At this step, you can open in your web browser:"
    echo "https://$BASE_DOMAIN:8443$BASE_PATH"
    echo
    echo "Notice: if Tomcat starts slowly:"
    echo "Open a file /etc/java-11-openjdk/security/java.security"
    echo "Replace securerandom.source=file:/dev/random"
    echo "to securerandom.source=file:/dev/urandom"
    echo "and restart Tomcat."
    echo "======================================"
    echo

    CERTBOT_RENEWAL=$(crontab -l | grep letsencrypt-ssl.sh)
    if [ -z "$CERTBOT_RENEWAL" ]; then 
        read -e -p "Setup regular HTTPS certificate renewal [Y/n]?: " -i "Y" REPLY
        if [[ "$REPLY" =~ ^[Yy]$ ]]; then
            crontab -l > /tmp/current-crontab
            echo "0 5 * * 1 $SCRIPT_LOCATION/letsencrypt-ssl.sh" >> /tmp/current-crontab
	    crontab /tmp/current-crontab
	    rm /tmp/current-crontab
        fi
    fi
fi

# Redirect the ports
IPTABLES_HTTPS_SET=$(/sbin/iptables -t nat --list | grep 8443)
if [ -z "$IPTABLES_HTTPS_SET" ]; then
    if [ "$NONINTERACTIVE" == "true" ]; then
        REPLY="n"
    else
        read -e -p "Use iptables to redirect port 443 to 8443 [Y/n]?: " -i "Y" REPLY
    fi
    if [[ "$REPLY" =~ ^[Yy]$ ]]; then
        cp iptables-tomcat.sh $SCRIPT_LOCATION/iptables-tomcat.sh
	chmod +x $SCRIPT_LOCATION/iptables-tomcat.sh
	$SCRIPT_LOCATION/iptables-tomcat.sh

        IPTABLES_RENEWAL=$(crontab -l | grep iptables-tomcat.sh)
	if [ -z "$IPTABLES_RENEWAL" ]; then
            crontab -l > /tmp/current-crontab
	    echo "@reboot $SCRIPT_LOCATION/iptables-tomcat.sh" >> /tmp/current-crontab
            crontab /tmp/current-crontab
	    rm /tmp/current-crontab
	fi
    fi
fi

# Download required files

# Check if the files folder already contains APKs (reinstall scenario)
if [ -d "$LOCATION/files" ]; then
    EXISTING_APKS=$(ls -1 "$LOCATION/files"/*.apk 2>/dev/null)
    if [ -n "$EXISTING_APKS" ]; then
        echo ""
        echo "The files folder already contains the following APKs:"
        echo "$EXISTING_APKS" | xargs -I{} basename {}
        echo ""
        if [ "$NONINTERACTIVE" == "true" ]; then
            WIPE_REPLY="y"
        else
            read -e -p "Do you want to wipe the existing APKs before proceeding [y/N]?: " -i "N" WIPE_REPLY
        fi
        if [[ "$WIPE_REPLY" =~ ^[Yy]$ ]]; then
            rm -f "$LOCATION/files"/*.apk
            echo "APKs removed."
        fi
        echo ""
    fi
fi

if [ "$NONINTERACTIVE" == "true" ]; then
    REPLY="Y"
else
    read -e -p "Move required APKs from h-mdm.com to your server [Y/n]?: " -i "Y" REPLY
fi
if [[ "$REPLY" =~ ^[Yy]$ ]]; then
    FILES=$(echo "SELECT url FROM applicationversions WHERE url IS NOT NULL" | $MYSQL_CMD 2>/dev/null | tail -n +2)
    CURRENT_DIR=$(pwd)
    cd $LOCATION/files
    for FILE in $FILES; do
        echo "Downloading $FILE..."
	wget $FILE
    done
    
    # Copy install/apk
    if [ -d "$CURRENT_DIR/install/apk" ]; then
        for APK_PATH in "$CURRENT_DIR/install/apk"/*.apk; do
            if [ -f "$APK_PATH" ]; then
                APK_FILE=$(basename "$APK_PATH")
                echo "Copying $APK_FILE..."
                cp "$APK_PATH" .
                
                if [ "$APK_FILE" == "org.glpi.inventory.agent.apk" ]; then
                    APK_URL="$PROTOCOL://$BASE_HOST$BASE_PATH/files/$APK_FILE"
                    echo "UPDATE applicationversions SET url='$APK_URL' WHERE applicationid=100;" | $MYSQL_CMD 2>/dev/null
                    echo "Set URL for GLPI Agent"
                fi
            fi
        done
    fi
    
    chown $TOMCAT_USER:$TOMCAT_USER *
    chmod 644 *.apk 2>/dev/null
    
    # Copy hmdm to /android and create latest symlink
    HMDM_APK=$(ls -1 hmdm-*.apk 2>/dev/null | head -1)
    if [ ! -z "$HMDM_APK" ]; then
        PULSE_DIR="/var/lib/pulse2/clients/android"
        mkdir -p "$PULSE_DIR" 2>/dev/null
        chown syncthing:syncthing "$PULSE_DIR" 2>/dev/null
        chmod 775 "$PULSE_DIR" 2>/dev/null
        # Copy rights from the win folder
        if [ -d "/var/lib/pulse2/clients/win" ] && which getfacl > /dev/null && which setfacl > /dev/null; then
            getfacl /var/lib/pulse2/clients/win 2>/dev/null | setfacl --set-file=- "$PULSE_DIR" 2>/dev/null
        fi
        echo "Copying $HMDM_APK to $PULSE_DIR..."
        cp "$HMDM_APK" "$PULSE_DIR/"
        cd "$PULSE_DIR"
        chmod 644 "$HMDM_APK"
        rm -f hmdm-latest.apk
        ln -s "$HMDM_APK" hmdm-latest.apk
        echo "Created symlink: hmdm-latest.apk -> $HMDM_APK"
        cd $LOCATION/files
    fi
    
    echo "UPDATE applicationversions SET url=REPLACE(url, 'https://h-mdm.com', '$PROTOCOL://$BASE_HOST$BASE_PATH') WHERE url IS NOT NULL;" | $MYSQL_CMD >/dev/null 2>&1
    cd $CURRENT_DIR
fi

echo
echo "======================================"
echo "Headwind MDM installation is completed!"
echo "To access your web panel, open in the web browser:"
echo "$PROTOCOL://$BASE_HOST$BASE_PATH"
echo "Login: admin:admin"
echo "======================================"
echo

# GLPI Agent auto-configuration
if [ "$NONINTERACTIVE" == "true" ]; then
    GLPI_REPLY="Y"
else
    read -e -p "Configure GLPI Agent automatically? [Y/n]: " -i "Y" GLPI_REPLY
fi
if [[ "$GLPI_REPLY" =~ ^[Yy]$ ]]; then
    GLPI_APP_ID=$(echo "SELECT id FROM applications WHERE pkg='org.glpi.inventory.agent' LIMIT 1;" | $MYSQL_CMD -N -B 2>/dev/null)
    if [ -z "$GLPI_APP_ID" ]; then
        echo "WARNING: GLPI Agent not found in the database. Skipping."
    else
        GLPI_URL="$PROTOCOL://$BASE_HOST/glpi/front/inventory.php"
        GLPI_TS=$(date +%s)000
        echo "INSERT INTO configurationapplicationsettings (extRefId, applicationId, name, type, value, comment, readonly, lastUpdate) VALUES
(1, $GLPI_APP_ID, 'server_configuration_list[0].server_url',     'STRING',  '$GLPI_URL', NULL, FALSE, $GLPI_TS),
(1, $GLPI_APP_ID, 'server_configuration_list[0].server_itemtype','STRING',  'Phone',  NULL, FALSE, $GLPI_TS),
(1, $GLPI_APP_ID, 'frequency',                                    'STRING',  'Day',       NULL, FALSE, $GLPI_TS),
(1, $GLPI_APP_ID, 'automatic_inventory',                          'BOOLEAN', 'true',      NULL, FALSE, $GLPI_TS);" | $MYSQL_CMD
        echo "GLPI Agent configured: $GLPI_URL"
    fi
fi

# Admin password setup
if [ "$NONINTERACTIVE" == "true" ]; then
    if [ -n "$CLI_ADMIN_PASS" ]; then
        ADMIN_PASS_MD5=$(echo -n "$CLI_ADMIN_PASS" | md5sum | awk '{print $1}' | tr '[:lower:]' '[:upper:]')
        ADMIN_PASS_HASH=$(echo -n "${ADMIN_PASS_MD5}5YdSYHyg2U" | sha1sum | awk '{print $1}' | tr '[:lower:]' '[:upper:]')
        echo "UPDATE users SET password='$ADMIN_PASS_HASH', passwordReset=0, passwordResetToken=NULL WHERE id=1;" | $MYSQL_CMD
        echo "Admin password set from --admin-pass."
    else
        echo "No --admin-pass provided; leaving default admin:admin password."
    fi
else
    read -e -p "Set the admin password now? [Y/n]: " -i "Y" ADMIN_PASS_REPLY
    if [[ "$ADMIN_PASS_REPLY" =~ ^[Yy]$ ]]; then
        while true; do
            read -s -p "New admin password: " ADMIN_PASS_1; echo
            read -s -p "Confirm password: " ADMIN_PASS_2; echo
            if [ "$ADMIN_PASS_1" == "$ADMIN_PASS_2" ] && [ -n "$ADMIN_PASS_1" ]; then
                break
            fi
            echo "Passwords do not match or are empty, please try again."
        done
        ADMIN_PASS_MD5=$(echo -n "$ADMIN_PASS_1" | md5sum | awk '{print $1}' | tr '[:lower:]' '[:upper:]')
        ADMIN_PASS_HASH=$(echo -n "${ADMIN_PASS_MD5}5YdSYHyg2U" | sha1sum | awk '{print $1}' | tr '[:lower:]' '[:upper:]')
        echo "UPDATE users SET password='$ADMIN_PASS_HASH', passwordReset=0, passwordResetToken=NULL WHERE id=1;" | $MYSQL_CMD
        echo "Admin password updated."
    fi
fi