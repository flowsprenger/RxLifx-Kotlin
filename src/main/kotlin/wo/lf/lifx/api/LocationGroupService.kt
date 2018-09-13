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

import wo.lf.lifx.domain.LifxMessagePayload
import wo.lf.lifx.domain.StateGroup
import wo.lf.lifx.domain.StateLocation

interface IGroupLocationChangeListener {
    fun locationAdded(newLocation: Location)
    fun groupAdded(location: Location, group: Group)
    fun locationGroupChanged(location: Location, group: Group, light: Light)
    fun groupRemoved(location: Location, group: Group)
    fun locationRemoved(location: Location)

}

class LocationGroupService(
        private val wrappedChangeDispatcher: ILightsChangeDispatcher
) : ILightServiceExtension<LifxMessage<LifxMessagePayload>>, ILightsChangeDispatcher {

    private var listeners: Set<IGroupLocationChangeListener> = setOf()

    private val locationsById: MutableMap<String, Location> = mutableMapOf()

    val locations: List<Location>
        get() = locationsById.values.toList()

    override fun start(source: ILightSource<LifxMessage<LifxMessagePayload>>) {
    }

    override fun stop() {
        locationsById.clear()
    }

    override fun onLightAdded(light: Light) {
        if (locationsById[light.location.id]?.groupsById?.get(light.group.id)?.lights?.contains(light) == true) {
            return
        }
        addToLocationAndGroup(light)
        wrappedChangeDispatcher.onLightAdded(light)
    }

    private fun addToLocationAndGroup(light: Light) {
        synchronized(this) {
            var newLocation = false
            var newGroup = false
            val location = locationsById.getOrPut(light.location.id) {
                newLocation = true
                Location(light.location.id)
            }
            val group = location.groupsById.getOrPut(light.group.id) {
                newGroup = true
                Group(light.group.id)
            }
            group.lights = group.lights.plus(light)
            if (newLocation) {
                listeners.forEach { it.locationAdded(location) }
            }
            if (newGroup) {
                listeners.forEach { it.groupAdded(location, group) }
            }
            listeners.forEach { it.locationGroupChanged(location, group, light) }
        }
    }

    override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {
        when (property) {
            LightProperty.Group -> changeGroup(light, oldValue as StateGroup, newValue as StateGroup)
            LightProperty.Location -> changeLocation(light, oldValue as StateLocation, newValue as StateLocation)
        }
        wrappedChangeDispatcher.onLightChange(light, property, oldValue, newValue)
    }

    private fun changeLocation(light: Light, oldLocation: StateLocation, newLocation: StateLocation) {
        synchronized(this) {
            if (oldLocation.location.contentEquals(newLocation.location)) {
                val (location, group) = getLocationGroup(newLocation.id, light.group.id)
                listeners.forEach { it.locationGroupChanged(location, group, light) }
            } else {
                val (location, group) = getLocationGroup(oldLocation.id, light.group.id)
                group.lights -= light
                if (group.lights.isEmpty()) {
                    location.groupsById.remove(group.id)
                    listeners.forEach { it.groupRemoved(location, group) }
                }
                if (location.groups.isEmpty()) {
                    locationsById.remove(location.id)
                    listeners.forEach { it.locationRemoved(location) }
                }
                addToLocationAndGroup(light)
            }
        }
    }

    private fun changeGroup(light: Light, oldGroup: StateGroup, newGroup: StateGroup) {
        synchronized(this) {
            if (oldGroup.group.contentEquals(newGroup.group)) {
                val (location, group) = getLocationGroup(light.location.id, newGroup.id)
                listeners.forEach { it.locationGroupChanged(location, group, light) }
            } else {
                val (location, group) = getLocationGroup(light.location.id, oldGroup.id)
                group.lights -= light
                if (group.lights.isEmpty()) {
                    location.groupsById.remove(group.id)
                    listeners.forEach { it.groupRemoved(location, group) }
                }
                addToLocationAndGroup(light)
            }
        }
    }

    private fun getLocationGroup(location: String, group: String): Pair<Location, Group> {
        locationsById[location]?.let { location ->
            location.groupsById[group]?.let { group ->
                return Pair(location, group)
            }
            throw IllegalStateException("cannot find group")
        }
        throw IllegalStateException("cannot find location")
    }

    fun getLocationGroup(light: Light): Pair<Location, Group> {
        return getLocationGroup(light.location.id, light.group.id)
    }

    fun addListener(listener: IGroupLocationChangeListener) {
        listeners = listeners.plus(listener)
    }

    fun removeListener(listener: IGroupLocationChangeListener) {
        listeners = listeners.minus(listener)
    }

    companion object : ILightServiceExtensionFactory<LifxMessage<LifxMessagePayload>> {
        override fun create(changeDispatcher: ILightsChangeDispatcher): ILightServiceExtension<LifxMessage<LifxMessagePayload>> {
            return LocationGroupService(changeDispatcher)
        }
    }
}

val StateGroup.id
    get() = group.getGuidFromByteArray()

val StateLocation.id
    get() = location.getGuidFromByteArray()

fun Array<Byte>.getGuidFromByteArray(): String {
    val buffer = StringBuilder()
    for (i in indices) {
        buffer.append(String.format("%02x", this[i]))
    }
    return buffer.toString()
}

fun Light.group(): Group? {
    return source.extensionOf(LocationGroupService::class)?.getLocationGroup(this)?.second
}

fun Light.location(): Location? {
    return source.extensionOf(LocationGroupService::class)?.getLocationGroup(this)?.first
}
