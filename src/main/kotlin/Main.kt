import wo.lf.lifx.net.UdpTransport
import wo.lf.lifx.api.ILightsChangeDispatcher
import wo.lf.lifx.api.Light
import wo.lf.lifx.api.LightProperty
import wo.lf.lifx.api.LightService

fun main(args: Array<String>) {

    val lightSource = LightService(UdpTransport, object : ILightsChangeDispatcher {
        override fun onLightAdded(light: Light) {
            println("light added : ${light.id}")
        }

        override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
            println("light ${light.id} changed $property from $oldValue to $newValue")
        }
    }).apply { start() }

    while (true) {
    }
    println("done")
}