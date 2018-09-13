package wo.lf.lifx.api

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.TestScheduler
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import wo.lf.lifx.domain.LifxMessagePayload
import wo.lf.lifx.net.SourcedLifxMessage
import wo.lf.lifx.net.TargetedLifxMessage
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.reflect.KClass

class CommandsKtTest : Spek({
    context("a light") {

        lateinit var scheduler: TestScheduler
        lateinit var lightSource: TestLightSource
        lateinit var light: Light
        var sendInvocationCount: Int = 0

        beforeEachTest {
            scheduler = TestScheduler()
            lightSource = TestLightSource(scheduler, scheduler)
            light = Light(15, lightSource, TestLightChangeDispatcher())
            sendInvocationCount = 0

            lightSource.sendImpl = {
                sendInvocationCount++
                true
            }
        }

        on("send message without receiving response") {
            val testSubscriber = LightGetCommand.create(light, false, true).test()

            it("should invoke send once") {
                assert(1 == sendInvocationCount)
            }
        }

        on("send message without receiving response await timeout") {
            val testSubscriber = LightGetCommand.create(light, false, true).test()

            scheduler.advanceTimeBy(5L, TimeUnit.SECONDS)

            it("should end with failure after 3 retries") {
                testSubscriber.assertError { it is TimeoutException }
                assert(3 == sendInvocationCount)
            }
        }
    }
})

open class TestLightChangeDispatcher : ILightChangeDispatcher {
    override fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?) {

    }
}

class TestLightSource(override val ioScheduler: Scheduler, override val observeScheduler: Scheduler) : ILightSource<LifxMessage<LifxMessagePayload>> {

    var sendImpl: ((TargetedLifxMessage<LifxMessage<LifxMessagePayload>>) -> Boolean)? = null
    override fun send(message: TargetedLifxMessage<LifxMessage<LifxMessagePayload>>): Boolean {
        return sendImpl?.invoke(message) ?: true
    }

    override val tick: Observable<Long> = Observable.interval(LightService.REFRESH_INTERVAL_SECONDS, TimeUnit.SECONDS, ioScheduler)
    override val sourceId: Int = (Math.random() * 100).toInt()

    val messagePublisher: PublishProcessor<SourcedLifxMessage<LifxMessage<LifxMessagePayload>>> = PublishProcessor.create<SourcedLifxMessage<LifxMessage<LifxMessagePayload>>>()

    override val messages: Flowable<SourcedLifxMessage<LifxMessage<LifxMessagePayload>>> = messagePublisher

    override fun <T : Any> extensionOf(type: KClass<T>): T? {
        return null
    }
}