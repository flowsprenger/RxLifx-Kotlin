package wo.lf.lifx.api

import io.reactivex.schedulers.TestScheduler
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import wo.lf.lifx.domain.StateGroup
import wo.lf.lifx.domain.StateLocation

class LocationGroupManagerTest : Spek({

    lateinit var scheduler: TestScheduler
    lateinit var lightSource: TestLightSource

    beforeEachTest {
        scheduler = TestScheduler()
        lightSource = TestLightSource(scheduler, scheduler)
    }

    fun createTestLight(id: Long): Light {
        return Light(id, lightSource, TestLightChangeDispatcher())
    }

    context("a location and group manager") {
        lateinit var manager: LocationGroupManager
        lateinit var groupLocationChangeListener: TestGroupLocationChangeListener

        beforeEachTest {
            groupLocationChangeListener = TestGroupLocationChangeListener()
            manager = LocationGroupManager(TestLightsChangeDispatcher(), groupLocationChangeListener)
        }

        on("light added with default location and group") {
            val light = createTestLight(1)

            manager.onLightAdded(light)

            it("creates one location with default id") {
                assertEquals(1, manager.locations.size)
                assertEquals(Light.defaultLocation.id, manager.locations.first().id)
                assertEquals("", manager.locations.first().name)
                assertEquals(1, manager.locations.first().lights.size)
                assertEquals(light, manager.locations.first().lights.first())
            }

            it("creates one group with default id inside location") {
                assertEquals(1, manager.locations.first().groups.size)
                assertEquals(Light.defaultGroup.id, manager.locations.first().groups.first().id)
                assertEquals("", manager.locations.first().groups.first().name)
                assertEquals(1, manager.locations.first().groups.first().lights.size)
                assertEquals(light, manager.locations.first().groups.first().lights.first())
            }
        }

        on("adding light") {
            val light = createTestLight(1)

            var hasNotifiedLocationAddition = 0
            groupLocationChangeListener.locationAddedImpl = { hasNotifiedLocationAddition++ }

            var hasNotifiedGroupAddition = 0
            groupLocationChangeListener.groupAddedImpl = { _, _ -> hasNotifiedGroupAddition++ }

            var hasNotifiedLocationGroupChange = 0
            groupLocationChangeListener.locationGroupChangedImpl = { _, _, _ -> hasNotifiedLocationGroupChange++ }

            manager.onLightAdded(light)

            it("notifies a new location") {
                assertEquals(1, hasNotifiedLocationAddition)
            }

            it("notifies a new group") {
                assertEquals(1, hasNotifiedGroupAddition)
            }

            it("notifies location and group changed") {
                assertEquals(1, hasNotifiedLocationGroupChange)
            }
        }

        on("light added with non default location and group") {

            val locationId = Array(8) { 1.toByte() }
            val groupId = Array(8) { 1.toByte() }

            val light = createTestLight(2).apply {
                this.location = StateLocation(locationId, "Location Light 2".toByteArray(), 15)
                this.group = StateGroup(groupId, "Group Light 2".toByteArray(), 15)
            }
            manager.onLightAdded(light)

            it("creates one location with default id") {
                assertEquals(1, manager.locations.size)
                assertEquals(locationId.getGuidFromByteArray(), manager.locations.first().id)
                assertEquals("Location Light 2", manager.locations.first().name)
                assertEquals(1, manager.locations.first().lights.size)
                assertEquals(light, manager.locations.first().lights.first())
            }

            it("creates one group with default id inside location") {
                assertEquals(1, manager.locations.first().groups.size)
                assertEquals(groupId.getGuidFromByteArray(), manager.locations.first().groups.first().id)
                assertEquals("Group Light 2", manager.locations.first().groups.first().name)
                assertEquals(1, manager.locations.first().groups.first().lights.size)
                assertEquals(light, manager.locations.first().groups.first().lights.first())
            }
        }

        on("light added with default location and group and location changes") {
            val light = createTestLight(1)

            manager.onLightAdded(light)

            val originalLocation = light.location
            val locationId = Array(8) { 1.toByte() }
            light.location = StateLocation(locationId, "Location Light 2".toByteArray(), 15)

            var hasNotifiedLocationRenoval = 0
            groupLocationChangeListener.locationRemovedImpl = { hasNotifiedLocationRenoval++ }

            var hasNotifiedLocationAddition = 0
            groupLocationChangeListener.locationAddedImpl = { hasNotifiedLocationAddition++ }

            var hasNotifiedGroupRemoval = 0
            groupLocationChangeListener.groupRemovedImpl = { _, _ -> hasNotifiedGroupRemoval++ }

            var hasNotifiedGroupAddition = 0
            groupLocationChangeListener.groupAddedImpl = { _, _ -> hasNotifiedGroupAddition++ }

            var hasNotifiedLocationGroupChange = 0
            groupLocationChangeListener.locationGroupChangedImpl = { _, _, _ -> hasNotifiedLocationGroupChange++ }

            manager.onLightChange(light, LightProperty.Location, originalLocation, light.location)

            it("should only have 1 location") {
                assertEquals(1, manager.locations.size)
                assertEquals(locationId.getGuidFromByteArray(), manager.locations.first().id)
            }

            it("location should contain light") {
                assertTrue(manager.locations.first().lights.contains(light))
            }

            it("group is removed") {
                assertEquals(1, hasNotifiedGroupRemoval)
            }

            it("location is removed") {
                assertEquals(1, hasNotifiedLocationRenoval)
            }

            it("new group is added") {
                assertEquals(1, hasNotifiedGroupAddition)
            }

            it("new location is added") {
                assertEquals(1, hasNotifiedLocationAddition)
            }

            it("change is notified") {
                assertEquals(1, hasNotifiedLocationGroupChange)
            }
        }

        on("two lights added to same group and location") {

            val locationId = Array(8) { 1.toByte() }
            val groupId = Array(8) { 1.toByte() }

            val light = createTestLight(2).apply {
                this.location = StateLocation(locationId, "Location Light 2".toByteArray(), 15)
                this.group = StateGroup(groupId, "Newer Group".toByteArray(), 20)
            }
            manager.onLightAdded(light)

            val light2 = createTestLight(3).apply {
                this.location = StateLocation(locationId, "Newer Location".toByteArray(), 20)
                this.group = StateGroup(groupId, "Group Light 2".toByteArray(), 15)
            }
            manager.onLightAdded(light2)

            it("location name is derived from location which has latest update") {
                assertEquals("Newer Location", manager.locations.first().name)
            }

            it("group name is derived from group which has latest update") {
                assertEquals("Newer Group", manager.locations.first().groups.first().name)
            }
        }
    }
})

class TestGroupLocationChangeListener : IGroupLocationChangeListener {
    var locationAddedImpl: ((Location) -> Unit)? = null
    override fun locationAdded(newLocation: Location) {
        locationAddedImpl?.invoke(newLocation)
    }

    var groupAddedImpl: ((Location, Group) -> Unit)? = null
    override fun groupAdded(location: Location, group: Group) {
        groupAddedImpl?.invoke(location, group)
    }

    var locationGroupChangedImpl: ((Location, Group, Light) -> Unit)? = null
    override fun locationGroupChanged(location: Location, group: Group, light: Light) {
        locationGroupChangedImpl?.invoke(location, group, light)
    }

    var groupRemovedImpl: ((Location, Group) -> Unit)? = null
    override fun groupRemoved(location: Location, group: Group) {
        groupRemovedImpl?.invoke(location, group)
    }

    var locationRemovedImpl: ((Location) -> Unit)? = null
    override fun locationRemoved(location: Location) {
        locationRemovedImpl?.invoke(location)
    }

}

class TestLightsChangeDispatcher : TestLightChangeDispatcher(), ILightsChangeDispatcher {
    override fun onLightAdded(light: Light) {
    }
}