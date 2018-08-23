package wo.lf.lifx.api

import wo.lf.lifx.domain.StateGroup
import wo.lf.lifx.domain.StateLocation

interface IGroupLocationChangeListener {
    fun locationAdded(newLocation: Location)
    fun groupAdded(location: Location, group: Group)
    fun locationGroupChanged(location: Location, group: Group, light: Light)
    fun groupRemoved(location: Location, group: Group)
    fun locationRemoved(location: Location)

}

class LocationGroupManager(private val wrappedChangeDispatcher: ILightsChangeDispatcher, private val groupLocationChangeListener: IGroupLocationChangeListener) : ILightsChangeDispatcher {

    private val locationsById: MutableMap<String, Location> = mutableMapOf()

    val locations: List<Location>
        get() = locationsById.values.toList()

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
                groupLocationChangeListener.locationAdded(location)
            }
            if (newGroup) {
                groupLocationChangeListener.groupAdded(location, group)
            }
            groupLocationChangeListener.locationGroupChanged(location, group, light)
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
                groupLocationChangeListener.locationGroupChanged(location, group, light)
            } else {
                val (location, group) = getLocationGroup(oldLocation.id, light.group.id)
                group.lights -= light
                if (group.lights.isEmpty()) {
                    location.groupsById.remove(group.id)
                    groupLocationChangeListener.groupRemoved(location, group)
                }
                if (location.groups.isEmpty()) {
                    locationsById.remove(location.id)
                    groupLocationChangeListener.locationRemoved(location)
                }
                addToLocationAndGroup(light)
            }
        }
    }

    private fun changeGroup(light: Light, oldGroup: StateGroup, newGroup: StateGroup) {
        synchronized(this) {
            if (oldGroup.group.contentEquals(newGroup.group)) {
                val (location, group) = getLocationGroup(light.location.id, newGroup.id)
                groupLocationChangeListener.locationGroupChanged(location, group, light)
            } else {
                val (location, group) = getLocationGroup(light.location.id, oldGroup.id)
                group.lights -= light
                if (group.lights.isEmpty()) {
                    location.groupsById.remove(group.id)
                    groupLocationChangeListener.groupRemoved(location, group)
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


