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
import io.reactivex.disposables.Disposable
import io.reactivex.flowables.GroupedFlowable
import wo.lf.lifx.api.Light.Companion.CLIENT_CHANGE_TIMEOUT
import wo.lf.lifx.api.Light.Companion.defaultGroup
import wo.lf.lifx.api.Light.Companion.defaultLocation
import wo.lf.lifx.domain.*
import wo.lf.lifx.extensions.broadcastAddress
import wo.lf.lifx.extensions.capture
import wo.lf.lifx.extensions.fireAndForget
import wo.lf.lifx.net.SourcedLifxMessage
import java.net.InetAddress
import java.util.*

enum class LightProperty {
    Label,
    Color,
    Power,
    Reachable,
    Group,
    Location,
    HostFirmware,
    WifiFirmware,
    ProductInfo,
    InfraredBrightness,
    Zones
}

data class FirmwareVersion(val build: Long, val version: Int) {
    companion object {
        fun fromHostFirmwareVersion(version: StateHostFirmware): FirmwareVersion {
            return FirmwareVersion(version.build, version.version)
        }

        fun fromWifiFirmwareVersion(version: StateWifiFirmware): FirmwareVersion {
            return FirmwareVersion(version.build, version.version)
        }

        val default = FirmwareVersion(0L, 0)
    }
}

data class ProductInfo(val vendorId: Int, val productId: Int, val version: Int) {
    companion object {
        fun fromStateVersion(version: StateVersion): ProductInfo {
            return ProductInfo(version.vendor, version.product, version.version)
        }

        val default = ProductInfo(0, 0, 0)

        val productsSupportingMultiZone = listOf(0, 31, 32, 38)
        val productsSupportingInfrared = listOf(0, 29, 30, 45, 46)
    }

    val hasMultiZoneSupport: Boolean
        get() {
            return productsSupportingMultiZone.contains(productId)
        }

    val hasInfraredSupport: Boolean
        get() {
            return productsSupportingInfrared.contains(productId)
        }
}

data class DefaultLightState(
        val address: InetAddress = broadcastAddress,
        val lastSeenAt: Long = 0L,
        val reachable: Boolean = false,
        val label: String = "",
        val color: HSBK = Lifx.defaultColor,
        val power: PowerState = PowerState.OFF,
        val group: StateGroup = defaultGroup,
        val location: StateLocation = defaultLocation,
        val hostFirmware: FirmwareVersion = FirmwareVersion.default,
        val wifiFirmware: FirmwareVersion = FirmwareVersion.default,
        val productInfo: ProductInfo = ProductInfo.default,
        val infraredBrightness: Short = 0,
        val zones: Zones = Zones(count = 0, colors = listOf())
)

sealed class LifxEntity {
    abstract val entityId: Int
    abstract val lights: List<Light>
}

class Location(val id: Array<Byte>) : LifxEntity() {

    override val entityId: Int by lazy { Arrays.hashCode(id) }

    internal val groupsById: MutableMap<Array<Byte>, Group> = mutableMapOf()

    val groups: List<Group>
        get() = groupsById.values.toList()

    override val lights: List<Light>
        get() = groupsById.values.fold(listOf()) { acc, group -> acc.plus(group.lights) }

    val name
        get() = groups.flatMap { it.lights }.fold(Light.defaultLocation) { newestLocation, light ->
            if (light.location.updated_at > newestLocation.updated_at) {
                light.location
            } else {
                newestLocation
            }
        }.name
}

class Group(val id: Array<Byte>) : LifxEntity() {
    override val entityId: Int by lazy { Arrays.hashCode(id) }

    override var lights: List<Light> = listOf()
        internal set

    val name
        get() = lights.fold(Light.defaultGroup) { newestGroup, light ->
            if (light.group.updated_at > newestGroup.updated_at) {
                light.group
            } else {
                newestGroup
            }
        }.name


}

class Light(val id: Long, var source: ILightSource<LifxMessage<LifxMessagePayload>>, sourceChangeDispatcher: ILightChangeDispatcher, defaultState: DefaultLightState = DefaultLightState(), private val messageHandler: ILightMessageHandler = LightMessageHandler) : LifxEntity() {
    override val entityId: Int by lazy { id.hashCode() }

    override val lights: List<Light> = listOf(this)

