package wo.lf.lifx.api

import wo.lf.lifx.domain.*

interface ILightMessageHandler {
    fun handleMessage(light: Light, payload: LifxMessagePayload)
}

object LightMessageHandler: ILightMessageHandler {

    override fun handleMessage(light: Light, payload: LifxMessagePayload) {
        light.apply {
            when (payload) {
                is LightState -> {
                    update(LightChangeSource.Device, LightProperty.Label) {
                        label = String(payload.label).trimNullbytes()
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
                is StateMultiZone -> {
                    update(LightChangeSource.Device, LightProperty.Zones) {
                        val count = payload.count.toInt()
                        val index = payload.index.toInt()
                        val firstAfter = Math.min(index + 8, count)
                        if (zones.count != count || zones.colors.subList(index, firstAfter) != payload.color.toList().subList(0, Math.min(8, count - index))) {
                            zones = Zones(count = count, colors = List(index,
                                    { zones.colors.getOrNull(it) ?: Lifx.defaultColor })
                                    .plus(payload.color.map { it }.subList(0, Math.min(8, count - index))
                                            .plus(List(count - firstAfter, {
                                                zones.colors.getOrNull(firstAfter + it) ?: Lifx.defaultColor
                                            })))
                            )
                        }
                    }
                }
                is StateService -> {
                    // NOOP
                }
                else -> println("${payload}")
            }
        }
    }
}