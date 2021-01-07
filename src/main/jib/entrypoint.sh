#!/bin/sh

die () {
  echo $@ >&2
  exit 1
}

[ -z "$SOLOPOOL_DAEMON" ] && die SOLOPOOL_DAEMON unset
[ -z "$SOLOPOOL_WALLET" ] && die SOLOPOOL_WALLET unset

SOLOPOOL_ALGO=${SOLOPOOL_ALGO-rx/0}
SOLOPOOL_HTTP_PORT=${SOLOPOOL_HTTP_PORT-8000}
SOLOPOOL_POLL_INTERVAL=${SOLOPOOL_POLL_INTERVAL-10}
SOLOPOOL_PORT=${SOLOPOOL_PORT-3000}

CONFIG_FILE=/app/config.properties

cat <<EOF >$CONFIG_FILE
com.moneromint.solo.algo=$SOLOPOOL_ALGO
com.moneromint.solo.daemon=$SOLOPOOL_DAEMON
com.moneromint.solo.daemon.poll-interval=$SOLOPOOL_POLL_INTERVAL
com.moneromint.solo.http.port=$SOLOPOOL_HTTP_PORT
com.moneromint.solo.port=$SOLOPOOL_PORT
com.moneromint.solo.wallet=$SOLOPOOL_WALLET
EOF

java -cp /app/resources:/app/classes:/app/libs/* com.moneromint.solo.Main $CONFIG_FILE
