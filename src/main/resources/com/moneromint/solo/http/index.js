const STATS_URI = 'stats.json'
const UPDATE_INTERVAL = 2500

function updateStats({$hashrate, $connections, $validShares, $invalidShares, $blocksFound}) {
    console.log('updating stats...')
    return fetch(STATS_URI)
        .then(r => r.json())
        .then(data => {
            $hashrate.innerText = data['hashrate']
            $connections.innerText = data['connections']
            $validShares.innerText = data['validShares']
            $invalidShares.innerText = data['invalidShares']
            $blocksFound.innerText = data['blocksFound']
        })
}

document.addEventListener('DOMContentLoaded', () => {
    const $hashrate = document.getElementById('hashrate')
    const $connections = document.getElementById('connections')
    const $validShares = document.getElementById('validShares')
    const $invalidShares = document.getElementById('invalidShares')
    const $blocksFound = document.getElementById('blocksFound')
    const elems = {$hashrate, $connections, $validShares, $invalidShares, $blocksFound}
    const doUpdate = updateStats.bind(undefined, elems)

    doUpdate()

    let interval = setInterval(doUpdate, UPDATE_INTERVAL)

    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') {
            interval = interval || setInterval(doUpdate, UPDATE_INTERVAL)
        } else {
            clearInterval(interval)
            interval = null
        }
    })
})
