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

import io.reactivex.Completable
import io.reactivex.Maybe
import wo.lf.lifx.domain.*
import wo.lf.lifx.extensions.broadcastTo
import wo.lf.lifx.extensions.targetTo
import java.util.concurrent.TimeUnit

object BroadcastGetServiceCommand {
    fun create(lightSource: LightService): Completable{
        return lightSource.broadcast(GetService())
    }
}

object  LightGetCommand {
    fun create(light: Light, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<LightState> {
        return light.send(LightGet(), ackRequired, responseRequired)
    }
}

fun LightService.broadcast(payload: LifxMessagePayload): Completable {
    return Completable.create {
        if(send(payload.broadcastTo(sourceId))) {
            it.onComplete()
        }else{
            it.onError(TransportNotConnectedException())
        }
    }
}

inline fun <reified R> Light.send(payload: LifxMessagePayload, ackRequired: Boolean = false, responseRequired: Boolean = false): Maybe<R>{
    return Maybe.create<R> { emitter ->
        if(ackRequired || responseRequired) {
            var awaitingAck = ackRequired
            var awaitingResponse = responseRequired
            var response: R? = null
            val sequence = getNextSequence()
            source.send(payload.targetTo(source.sourceId, id, address, sequence))
            source.messages.filter{ it.message.header.target == id && it.message.header.source == source.sourceId && it.message.header.sequence == sequence }.timeout(2L, TimeUnit.SECONDS).subscribe({
                if(awaitingAck && it.message.header.type == MessageType.Acknowledgement.value){
                    awaitingAck = false
                }

                if(awaitingResponse && it.message.payload is R){
                    response = it.message.payload
                    awaitingResponse = false
                }

                if(!awaitingResponse && !awaitingAck){
                    if(awaitingResponse){
                        emitter.onSuccess(response!!)
                    }else{
                        emitter.onComplete()
                    }
                }
            }, { emitter.onError(it) })
        }else{
            source.send(payload.targetTo(source.sourceId, id, address))
            emitter.onComplete()
        }
    }
}

class TransportNotConnectedException : Throwable()