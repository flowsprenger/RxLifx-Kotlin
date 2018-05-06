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
import wo.lf.lifx.domain.*
import wo.lf.lifx.extensions.broadcastTo
import wo.lf.lifx.extensions.targetTo
import java.util.concurrent.TimeUnit

object BroadcastGetServiceCommand {
    fun create(lightSource: LightService): Completable {
        return lightSource.broadcast(GetService())
    }
}

object LightGetCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState> {
        return light.send(LightGet(), ackRequired, responseRequired)
    }
}

object DeviceGetGroupCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup> {
        return light.send(GetGroup(), ackRequired, responseRequired)
    }
}

object DeviceGetLocationCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLocation> {
        return light.send(GetLocation(), ackRequired, responseRequired)
    }
}

object DeviceGetHostFirmwareCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateHostFirmware> {
        return light.send(GetHostFirmware(), ackRequired, responseRequired)
    }
}

object DeviceGetWifiFirmwareCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateWifiFirmware> {
        return light.send(GetWifiFirmware(), ackRequired, responseRequired)
    }
}

object DeviceGetVersionCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateVersion> {
        return light.send(GetVersion(), ackRequired, responseRequired)
    }
}

object LightGetInfraredCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = true): Maybe<StateInfrared> {
        return light.send(GetInfrared(), ackRequired, responseRequired)
    }
}

object MultiZoneGetColorZonesCommand {
    fun create(light: Light, startIndex: Int = 0, endIndex: Int = 255, ackRequired: Boolean = false, responseRequired: Boolean = true): Maybe<StateMultiZone> {
        return light.send(GetColorZones(startIndex.toByte(), endIndex.toByte()), ackRequired, responseRequired)
    }
}

object LightSetColorCommand {
   fun create(light: Light, color: HSBK, duration: Int, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState> {
       return light.send(LightSetColor(0.toByte(), color, duration), ackRequired, responseRequired){
           light.update(LightChangeSource.Client, LightProperty.Color) {
               light.color = color
           }
       }
    }
}

object LightSetInfraredCommand {
    fun create(light: Light, brightness: Short, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateInfrared> {
        return light.send(SetInfrared(brightness), ackRequired, responseRequired){
            light.update(LightChangeSource.Client, LightProperty.InfraredBrightness) {
                light.infraredBrightness = brightness
            }
        }
    }
}

object LightSetPowerCommand {
    fun create(light: Light, status: Boolean, duration: Int, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightStatePower> {
        val power: Short = if (status) 0xffff.toShort() else 0
        return light.send(LightSetPower(level = power, duration = duration), ackRequired, responseRequired){
            light.update(LightChangeSource.Client, LightProperty.Power) {
                light.power = PowerState.fromValue(power)
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



object DeviceSetGroupCommand {
    fun create(light: Light, group: Array<Byte>, label: String, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup> {
        return create(light, group, label.maxLengthPadNull(32).toByteArray(), updatedAt, ackRequired, responseRequired)
    }

    fun create(light: Light, group: Array<Byte>, label: ByteArray, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup> {
        return light.send(SetGroup(group, label, updatedAt), ackRequired, responseRequired){
            light.update(LightChangeSource.Client, LightProperty.Group) {
                light.group = StateGroup(group, label, updatedAt)
            }
        }
    }
}

object DeviceSetLocationCommand {
    fun create(light: Light, location: Array<Byte>, label: String, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLocation> {
        return create(light, location, label.maxLengthPadNull(32).toByteArray(), updatedAt, ackRequired, responseRequired)
    }

    fun create(light: Light, location: Array<Byte>, label: ByteArray, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLocation> {
        return light.send(SetLocation(location, label, updatedAt), ackRequired, responseRequired){
            light.update(LightChangeSource.Client, LightProperty.Location) {
                light.location = StateLocation(location, label, updatedAt)
            }
        }
    }
}

object DeviceSetLabelCommand {
    fun create(light: Light, label: String, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup> {
        val sanitizedLabel = label.maxLengthPadNull(32)
        return light.send(SetLabel(sanitizedLabel.toByteArray()), ackRequired, responseRequired){
            light.update(LightChangeSource.Client, LightProperty.Label) {
                light.label = sanitizedLabel.trimNullbytes()
            }
        }
    }
}

fun Boolean.toByte(): Byte {
    if(this){
        return 1
    }
    return 0
}

fun String.maxLengthPadNull(length: Int): String {
    return substring(0, length).padEnd(length, '\u0000')
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
    return Maybe.create<R> { emitter ->
        if (ackRequired || responseRequired) {
            var awaitingAck = ackRequired
            var awaitingResponse = responseRequired
            var response: R? = null
            val sequence = getNextSequence()
            if(source.send(payload.targetTo(source.sourceId, id, address, sequence))) {
                sideEffect?.invoke()
                source.messages.filter { it.message.header.target == id && it.message.header.source == source.sourceId && it.message.header.sequence == sequence }.timeout(2L, TimeUnit.SECONDS).subscribe({
                    if (awaitingAck && it.message.header.type == MessageType.Acknowledgement.value) {
                        awaitingAck = false
                    }

                    if (awaitingResponse && it.message.payload is R) {
                        response = it.message.payload
                        awaitingResponse = false
                    }

                    if (!awaitingResponse && !awaitingAck) {
                        if (awaitingResponse) {
                            emitter.onSuccess(response!!)
                        } else {
                            emitter.onComplete()
                        }
                    }
                }, { emitter.onError(it) })
            }else{
                emitter.onError(TransportNotConnectedException())
            }
        } else {
            source.send(payload.targetTo(source.sourceId, id, address))
            emitter.onComplete()
        }
    }
}

class TransportNotConnectedException : Throwable()