import wo.lf.lifx.api.*
import wo.lf.lifx.domain.HSBK
import wo.lf.lifx.extensions.fireAndForget

fun main(args: Array<String>) {

    val lightSource = LightService(clientChangeDispatcher = object : ILightsChangeDispatcher {
        override fun onLightAdded(light: Light) {
            println("light added : ${light.id}")
        }

        override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
            println("light ${light.id} changed $property from $oldValue to $newValue")
        }
    }, extensionFactories = listOf(TileService, LocationGroupService)).apply { start() }

    lightSource.extensionOf(TileService::class)?.let { tileManager ->
        tileManager.addListener(object : ITileServiceListener {


            override fun tileAdded(tile: TileLight) {

            }

            override fun chainUpdated(tile: TileLight) {
                val colors = List(64) { HSBK((it * (Short.MAX_VALUE.toInt() * 2) / 64).toShort(), (Short.MAX_VALUE.toInt() * 2).toShort(), Short.MAX_VALUE, 0) }

                for (index in 0 until tile.chain.size) {
                    TileSetTileState64Command.create(
                            tileService = tileManager,
                            light = tile.light,
                            tileIndex = index,
                            colors = colors
                    ).fireAndForget()
                }
            }

            override fun deviceUpdated(tile: TileLight, device: TileDevice) {
                val degree = Math.random() * 360
                val colors = List(64) { HSBK((degree * (Short.MAX_VALUE.toInt() * 2) / 360).toShort(), (Short.MAX_VALUE.toInt() * 2).toShort(), Short.MAX_VALUE, 0) }

                TileSetTileState64Command.create(
                        light = tile.light,
                        tileIndex = device.index,
                        colors = colors
                ).fireAndForget()
            }

        })
    }

    lightSource.extensionOf(LocationGroupService::class)?.let { locationGroupManager ->
        locationGroupManager.addListener(object : IGroupLocationChangeListener {
            override fun locationAdded(newLocation: Location) {
                println("location added ${newLocation.name}")
            }

            override fun groupAdded(location: Location, group: Group) {
                println("group added ${group.name}")
            }

            override fun locationGroupChanged(location: Location, group: Group, light: Light) {
                println("location group added ${location.name} ${group.name} ${light.label}")
            }

            override fun groupRemoved(location: Location, group: Group) {
                println("group removed ${group.name}")
            }

            override fun locationRemoved(location: Location) {
                println("location removed ${location.name}")
            }

        })

    }

    while (true) {
    }
    println("done")
}