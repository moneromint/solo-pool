# solo-pool

Solo mining pool for Monero.

## Container Image

Pre-built container images are available on Docker Hub. Example usage:

```sh
docker run \
    --rm \
    -d \
    -e SOLOPOOL_DAEMON=http://1.2.3.4:18081/json_rpc \
    -e SOLOPOOL_WALLET=YOUR_WALLET_ADDRESS_HERE \
    -e SOLOPOOL_PORT=3000 \
    -e SOLOPOOL_HTTP_PORT=8000 \
    -p 3000:3000 \
    -p 8000:8000 \
    moneromint/solo-pool
```

## Deployment With docker-compose

An example deployment of solo-pool, Prometheus, and Grafana is
available in the
[moneromint/solo-pool-deployment](https://github.com/moneromint/solo-pool-deployment)
repository.

## License

The code in this repository is released under the terms of the MIT license.
See LICENSE file in project root for more info.
