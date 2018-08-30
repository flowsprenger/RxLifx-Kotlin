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
    }, extensionFactories = listOf(TileManager)).apply { start() }

    lightSource.extensionOf(TileManager::class)?.let { tileManager ->
        tileManager.addListener(object : ITileManagerListener {


            override fun tileAdded(tile: TileLight) {

            }

            override fun chainUpdated(tile: TileLight) {
                val colors = List(64) { HSBK((it * (Short.MAX_VALUE.toInt() * 2) / 64).toShort(), (Short.MAX_VALUE.toInt() * 2).toShort(), Short.MAX_VALUE, 0) }

                for (index in 0 until tile.chain.size) {
                    TileSetTileState64Command.create(
                            tileManager = tileManager,
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
                        tileManager = tileManager,
                        light = tile.light,
                        tileIndex = device.index,
                        colors = colors
                ).fireAndForget()
            }

        })
    }

    while (true) {
    }
    println("done")
}