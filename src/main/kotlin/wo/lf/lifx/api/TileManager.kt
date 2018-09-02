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

class TileDevice(
        val index: Int,
        var user_x: Float,
        var user_y: Float,
        var width: Byte,
        var height: Byte,
        val colors: Array<HSBK>
)

interface ITileManagerListener {
    fun tileAdded(tile: TileLight)
    fun chainUpdated(tile: TileLight)
    fun deviceUpdated(tile: TileLight, device: TileDevice)
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

    private val tilesById: MutableMap<Long, TileLight> = mutableMapOf()

    val tiles: List<TileLight>
        get() = tilesById.values.toList()

    override fun start(source: ILightSource<LifxMessage<LifxMessagePayload>>) {
        disposables.add(source.tick.subscribe {
            tilesById.forEach { (id, tile) ->
                TileGetTileState64Command.create(tile.light).fireAndForget()
            }
        })

        disposables.add(source.messages.filter { tilesById.containsKey(it.message.header.target) }.observeOn(source.observeScheduler).subscribe { message ->
            val payload = message.message.payload
            when (payload) {
                is StateDeviceChain -> {
                    var tileChanged = false
                    val tile = tilesById[message.message.header.target]
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
                                        existingDevice.apply {
                                            user_x = device.user_x
                                            user_y = device.user_y
                                            width = device.width
                                            height = device.height
                                        }
                                    } else {
                                        existingDevice
                                    }
                                } else {
                                    tileChanged = true
                                    TileDevice(
                                            index = index,
                                            user_x = device.user_x,
                                            user_y = device.user_y,
                                            width = device.width,
                                            height = device.height,
                                            colors = Array(device.width * device.height) { Lifx.defaultColor }
                                    )
                                }
                            } else {
                                tileChanged = true
                                tile.chain.getOrNull(index) ?: TileDevice(
                                        index = index,
                                        user_x = 0f,
                                        user_y = 0f,
                                        width = 0,
                                        height = 0,
                                        colors = arrayOf()
                                )
                            }
                        }

                        if (tileChanged) {
                            tile.chain = devices
                            // dispatch change
                            listeners.forEach { it.chainUpdated(tile) }
                        }
                    }
                }
                is StateTileState64 -> {
                    val tile = tilesById[message.message.header.target]
                    if (tile != null && tile.chain.size > payload.tile_index) {
                        val device = tile.chain[payload.tile_index.toInt()]
                        updateTile(tile, device, payload.x.toInt(), payload.y.toInt(), payload.width.toInt(), payload.colors)
                    }
                }
            }
        })
    }

    internal fun updateTile(tile: TileLight, device: TileDevice, setX: Int, setY: Int, width: Int, colors: Array<HSBK>) {
        var colorsChanged = false
        for (x in setX until Math.min(8, setX + width)) {
            for (y in setY until Math.min(8, setY + 64 / width)) {
                val existingColor = device.colors[y * 8 + x]
                val newColor = colors[(y - setY) * width + x - setX]
                if (existingColor != newColor) {
                    device.colors[y * 8 + x] = newColor
                    colorsChanged = true
                }
            }
        }

        if (colorsChanged) {
            listeners.forEach { it.deviceUpdated(tile, device) }
        }
    }

    override fun stop() {
        tilesById.clear()
        disposables.clear()
    }

    override fun onLightAdded(light: Light) {
        if (light.productInfo.hasTileSupport) {
            trackTile(light)
        }
        wrappedChangeDispatcher.onLightAdded(light)
    }

    private fun trackTile(light: Light) {
        if (!tilesById.containsKey(light.id)) {
            val tile = TileLight(light)
            tilesById[light.id] = tile
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