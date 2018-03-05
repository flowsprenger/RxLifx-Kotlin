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

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.flowables.GroupedFlowable
import wo.lf.lifx.domain.*
import wo.lf.lifx.extensions.capture
import wo.lf.lifx.extensions.fireAndForget
import wo.lf.lifx.net.SourcedLifxMessage
import java.net.InetAddress

enum class LightProperty{
    Label,
    Color,
    Power
}

class Light(val id: Long, val source: ILightSource<LifxMessage<LifxMessagePayload>>, changeDispatcher: ILightChangeDispatcher) {

    lateinit var address: InetAddress

    fun attach(messages: GroupedFlowable<Long, SourcedLifxMessage<LifxMessage<LifxMessagePayload>>>): Disposable {
        val disposables = CompositeDisposable()

        messages.subscribe { message ->
            address = message.source
            val payload = message.message.payload
            when (payload) {
                is LightState -> {
                    label = String(payload.label)
                    color = payload.color
                    power = PowerState.fromValue(payload.power)
                }
                else -> println("${message.message.payload}")
            }
        }.capture(disposables)

        source.tick.subscribe {
            LightGetCommand.create(this).fireAndForget()
        }.capture(disposables)

        return disposables
    }

    var label: String by LightChangeNotifier(LightProperty.Label, "", changeDispatcher)
    var color: HSBK by LightChangeNotifier(LightProperty.Color, Lifx.defaultColor, changeDispatcher)
    var power: PowerState by LightChangeNotifier(LightProperty.Power, PowerState.OFF, changeDispatcher)

    private var sequence: Byte = 0
    fun getNextSequence(): Byte = sequence.inc()
}
