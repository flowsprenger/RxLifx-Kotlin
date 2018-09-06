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

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import wo.lf.lifx.domain.*
import wo.lf.lifx.extensions.broadcastTo
import wo.lf.lifx.extensions.copy
import wo.lf.lifx.extensions.retryTimes
import wo.lf.lifx.extensions.targetTo
import java.util.concurrent.TimeUnit

object BroadcastGetServiceCommand {
    fun create(lightSource: LightService): Completable {
        return lightSource.broadcast(GetService())
    }
}

object DeviceGetHostInfoCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateHostInfo> {
        return light.send(GetHostInfo(), ackRequired, responseRequired)
    }
}

object DeviceGetHostFirmwareCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateHostFirmware> {
        return light.send(GetHostFirmware(), ackRequired, responseRequired)
    }
}

object DeviceGetWifiInfoCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateWifiInfo> {
        return light.send(GetWifiInfo(), ackRequired, responseRequired)
    }
}

object DeviceGetWifiFirmwareCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateWifiFirmware> {
        return light.send(GetWifiFirmware(), ackRequired, responseRequired)
    }
}

object DeviceGetPowerCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StatePower> {
        return light.send(GetPower(), ackRequired, responseRequired)
    }
}

object DeviceSetPowerCommand {
    fun create(light: Light, status: Boolean, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StatePower> {
        val power: Short = if (status) 0xffff.toShort() else 0
        return light.send(SetPower(level = power), ackRequired, responseRequired) {
            light.update(LightChangeSource.Client, LightProperty.Power) {
                light.power = PowerState.fromValue(power)
            }
        }
    }
}

object DeviceGetLabelCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLabel> {
        return light.send(GetLabel(), ackRequired, responseRequired)
    }
}

object DeviceSetLabelCommand {
    fun create(light: Light, label: String, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup> {
        val sanitizedLabel = label.maxLengthPadNull(32)
        return light.send(SetLabel(sanitizedLabel.toByteArray()), ackRequired, responseRequired) {
            light.update(LightChangeSource.Client, LightProperty.Label) {
                light.label = sanitizedLabel.trimNullBytes()
            }
        }
    }
}

object DeviceGetVersionCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateVersion> {
        return light.send(GetVersion(), ackRequired, responseRequired)
    }
}

object DeviceGetInfoCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateInfo> {
        return light.send(GetInfo(), ackRequired, responseRequired)
    }
}

object DeviceGetLocationCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLocation> {
        return light.send(GetLocation(), ackRequired, responseRequired)
    }
}

