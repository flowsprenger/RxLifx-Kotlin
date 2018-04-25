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
import wo.lf.lifx.domain.*
import wo.lf.lifx.domain.Lifx.defaultColor
import wo.lf.lifx.extensions.capture
import wo.lf.lifx.extensions.fireAndForget
import wo.lf.lifx.net.SourcedLifxMessage
import java.lang.Math.min
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
    InfraredBrightness
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

class Light(val id: Long, val source: ILightSource<LifxMessage<LifxMessagePayload>>, changeDispatcher: ILightChangeDispatcher) {

    lateinit var address: InetAddress

    fun attach(messages: GroupedFlowable<Long, SourcedLifxMessage<LifxMessage<LifxMessagePayload>>>): Disposable {
        val disposables = CompositeDisposable()

        messages.subscribe { message ->
            address = message.source
            lastSeenAt = Date().time

            updateReachability()

            val payload = message.message.payload
            when (payload) {
                is LightState -> {
                    label = String(payload.label).trimNullbytes()
                    color = payload.color
                    power = PowerState.fromValue(payload.power)
                }
                is StateGroup -> {
                    group = payload
                }
                is StateLocation -> {
                    location = payload
                }
                is StateHostFirmware -> {
                    hostFirmware = FirmwareVersion.fromHostFirmwareVersion(payload)
                }
                is StateWifiFirmware -> {
                    wifiFirmware = FirmwareVersion.fromWifiFirmwareVersion(payload)
                }
                is StateVersion -> {
                    productInfo = ProductInfo.fromStateVersion(payload)
                }
                is StateInfrared -> {
                    infraredBrightness = payload.brightness
                }
                is StateMultiZone -> {
                    val count = payload.count.toInt()
                    val index = payload.index.toInt()
                    val firstAfter = min(index + 8, count)
                    if(zones.count != count || zones.colors.subList(index, firstAfter) != payload.color.toList().subList(0, min(8, count - index))){
                        zones = Zones(count = count, colors = List(index,
                                { zones.colors.getOrNull(it) ?: defaultColor })
                                .plus(payload.color.map { it }.subList(0, Math.min(8, count - index))
                                .plus(List(count - firstAfter, { zones.colors.getOrNull(firstAfter + it) ?: defaultColor })))
                        )
                    }
                }
                is StateService -> {
                    // NOOP
                }
                else -> println("${message.message.payload}")
            }
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
        if (productInfo.hasInfraredSupport) {
            MultiZoneGetColorZonesCommand.create(this).fireAndForget()
        }
        if (productInfo.hasMultiZoneSupport) {
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

    var lastSeenAt: Long = 0L
        private set
    var reachable: Boolean by LightChangeNotifier(LightProperty.Reachable, false, changeDispatcher)
        private set

    var label: String by LightChangeNotifier(LightProperty.Label, "", changeDispatcher)
        private set
    var color: HSBK by LightChangeNotifier(LightProperty.Color, Lifx.defaultColor, changeDispatcher)
        private set
    var power: PowerState by LightChangeNotifier(LightProperty.Power, PowerState.OFF, changeDispatcher)
        private set

    var group: StateGroup by LightChangeNotifier(LightProperty.Group, defaultGroup, changeDispatcher)
        private set
    var location: StateLocation by LightChangeNotifier(LightProperty.Location, defaultLocation, changeDispatcher)
        private set

    var hostFirmware: FirmwareVersion by LightChangeNotifier(LightProperty.HostFirmware, FirmwareVersion.default, changeDispatcher)
        private set
    var wifiFirmware: FirmwareVersion by LightChangeNotifier(LightProperty.WifiFirmware, FirmwareVersion.default, changeDispatcher)
        private set

    var productInfo: ProductInfo by LightChangeNotifier(LightProperty.ProductInfo, ProductInfo.default, changeDispatcher)
        private set

    var infraredBrightness: Short by LightChangeNotifier(LightProperty.InfraredBrightness, 0, changeDispatcher)
        private set

    var zones: Zones by LightChangeNotifier(LightProperty.InfraredBrightness, Zones(count = 0, colors = listOf()), changeDispatcher)
        private set

    private var sequence: Byte = 0
    fun getNextSequence(): Byte = sequence.inc()

    companion object {
        const val REACHABILITY_TIMEOUT = 11_000L

        val defaultGroup = StateGroup(Array(8, { 48.toByte() }), byteArrayOf(), 0L)
        val defaultLocation = StateLocation(Array(8, { 48.toByte() }), byteArrayOf(), 0L)
    }
}

private fun String.trimNullbytes(): String {
    return substring(0, this.indexOf('\u0000'))
}

val StateGroup.name
    get() = String(label).trimNullbytes()

val StateLocation.name
    get() = String(label).trimNullbytes()

data class Zones(val count: Int, val colors: List<HSBK>)