    private val changeDispatcher = object : ILightChangeDispatcher {
        val dispatchers = mutableSetOf(sourceChangeDispatcher)

        override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
            dispatchers.forEach { it.onLightChange(light, property, oldValue, newValue) }
        }
    }

    fun addChangeDispatcher(dispatcher: ILightChangeDispatcher) {
        changeDispatcher.dispatchers.add(dispatcher)
    }

    fun removeChangeDispatcher(dispatcher: ILightChangeDispatcher) {
        changeDispatcher.dispatchers.remove(dispatcher)
    }

    internal val updatedAtByProperty = mutableMapOf<LightProperty, Long>()
    internal val zoneUpdatedAtByProperty = Array<Long>(80, { 0L })

    var address: InetAddress = defaultState.address
        internal set

    fun attach(messages: GroupedFlowable<Long, SourcedLifxMessage<LifxMessage<LifxMessagePayload>>>): Disposable {
        val disposables = CompositeDisposable()

        messages.subscribe { message ->
            address = message.source
            lastSeenAt = Date().time

            updateReachability()

            messageHandler.handleMessage(this, message.message.payload)
        }.capture(disposables)

        source.tick.subscribe {
            pollState()

            updateReachability()
        }.capture(disposables)

        pollProperties()
        pollState()

        return disposables
    }

    private fun pollState() {
        LightGetCommand.create(this).fireAndForget()
        if (productInfo.hasMultiZoneSupport) {
            MultiZoneGetColorZonesCommand.create(this).fireAndForget()
        }
        if (productInfo.hasInfraredSupport) {
            LightGetInfraredCommand.create(this).fireAndForget()
        }
    }

    private fun pollProperties() {
        DeviceGetHostFirmwareCommand.create(this).fireAndForget()
        DeviceGetWifiFirmwareCommand.create(this).fireAndForget()
        DeviceGetVersionCommand.create(this).fireAndForget()
        pollMutableProperties()
    }

    private fun pollMutableProperties() {
        DeviceGetGroupCommand.create(this).fireAndForget()
        DeviceGetLocationCommand.create(this).fireAndForget()
    }

    private fun updateReachability() {
        reachable = lastSeenAt > Date().time - REACHABILITY_TIMEOUT
    }

    var lastSeenAt: Long = defaultState.lastSeenAt
        private set
    var reachable: Boolean by LightChangeNotifier(LightProperty.Reachable, defaultState.reachable, changeDispatcher)
        private set

    var label: String by LightChangeNotifier(LightProperty.Label, defaultState.label, changeDispatcher)
        internal set
    var color: HSBK by LightChangeNotifier(LightProperty.Color, defaultState.color, changeDispatcher)
        internal set
    var power: PowerState by LightChangeNotifier(LightProperty.Power, defaultState.power, changeDispatcher)
        internal set

    var group: StateGroup by LightChangeNotifier(LightProperty.Group, defaultState.group, changeDispatcher)
        internal set
    var location: StateLocation by LightChangeNotifier(LightProperty.Location, defaultState.location, changeDispatcher)
        internal set

    var hostFirmware: FirmwareVersion by LightChangeNotifier(LightProperty.HostFirmware, defaultState.hostFirmware, changeDispatcher)
        internal set
    var wifiFirmware: FirmwareVersion by LightChangeNotifier(LightProperty.WifiFirmware, defaultState.wifiFirmware, changeDispatcher)
        internal set

    var productInfo: ProductInfo by LightChangeNotifier(LightProperty.ProductInfo, defaultState.productInfo, changeDispatcher)
        internal set

    var infraredBrightness: Short by LightChangeNotifier(LightProperty.InfraredBrightness, defaultState.infraredBrightness, changeDispatcher)
        internal set

    var zones: Zones by LightChangeNotifier(LightProperty.Zones, defaultState.zones, changeDispatcher)
        internal set

    private var sequence: Byte = 0
    fun getNextSequence(): Byte = sequence.inc()

    companion object {
        const val REACHABILITY_TIMEOUT = 11_000L
        const val CLIENT_CHANGE_TIMEOUT = 2_000L

        val defaultGroup = StateGroup(Array(8, { 48.toByte() }), byteArrayOf(), 0L)
        val defaultLocation = StateLocation(Array(8, { 48.toByte() }), byteArrayOf(), 0L)
    }
}

enum class LightChangeSource {
    Client,
    Device,
}

internal inline fun Light.update(source: LightChangeSource, property: LightProperty, apply: () -> Unit) {
    val now = Date().time
    if (source == LightChangeSource.Client || updatedAtByProperty.getOrDefault(property, 0) + CLIENT_CHANGE_TIMEOUT < now) {
        if (source == LightChangeSource.Client) {
            updatedAtByProperty[property] = now
        }
        apply()
    }
}

internal inline fun Light.updateZone(source: LightChangeSource, property: LightProperty, range: IntRange, apply: () -> Unit) {
    val now = Date().time
    if (source == LightChangeSource.Client || range.any { zoneUpdatedAtByProperty[it] + CLIENT_CHANGE_TIMEOUT < now }) {
        if (source == LightChangeSource.Client) {
            updatedAtByProperty[property] = now
        }
        apply()
    }
}

internal fun String.trimNullbytes(): String {
    val nullBytePosition = this.indexOf('\u0000')
    return if (nullBytePosition != -1) {
        substring(0, nullBytePosition)
    } else {
        this
    }
}

val StateGroup.name
    get() = String(label).trimNullbytes()

val StateLocation.name
    get() = String(label).trimNullbytes()

data class Zones(val count: Int, val colors: List<HSBK>)