package wo.lf.lifx.api

import io.reactivex.disposables.CompositeDisposable
import wo.lf.lifx.domain.LifxMessagePayload
import wo.lf.lifx.domain.StateDeviceChain
import wo.lf.lifx.domain.StateTileState64
import wo.lf.lifx.extensions.fireAndForget

class TileManager(
        private val wrappedChangeDispatcher: ILightsChangeDispatcher
) : ILightServiceExtension<LifxMessage<LifxMessagePayload>>, ILightsChangeDispatcher {

    private val disposables = CompositeDisposable()

    private val tiles: MutableMap<Long, Light> = mutableMapOf()

    override fun start(source: ILightSource<LifxMessage<LifxMessagePayload>>) {
        disposables.add(source.tick.subscribe {
            tiles.forEach { (id, tile) ->
                TileGetTileState64Command.create(tile).fireAndForget()
            }
        })

        disposables.add(source.messages.filter { tiles.containsKey(it.message.header.target) }.subscribe { message ->
            when (message.message.payload) {
                is StateDeviceChain -> {
                    println(message.message.payload)
                }
                is StateTileState64 -> {
                    println(message.message.payload)
                }
            }
        })
    }

    override fun stop() {
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
            tiles[light.id] = light
            TileGetDeviceChainCommand.create(light).fireAndForget()
            TileGetTileState64Command.create(light).fireAndForget()
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