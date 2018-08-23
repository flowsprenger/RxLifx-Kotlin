package wo.lf.lifx.api

import wo.lf.lifx.domain.*
import java.lang.Integer.min

interface ILightMessageHandler {
    fun handleMessage(light: Light, payload: LifxMessagePayload)
}

object LightMessageHandler : ILightMessageHandler {

    override fun handleMessage(light: Light, payload: LifxMessagePayload) {
        light.apply {
            when (payload) {
                is LightState -> {
                    update(LightChangeSource.Device, LightProperty.Label) {
                        label = String(payload.label).trimNullBytes()
                    }
                    update(LightChangeSource.Device, LightProperty.Color) {
                        color = payload.color
                    }
                    update(LightChangeSource.Device, LightProperty.Power) {
                        power = PowerState.fromValue(payload.power)
                    }
                }
                is StateGroup -> {
                    update(LightChangeSource.Device, LightProperty.Group) {
                        group = payload
                    }
                }
                is StateLocation -> {
                    update(LightChangeSource.Device, LightProperty.Location) {
                        location = payload
                    }
                }
                is StateHostFirmware -> {
                    update(LightChangeSource.Device, LightProperty.HostFirmware) {
                        hostFirmware = FirmwareVersion.fromHostFirmwareVersion(payload)
                    }
                }
                is StateWifiFirmware -> {
                    update(LightChangeSource.Device, LightProperty.WifiFirmware) {
                        wifiFirmware = FirmwareVersion.fromWifiFirmwareVersion(payload)
                    }
                }
                is StateVersion -> {
                    update(LightChangeSource.Device, LightProperty.ProductInfo) {
                        productInfo = ProductInfo.fromStateVersion(payload)
                    }
                }
                is StateInfrared -> {
                    update(LightChangeSource.Device, LightProperty.InfraredBrightness) {
                        infraredBrightness = payload.brightness
                    }
                }
                is StateZone -> {
                    val index = payload.index.toInt()
                    updateZone(LightChangeSource.Device, LightProperty.Zones, index .. index + 1) {
                        val count = payload.count.toInt()
                        val firstAfter = index + 1
                        if (zones.count != count || zones.count < index || zones.colors[index] != payload.color) {
                            zones = Zones(count = count, colors = List(index) {
                                zones.colors.getOrNull(it) ?: Lifx.defaultColor
                            }
                                    .plus(listOf(payload.color)
                                            .plus(List(count - firstAfter) {
                                                zones.colors.getOrNull(firstAfter + it) ?: Lifx.defaultColor
                                            }))
                            )
                        }
                    }
                }
                is StateMultiZone -> {
                    val index = payload.index.toInt()
                    val count = payload.count.toInt()
                    updateZone(LightChangeSource.Device, LightProperty.Zones, index .. min(count, payload.index + 8)) {
                        val firstAfter = Math.min(index + 8, count)
                        if (zones.count != count || zones.count < firstAfter || zones.colors.subList(index, firstAfter) != payload.color.toList().subList(0, Math.min(8, count - index))) {
                            zones = Zones(count = count, colors = List(index) {
                                zones.colors.getOrNull(it) ?: Lifx.defaultColor
                            }
                                    .plus(payload.color.map { it }.subList(0, Math.min(8, count - index))
                                            .plus(List(count - firstAfter) {
                                                zones.colors.getOrNull(firstAfter + it) ?: Lifx.defaultColor
                                            }))
                            )
                        }
                    }
                }
                is LightStatePower -> {
                    update(LightChangeSource.Device, LightProperty.Power) {
                        power = PowerState.fromValue(payload.level)
                    }
                }
                is StatePower -> {
                    update(LightChangeSource.Device, LightProperty.Power) {
                        power = PowerState.fromValue(payload.level)
                    }
                }
                is StateLabel -> {
                    update(LightChangeSource.Device, LightProperty.Label) {
                        label = String(payload.label).trimNullBytes()
                    }
                }
                is StateService, is StateWifiInfo, is StateHostInfo, is EchoResponse -> {
                    // NOOP
                }
                is SetPower, is LightSetWaveformOptional, is LightSetWaveform, is LightSetPower, is LightSetColor, is SetGroup, is SetLocation, is SetLabel, is SetInfrared, is SetColorZones -> {

                }
                else -> println("cannot recognize payload: $payload")
            }
        }
    }
}