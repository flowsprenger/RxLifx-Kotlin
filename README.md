# RxLifx-Kotlin
[![Build Status](https://travis-ci.org/flowsprenger/RxLifx-Kotlin.svg?branch=master)](https://travis-ci.org/flowsprenger/RxLifx-Kotlin)

A library to communicate with local LIFX lights using reactive programming and RxJava 2.

Currently non-complete and work in progress, mostly untested and undocumented.
Use at your own risk.

For a more complete swift implementation see: https://github.com/flowsprenger/RxLifx-Swift
For generating (de)serializers see: https://github.com/flowsprenger/Lifx-Protocol-Generator

Implements protocol & messages described at https://lan.developer.lifx.com/

# Install

Download the latest JAR or grab via Maven:

```
<dependency>
  <groupId>wo.lf</groupId>
  <artifactId>rx-lifx</artifactId>
  <version>0.0.5</version>
</dependency>
```

or Gradle:

```
implementation 'wo.lf:rx-lifx:0.0.5'
```

using jCenter()

# Usage

Implement ILightChangeDispatcher to track lights being detected and properties of the lights changing
```kotlin
val changeListener = object : ILightChangeDispatcher {
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