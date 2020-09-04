# RxLifx-Kotlin
[![Build Status](https://travis-ci.org/flowsprenger/RxLifx-Kotlin.svg?branch=master)](https://travis-ci.org/flowsprenger/RxLifx-Kotlin)

A library to communicate with local LIFX lights using reactive programming and RxJava 2.

Use at your own risk. Feel free to modify, licensed under MIT.
Pull requests welcome.

A sample project on how to use this to build an Android App see my project [Enlight](https://github.com/flowsprenger/Enlight)

For a swift implementation see: https://github.com/flowsprenger/RxLifx-Swift

For generating (de)serializers see: https://github.com/flowsprenger/Lifx-Protocol-Generator

Implements protocol & messages described at [Lifx's Developer Portal](https://lan.developer.lifx.com/)

# Install

Download the latest JAR or grab via Maven:

```
<dependency>
  <groupId>wo.lf</groupId>
  <artifactId>rx-lifx</artifactId>
  <version>0.1.1-alpha</version>
</dependency>
```

or Gradle:

```
implementation 'wo.lf:rx-lifx:0.1.1-alpha'
```

using jCenter()

# Usage

Implement ILightChangeDispatcher to track lights being detected and properties of the lights changing
```kotlin
val changeListener = object : ILightsChangeDispatcher {
        override fun onLightAdded(light: Light) {
            println("light added : ${light.id}")
        }

        override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
            println("light ${light.id} changed $property from $oldValue to $newValue")
        }
    }
```

Create an instance of LightService referencing the change listener
```kotlin
val lightSource = LightService(UdpTransport, changeListener)
```

start (and stop) service:
```kotlin
lightSource.start()
lightSource.stop()
```

use commands in wo.lf.lifx.api to change and query lights
```kotlin
LightSetPowerCommand.create(light, true, 1000).fireAndForget()
```

Device messages as described [here](https://lan.developer.lifx.com/docs/device-messages)

```kotlin

BroadcastGetServiceCommand.create(lightSource: LightService): Completable
DeviceGetHostInfoCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateHostInfo>
DeviceGetHostFirmwareCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateHostFirmware>
DeviceGetWifiInfoCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateWifiInfo>
DeviceGetWifiFirmwareCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateWifiFirmware>
DeviceGetPowerCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StatePower>
DeviceSetPowerCommand.create(light: Light, status: Boolean, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StatePower>
DeviceGetLabelCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLabel>
DeviceSetLabelCommand.create(light: Light, label: String, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup>
DeviceGetVersionCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateVersion>
DeviceGetInfoCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateInfo>
DeviceGetLocationCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLocation>
DeviceSetLocationCommand.create(light: Light, location: Array<Byte>, label: String, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLocation>
DeviceSetLocationCommand.create(light: Light, location: Array<Byte>, label: ByteArray, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateLocation>
DeviceGetGroupCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup>
DeviceSetGroupCommand.create(light: Light, group: Array<Byte>, label: String, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup>
DeviceSetGroupCommand.create(light: Light, group: Array<Byte>, label: ByteArray, updatedAt: Long, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateGroup>
DeviceEchoRequestCommand.create(light: Light, echo: String, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<EchoResponse>
```

Light messages as described [here](https://lan.developer.lifx.com/docs/light-messages)

```kotlin

LightGetCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState>
LightSetColorCommand.create(light: Light, color: HSBK, duration: Int, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState>
LightSetWaveformCommand.create(light: Light, transient: Boolean, color: HSBK, period: Int, cycles: Float, skewRatio: Short, waveform: WaveformType, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState>
LightSetWaveformOptionalCommand.create(light: Light, transient: Boolean, color: HSBK, period: Int, cycles: Float, skewRatio: Short, waveform: WaveformType, setHue: Boolean, setSaturation: Boolean, setBrightness: Boolean, setKelvin: Boolean, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState>
LightGetPowerCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightStatePower>
LightSetPowerCommand.create(light: Light, status: Boolean, duration: Int, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightStatePower>
LightGetInfraredCommand.create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = true): Maybe<StateInfrared>
LightSetInfraredCommand.create(light: Light, brightness: Short, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateInfrared>

```

Light messages as described [here](https://lan.developer.lifx.com/docs/multizone-messages)

```kotlin
MultiZoneSetColorZonesCommand.create(light: Light, color: HSBK, startIndex: Int, endIndex: Int, duration: Int, apply: ApplicationRequest = ApplicationRequest.APPLY, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState>
MultiZoneGetColorZonesCommand.create(light: Light, startIndex: Int = 0, endIndex: Int = 255, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<StateMultiZone>
```

Convenience method to set brightness (will use LightSetWaveformOptionalCommand internally to set brightness on multizone enabled devices) 

```kotlin
LightSetBrightness.create(light: Light, brightness: Short, duration: Int, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState>
```

Tile messages as described [here](https://lan.developer.lifx.com/docs/tile-messages)

are currently unsupported, as I do not have a tile device to verify implementation and develop something useful

# Tracking Location / Group ownership

If you don't want to track location / group data yourself, wrap the change listener in an instance of LocationGroupManager
to intercept all updates

```kotlin
val groupLocationChangeListener = object : IGroupLocationChangeListener {
   override fun locationAdded(newLocation: Location) {}
   override fun groupAdded(location: Location, group: Group) {}
   override fun locationGroupChanged(location: Location, group: Group, light: Light) {}
   override fun groupRemoved(location: Location, group: Group) {}
   override fun locationRemoved(location: Location) {}
}

val locationGroupManager = LocationGroupManager(changeListener, groupLocationChangeListener)
val lightSource = LightService(UdpTransport, locationGroupManager)
```

Afterwards you can either received events through the implementation of IGroupLocationChangeListener, query locations through:

```kotlin
locationGroupManager.locations // list of all known Location instances

val location = locationGroupManager.locations.first()
location.name // name of the location with the newest timestamp
location.lights // all lights in the location
location.groups // list of all Group instances in a location

val group = location.groups.first()
val group.name // name of the group
val group.lights // all lights in this group

```

or query Location and Group for a Light

```kotlin
val (location, group) = locationGroupManager.getLocationGroup(light)
```

to apply commands a location or group:

```kotlin
location.lights.foreach { light -> LightSetPowerCommand.create(light = light, status = true, duration = 1000) }
```
