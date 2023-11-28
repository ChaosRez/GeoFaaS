package geofaas.experiment

import de.hasenburg.geobroker.commons.model.spatial.toGeofence
import de.hasenburg.geobroker.commons.sleepNoLog
import geofaas.Client
import geofaas.Model.FunctionMessage
import geofaas.Model.RequestID
import kotlin.system.measureTimeMillis

val locations = Commons.locScenario1
fun main() {
    val client = Client(locations.first().second, Commons.debug,
        Commons.brokerAddresses["BerlinW"]!!, 5560, "C1Scenario1")
    Measurement.log(client.id, -1, "Started at", locations.first().first, null)

    val elapsed = measureTimeMillis {
        locations.forEachIndexed { i, loc ->
            val reqId = RequestID(i, client.id, loc.first)

            if (i > 0)
                client.moveTo(loc, reqId)
            val res: Pair<FunctionMessage?, Long> = client.call("sieve",  "", reqId)
            // Note: call's run time also depends on number of the retries
            if(res.first != null) Measurement.log(client.id, res.second, "Done", loc.second.distanceKmTo(res.first!!.responseTopicFence.fence.toGeofence().center).toString(), reqId) // misc shows distance in km in Double format
            else client.throwSafeException("${client.id}-($i-${loc.first}): NOOOOOOOOOOOOOOO Response! (${res.second}ms)")
        }
    }

    client.shutdown()
    Measurement.log(client.id, elapsed, "Finished",
        "${locations.size} total locations", null)
    Measurement.close()
}