object DeviceSetLocationCommand {
    fun create(light: Light, location: Array<Byte>, label: String, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLocation> {
        return create(light, location, label.maxLengthPadNull(32).toByteArray(), updatedAt, ackRequired, responseRequired)
    }

    fun create(light: Light, location: Array<Byte>, label: ByteArray, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLocation> {
        return light.send(SetLocation(location, label, updatedAt), ackRequired, responseRequired) {
            light.update(LightChangeSource.Client, LightProperty.Location) {
                light.location = StateLocation(location, label, updatedAt)
            }
        }
    }
}

object DeviceGetGroupCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup> {
        return light.send(GetGroup(), ackRequired, responseRequired)
    }
}

object DeviceSetGroupCommand {
    fun create(light: Light, group: Array<Byte>, label: String, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup> {
        return create(light, group, label.maxLengthPadNull(32).toByteArray(), updatedAt, ackRequired, responseRequired)
    }

    fun create(light: Light, group: Array<Byte>, label: ByteArray, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup> {
        return light.send(SetGroup(group, label, updatedAt), ackRequired, responseRequired) {
            light.update(LightChangeSource.Client, LightProperty.Group) {
                light.group = StateGroup(group, label, updatedAt)
            }
        }
    }
}

object DeviceEchoRequestCommand {
    fun create(light: Light, echo: String, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<EchoResponse> {
        return light.send(EchoRequest(echo.maxLengthPadNull(64).toByteArray().toTypedArray()), ackRequired, responseRequired)
    }
}

object LightGetCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState> {
        return light.send(LightGet(), ackRequired, responseRequired)
    }
}

object LightSetColorCommand {
    fun create(light: Light, color: HSBK, duration: Int, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState> {
        return light.send(LightSetColor(0.toByte(), color, duration), ackRequired, responseRequired) {
            light.update(LightChangeSource.Client, LightProperty.Color) {
                light.color = color
            }
        }
    }
}

object LightSetWaveformCommand {
    fun create(light: Light, transient: Boolean, color: HSBK, period: Int, cycles: Float, skewRatio: Short, waveform: WaveformType, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState> {
        return light.send(LightSetWaveform(0, transient.toByte(), color, period, cycles, skewRatio, waveform), ackRequired, responseRequired)
    }
}

object LightSetWaveformOptionalCommand {
    fun create(light: Light, transient: Boolean, color: HSBK, period: Int, cycles: Float, skewRatio: Short, waveform: WaveformType, setHue: Boolean, setSaturation: Boolean, setBrightness: Boolean, setKelvin: Boolean, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState> {
        return light.send(LightSetWaveformOptional(0, transient.toByte(), color, period, cycles, skewRatio, waveform, setHue.toByte(), setSaturation.toByte(), setBrightness.toByte(), setKelvin.toByte()), ackRequired, responseRequired)
    }
}

object LightGetPowerCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightStatePower> {
        return light.send(LightGetPower(), ackRequired, responseRequired)
    }
}

object LightSetPowerCommand {
    fun create(light: Light, status: Boolean, duration: Int, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightStatePower> {
        val power: Short = if (status) 0xffff.toShort() else 0
        return light.send(LightSetPower(level = power, duration = duration), ackRequired, responseRequired) {
            light.update(LightChangeSource.Client, LightProperty.Power) {
                light.power = PowerState.fromValue(power)
            }
        }
    }
}

object LightGetInfraredCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = true): Maybe<StateInfrared> {
        return light.send(GetInfrared(), ackRequired, responseRequired)
    }
}

object LightSetInfraredCommand {
    fun create(light: Light, brightness: Short, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateInfrared> {
        return light.send(SetInfrared(brightness), ackRequired, responseRequired) {
            light.update(LightChangeSource.Client, LightProperty.InfraredBrightness) {
                light.infraredBrightness = brightness
            }
        }
    }
}

object MultiZoneSetColorZonesCommand {
    fun create(light: Light, color: HSBK, startIndex: Int, endIndex: Int, duration: Int, apply: ApplicationRequest = ApplicationRequest.APPLY, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState> {
        return light.send(SetColorZones(startIndex.toByte(), endIndex.toByte(), color, duration, apply), ackRequired, responseRequired) {
            light.update(LightChangeSource.Client, LightProperty.Color) {
                val start = Math.max(0, startIndex)
                val after = Math.min(light.zones.count, endIndex)
                light.zones = Zones(count = light.zones.count, colors = light.zones.colors.subList(0, start).plus(List(after - start, { color })).plus(light.zones.colors.subList(after, light.zones.colors.size)))
                if (startIndex == 0) {
                    light.color = color
                }
            }
        }
    }
}

object MultiZoneGetColorZonesCommand {
    fun create(light: Light, startIndex: Int = 0, endIndex: Int = 255, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateMultiZone> {
        return light.send(GetColorZones(startIndex.toByte(), endIndex.toByte()), ackRequired, responseRequired)
    }
}

object TileGetDeviceChainCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateDeviceChain> {
        return light.send(GetDeviceChain(), ackRequired, responseRequired)
    }
}

object TileGetTileState64Command {
    fun create(light: Light, startIndex: Int = 0, length: Int = 255, x: Int = 0, y: Int = 0, width: Int = 8, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateTileState64> {
        return light.send(GetTileState64(startIndex.toByte(), length.toByte(), 0, x.toByte(), y.toByte(), width.toByte()), ackRequired, responseRequired)
    }
}

object TileSetTileState64Command {
    fun create(tileService: TileService, light: Light, tileIndex: Int = 0, length: Int = tileIndex + 1, x: Int = 0, y: Int = 0, width: Int = 8, duration: Int = 1000, colors: List<HSBK>, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateTileState64> {
        return light.send(SetTileState64(tileIndex.toByte(), length.toByte(), 0, x.toByte(), y.toByte(), width.toByte(), duration, colors.toTypedArray()), ackRequired, responseRequired) {
            tileService.tiles.firstOrNull { it.light === light }?.let { tile ->
                val device = tile.chain[tileIndex]
                tileService.updateTile(tile, device, x, y, width, colors.toTypedArray())
            }
        }
    }

    fun create(light: Light, tileIndex: Int = 0, endIndex: Int = tileIndex + 1, x: Int = 0, y: Int = 0, width: Int = 8, duration: Int = 1000, colors: List<HSBK>, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateTileState64> {
        return light.send(SetTileState64(tileIndex.toByte(), endIndex.toByte(), 0, x.toByte(), y.toByte(), width.toByte(), duration, colors.toTypedArray()), ackRequired, responseRequired)
    }
}

object LightSetBrightness {
    fun create(light: Light, brightness: Short, duration: Int, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState> {
        return if (light.productInfo.hasMultiZoneSupport) {
            LightSetWaveformOptionalCommand.create(
                    light = light,
                    transient = false,
                    color = light.color.copy(brightness = brightness),
                    period = duration,
                    cycles = 1f,
                    skewRatio = 0,
                    waveform = WaveformType.SAW,
                    setHue = false,
                    setSaturation = false,
                    setBrightness = true,
                    setKelvin = false
            )
        } else {
            LightSetColorCommand.create(
                    light = light,
                    color = light.color.copy(brightness = brightness),
                    duration = duration,
                    ackRequired = ackRequired,
                    responseRequired = responseRequired
            )
        }
    }
}

fun Boolean.toByte(): Byte {
    if (this) {
        return 1
    }
    return 0
}

fun String.maxLengthPadNull(length: Int): String {
    return substring(0, Math.min(length, this.length)).padEnd(length, '\u0000')
}

fun LightService.broadcast(payload: LifxMessagePayload): Completable {
    return Completable.create {
        if (send(payload.broadcastTo(sourceId))) {
            it.onComplete()
        } else {
            it.onError(TransportNotConnectedException())
        }
    }
}

inline fun <reified R> Light.send(payload: LifxMessagePayload, ackRequired: Boolean = false, responseRequired: Boolean = false, noinline sideEffect: (() -> Unit)? = null): Maybe<R> {


    return if (ackRequired || responseRequired) {
        var awaitingAck = ackRequired
        var awaitingResponse = responseRequired
        var response: R? = null

        val sender = Single.create<Byte> {
            val sequence = getNextSequence()
            if (source.send(payload.targetTo(source.sourceId, id, address, sequence))) {
                sideEffect?.invoke()
                it.onSuccess(sequence)
            } else {
                it.onError(TransportNotConnectedException())
            }
        }

        sender
                .flatMapPublisher { sequence ->
                    source.messages.filter { it.message.header.target == id && it.message.header.source == source.sourceId && it.message.header.sequence == sequence }
                }
                .skipWhile {
                    if (awaitingAck && it.message.header.type == MessageType.Acknowledgement.value) {
                        awaitingAck = false
                    }

                    if (awaitingResponse && it.message.payload is R) {
                        response = it.message.payload
                        awaitingResponse = false
                    }

                    !(!awaitingResponse && !awaitingAck)
                }
                .singleOrError()
                .flatMapMaybe {
                    if (awaitingResponse) {
                        Maybe.just(response!!)
                    } else {
                        Maybe.empty()
                    }
                }
                .timeout(100, TimeUnit.MILLISECONDS, source.ioScheduler)
                .retryTimes(3)
    } else {
        val sender = Single.create<Boolean> {
            if (source.send(payload.targetTo(source.sourceId, id, address))) {
                sideEffect?.invoke()
                it.onSuccess(true)
            } else {
                it.onError(TransportNotConnectedException())
            }
        }

        sender.flatMapMaybe { Maybe.empty<R>() }
    }
}

class TransportNotConnectedException : Throwable()