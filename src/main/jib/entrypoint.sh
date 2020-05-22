#!/bin/sh

die () {
  echo $@ >&2
  exit 1
}

[ -z "$SOLOPOOL_DAEMON" ] && die SOLOPOOL_DAEMON unset
[ -z "$SOLOPOOL_WALLET" ] && die SOLOPOOL_WALLET unset

SOLOPOOL_PORT=${SOLOPOOL_PORT-3000}

CONFIG_FILE=/app/config.properties

echo com.moneromint.solo.daemon="$SOLOPOOL_DAEMON" > $CONFIG_FILE
echo com.moneromint.solo.port="$SOLOPOOL_PORT" >> $CONFIG_FILE
echo com.moneromint.solo.wallet="$SOLOPOOL_WALLET" >> $CONFIG_FILE

java -cp /app/resources:/app/classes:/app/libs/* com.moneromint.solo.Main $CONFIG_FILE
