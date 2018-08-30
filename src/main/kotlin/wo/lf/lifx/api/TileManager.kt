/*

Copyright 2018 Florian Sprenger

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*/


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

interface ITileManagerListener {
    fun tileAdded(tile: TileLight)
    fun tileUpdated(tile: TileLight, chain: List<TileDevice>)
}

data class TileLight(
        val light: Light
) {
    var chain: List<TileDevice> = listOf()
}

class TileManager(
        private val wrappedChangeDispatcher: ILightsChangeDispatcher
) : ILightServiceExtension<LifxMessage<LifxMessagePayload>>, ILightsChangeDispatcher {

    private var listeners: Set<ITileManagerListener> = setOf()

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
                            listeners.forEach { it.tileUpdated(tile, tile.chain) }
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
                                    val messageIndex = payload.width * (row - payload.y) + (column - payload.x)
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

                        // 360 -> (Short.MAX_VALUE.toInt() * 2)
                        // 0 -> 0

                        // x -> f    x * (Short.MAX_VALUE.toInt() * 2) / 360
                        val ncolors = List(64) { HSBK((it * (Short.MAX_VALUE.toInt() * 2) / 64).toShort(), (Short.MAX_VALUE.toInt() * 2).toShort(), Short.MAX_VALUE, 0) }
                        TileSetTileState64Command.create(
                                tileManager = this,
                                light = tile.light,
                                tileIndex = payload.tile_index.toInt(),
                                colors = ncolors
                        ).fireAndForget()

                        listeners.forEach { it.tileUpdated(tile, listOf(device)) }
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
            val tile = TileLight(light)
            tiles[light.id] = tile
            TileGetDeviceChainCommand.create(light).fireAndForget()
            TileGetTileState64Command.create(light).fireAndForget()
            // dispatch tile added
            listeners.forEach { it.tileAdded(tile) }
        }
    }

    override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
        if (property == LightProperty.ProductInfo && (newValue as ProductInfo).hasTileSupport) {
            trackTile(light)
        }
        wrappedChangeDispatcher.onLightChange(light, property, oldValue, newValue)
    }

    fun addListener(listener: ITileManagerListener) {
        listeners = listeners.plus(listener)
    }

    fun removeListener(listener: ITileManagerListener) {
        listeners = listeners.minus(listener)
    }

    companion object : ILightServiceExtensionFactory<LifxMessage<LifxMessagePayload>> {
        override fun create(changeDispatcher: ILightsChangeDispatcher): ILightServiceExtension<LifxMessage<LifxMessagePayload>> {
            return TileManager(changeDispatcher)
        }

    }
}