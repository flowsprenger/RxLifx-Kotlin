package wo.lf.lifx.api

import io.reactivex.disposables.CompositeDisposable
import wo.lf.lifx.domain.*
import wo.lf.lifx.extensions.fireAndForget

data class TileDevice(
        val user_x: Float,
        val user_y: Float,
        val width: Byte,
        val height: Byte,
        val colors: List<HSBK>
)

data class TileLight(
        val light: Light
) {
    var chain: List<TileDevice> = listOf()
}

class TileManager(
        private val wrappedChangeDispatcher: ILightsChangeDispatcher
) : ILightServiceExtension<LifxMessage<LifxMessagePayload>>, ILightsChangeDispatcher {

    private val disposables = CompositeDisposable()

    private val tiles: MutableMap<Long, TileLight> = mutableMapOf()

    override fun start(source: ILightSource<LifxMessage<LifxMessagePayload>>) {
        disposables.add(source.tick.subscribe {
            tiles.forEach { (id, tile) ->
                TileGetTileState64Command.create(tile.light).fireAndForget()
            }
        })

        disposables.add(source.messages.filter { tiles.containsKey(it.message.header.target) }.observeOn(source.observeScheduler).subscribe { message ->
            val payload = message.message.payload
            when (payload) {
                is StateDeviceChain -> {
                    var tileChanged = false
                    val tile = tiles[message.message.header.target]
                    if (tile != null) {
                        val devices = (0 until payload.total_count).map { index ->
                            val messageIndex = index - payload.start_index
                            if (messageIndex > -1 && messageIndex < 16) {
                                val device = payload.tile_devices[messageIndex]
                                val existingDevice = tile.chain.getOrNull(index)
                                if (existingDevice != null) {
                                    if (existingDevice.user_x != device.user_x
                                            || existingDevice.user_y != device.user_y
                                            || existingDevice.width != device.width
                                            || existingDevice.height != device.height) {

                                        tileChanged = true
                                        existingDevice.copy(
                                                user_x = device.user_x,
                                                user_y = device.user_y,
                                                width = device.width,
                                                height = device.height
                                        )
                                    } else {
                                        existingDevice
                                    }
                                } else {
                                    tileChanged = true
                                    TileDevice(
                                            user_x = device.user_x,
                                            user_y = device.user_y,
                                            width = device.width,
                                            height = device.height,
                                            colors = List(device.width * device.height) { Lifx.defaultColor }
                                    )
                                }
                            } else {
                                tileChanged = true
                                tile.chain.getOrNull(index) ?: TileDevice(
                                        user_x = 0f,
                                        user_y = 0f,
                                        width = 0,
                                        height = 0,
                                        colors = listOf()
                                )
                            }
                        }
                        tile.chain = devices
                        if (tileChanged) {
                            // dispatch change
                        }
                    }
                }
                is StateTileState64 -> {
                    val tile = tiles[message.message.header.target]
                    if (tile != null && tile.chain.size > payload.tile_index) {
                        val device = tile.chain[payload.tile_index.toInt()]
                        val colors = device.colors.mapIndexed { index, hsbk ->
                            val column = index % device.width
                            val row = index / device.height

                            if (column >= payload.x && column < payload.x + payload.width) {
                                if (row >= payload.y && row < payload.y + 64 / payload.width) {
                                    val messageIndex = (column - payload.x) * payload.width + (row - payload.y)
                                    payload.colors[messageIndex]
                                } else {
                                    hsbk
                                }
                            } else {
                                hsbk
                            }
                        }
                        tile.chain = tile.chain.mapIndexed { index, tileDevice ->
                            if (index == payload.tile_index.toInt()) {
                                tileDevice.copy(colors = colors)
                            } else {
                                tileDevice
                            }
                        }
                    }
                }
            }
        })
    }

    override fun stop() {
        // do we also need to reset tiles?
        disposables.clear()
    }

    override fun onLightAdded(light: Light) {
        if (light.productInfo.hasTileSupport) {
            trackTile(light)
        }
        wrappedChangeDispatcher.onLightAdded(light)
    }

    private fun trackTile(light: Light) {
        if (!tiles.containsKey(light.id)) {
            tiles[light.id] = TileLight(light)
            TileGetDeviceChainCommand.create(light).fireAndForget()
            TileGetTileState64Command.create(light).fireAndForget()
            // dispatch tile added
        }
    }

    override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
        if (property == LightProperty.ProductInfo && (newValue as ProductInfo).hasTileSupport) {
            trackTile(light)
        }
        wrappedChangeDispatcher.onLightChange(light, property, oldValue, newValue)
    }

    companion object : ILightServiceExtensionFactory<LifxMessage<LifxMessagePayload>> {
        override fun create(changeDispatcher: ILightsChangeDispatcher): ILightServiceExtension<LifxMessage<LifxMessagePayload>> {
            return TileManager(changeDispatcher)
        }

    }
}