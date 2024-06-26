package geofaas

import de.hasenburg.geobroker.commons.model.disgb.BrokerInfo
import de.hasenburg.geobroker.commons.model.spatial.Location
import de.hasenburg.geobroker.commons.model.spatial.toGeofence
import de.hasenburg.geobroker.commons.sleepNoLog
import org.apache.logging.log4j.LogManager
import kotlin.system.measureTimeMillis
import geofaas.Model.FunctionMessage
import geofaas.Model.StatusCode
import geofaas.Model.RequestID
import geofaas.experiment.Commons.brokerAddresses
import geofaas.experiment.Commons.clientLoc
import geofaas.experiment.Commons.locBerlinToFrance
import geofaas.experiment.Commons.locFranceToPoland
import geofaas.experiment.Commons.locFrankParisBerlin
import geofaas.experiment.Measurement

class Client(loc: Location, debug: Boolean, host: String, port: Int,
             id: String = "ClientGeoFaaS1", ackTimeout: Int = 8000, resTimeout: Int = 4000
    ) {
    private val logger = LogManager.getLogger()
    private val gbClient = ClientGBClient(loc, debug, host, port, id, ackTimeout, resTimeout)
    private val radius = 0.001
    val id
        get() = gbClient.id

    fun moveTo(dest: Pair<String, Location>, reqId: RequestID): Pair<StatusCode, BrokerInfo?> {
        val status: Pair<StatusCode, BrokerInfo?>
        val elapsed = measureTimeMillis { status = gbClient.updateLocation(dest.second) }
        val newBroker = if (status.second == null) "sameBroker" else status.second!!.brokerId
        Measurement.log(id, elapsed,"Moved", newBroker, reqId)
        return status
    }

    // returns a pair of result and run time
    fun call(funcName: String, param: String, reqId: RequestID, retries: Int = 0, ackAttempts: Int = 1, ackTArg: Int? = null, resTArg: Int? = null, isWithCloudRetry: Boolean = true, isContinousCall: Boolean = false): Pair<FunctionMessage?, Long> {
        val result: FunctionMessage?
        val elapsed = measureTimeMillis {
            result = gbClient.callFunction(funcName, param, retries, ackAttempts,ackTArg, resTArg, radius, reqId, isWithCloudRetry, isContinousCall)
        }
        if (result == null) {
            logger.error("No result received after {} retries! {}ms", retries, elapsed)
            Measurement.log(id, elapsed,"RESULT;NoResult", "$retries retries! ${elapsed}ms", reqId)
        }

        return Pair(result, elapsed)
    }

    fun shutdown() {
        gbClient.terminate()
    }

    fun throwSafeException(msg: String) {
        gbClient.throwSafeException(msg) // also runs terminate
    }
}

fun main() {

    val debug = false
//    val client = Client(locFranceToPoland.first().second, debug, brokerAddresses["Frankfurt"]!!, 5560, "client1")
//    val res: String? = client.call("sieve", client.id)
//    if(res != null) println("${client.id} Result: $res")
//    else println("${client.id}: NOOOOOOOOOOOOOOO Response!")
//    client.shutdown()
//    println("${client.id} finished!")

    /////////////////2 local nodes//////
    val client1 = Client(clientLoc[3].second, debug, brokerAddresses["Local"]!!, 5560, "client1")
    Measurement.log(client1.id, -1, "Started at", clientLoc[3].first, null)
    sleepNoLog(2000, 0)
    val reqId1 = RequestID(1, "client1", clientLoc[3].first)
    val res: Pair<FunctionMessage?, Long> = client1.call("sieve", "", reqId1)
    if(res.first != null) Measurement.log("client1", res.second, "Done", clientLoc[3].second.distanceKmTo(res.first!!.responseTopicFence.fence.toGeofence().center).toString(), reqId1) // misc shows distance in km in Double format
    else client1.throwSafeException("client1-(1-${clientLoc[3].first}): NOOOOOOOOOOOOOOO Response! (${res.second}ms)")
    sleepNoLog(2000, 0)

    val reqId2 = RequestID(2, "client1", clientLoc[8].first)
    client1.moveTo(clientLoc[8], reqId2)
    sleepNoLog(2000, 0)
    val res2: Pair<FunctionMessage?, Long> = client1.call("sieve", "", reqId2)
    if(res2.first != null)  Measurement.log("client1", res2.second, "Done", clientLoc[3].second.distanceKmTo(res2.first!!.responseTopicFence.fence.toGeofence().center).toString(), reqId2) // misc shows distance in km in Double format
    else client1.throwSafeException("client1-(2-${clientLoc[8].first}): NOOOOOOOOOOOOOOO Response! (${res2.second}ms)")
    sleepNoLog(2000, 0)
    client1.shutdown()
    Measurement.close()

}