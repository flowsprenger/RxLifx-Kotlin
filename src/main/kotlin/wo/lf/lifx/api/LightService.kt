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

import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import wo.lf.lifx.domain.Lifx
import wo.lf.lifx.domain.LifxMessagePayload
import wo.lf.lifx.extensions.discardBroadcasts
import wo.lf.lifx.extensions.fireAndForget
import wo.lf.lifx.net.SourcedLifxMessage
import wo.lf.lifx.net.TargetedLifxMessage
import wo.lf.lifx.net.TransportFactory
import java.util.concurrent.TimeUnit

interface ILightFactory {
    fun create(id: Long, source: ILightSource<LifxMessage<LifxMessagePayload>>, changeDispatcher: ILightsChangeDispatcher): Light
}

object DefaultLightFactory: ILightFactory {
    override fun create(id: Long, source: ILightSource<LifxMessage<LifxMessagePayload>>, changeDispatcher: ILightsChangeDispatcher): Light {
        return Light(id, source, changeDispatcher)
    }
}

interface ILightSource<T> {
    fun send(message: TargetedLifxMessage<T>): Boolean
    val tick: Observable<Long>
    val sourceId: Int
    val messages: Flowable<SourcedLifxMessage<T>>
}

class LightService(transportFactory: TransportFactory, private val changeDispatcher: ILightsChangeDispatcher, private val lightFactory:ILightFactory = DefaultLightFactory, ioScheduler: Scheduler = Schedulers.io(), observeScheduler: Scheduler = Schedulers.single()) : ILightSource<LifxMessage<LifxMessagePayload>> {

    private val transport = transportFactory.create(0, LifxMessageParserImpl())
    private val legacyTransport = transportFactory.create(Lifx.defaultPort, LifxMessageParserImpl())

    override val messages: Flowable<SourcedLifxMessage<LifxMessage<LifxMessagePayload>>> = transport.messages.retryConnect().subscribeOn(ioScheduler).mergeWith(legacyTransport.messages.retryConnect().subscribeOn(ioScheduler)).observeOn(observeScheduler).discardBroadcasts().share()
    override val tick: Observable<Long> = Observable.interval(5L, TimeUnit.SECONDS).share()
    override val sourceId: Int = (Math.random() * Int.MAX_VALUE).toInt()

    private val disposables = CompositeDisposable()

    override fun send(message: TargetedLifxMessage<LifxMessage<LifxMessagePayload>>): Boolean {
        return transport.send(message)
    }

    fun start(){
        disposables.add(messages.groupBy { it.message.header.target }.subscribe{ lightMessages ->
            lightFactory.create(lightMessages.key!!, this@LightService, changeDispatcher).apply {
                changeDispatcher.onLightAdded(this)
                disposables.add(attach(lightMessages))
            }
        })

        disposables.add(tick.subscribe {
            BroadcastGetServiceCommand.create(this).fireAndForget()
        })

        BroadcastGetServiceCommand.create(this).fireAndForget()
    }

    fun stop(){
        disposables.clear()
    }
}

private fun <T> Flowable<T>.retryConnect(): Flowable<T> {
    return retryWhen { error -> error.flatMap { Flowable.timer(2, TimeUnit.SECONDS) } }
}
