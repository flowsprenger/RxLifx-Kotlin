import wo.lf.lifx.api.*

fun main(args: Array<String>) {

    val lightSource = LightService(clientChangeDispatcher = object : ILightsChangeDispatcher {
        override fun onLightAdded(light: Light) {
            println("light added : ${light.id}")
        }

        override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
            println("light ${light.id} changed $property from $oldValue to $newValue")
        }
    }, extensionFactories = listOf(TileManager)).apply { start() }

    while (true) {
    }
    println("done")
}