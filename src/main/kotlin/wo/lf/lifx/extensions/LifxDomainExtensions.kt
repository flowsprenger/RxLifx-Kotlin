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

package wo.lf.lifx.extensions

import io.reactivex.Flowable
import wo.lf.lifx.api.LifxMessage
import wo.lf.lifx.domain.Header
import wo.lf.lifx.domain.LifxMessagePayload
import wo.lf.lifx.net.SourcedLifxMessage
import wo.lf.lifx.net.TargetedLifxMessage
import java.net.InetAddress
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

val broadcastAddress: InetAddress = InetAddress.getByName("255.255.255.255")

fun <T> T.target(source: Int, target: Long, sequence: Byte = 0): LifxMessage<T> where T : LifxMessagePayload {
    return assemble(source, 0L, sequence).apply { header.setTarget(target) }
}

fun <T> T.targetTo(source: Int, target: Long, address: InetAddress, sequence: Byte = 0): TargetedLifxMessage<LifxMessage<T>> where T : LifxMessagePayload {
    return TargetedLifxMessage(target(source, target, sequence), address)
}

fun <T> T.broadcast(source: Int): LifxMessage<T> where T : LifxMessagePayload {
    return assemble(source, 0L, 0.toByte()).apply { header.setBroadcast() }
}

fun <T> T.broadcastTo(source: Int): TargetedLifxMessage<LifxMessage<T>> where T : LifxMessagePayload {
    return TargetedLifxMessage(broadcast(source), broadcastAddress)
}

fun <T> T.assemble(source: Int, target: Long, sequence: Byte): LifxMessage<T> where T : LifxMessagePayload {
    return LifxMessage(Header((36 + _size).toShort(), 0, source, target, arrayOf(0, 0, 0, 0, 0, 0), 0.toByte(), sequence, 0L, _type.toShort(), 0.toShort()), this)
}

object LifxDefaults {
    const val LIFX_PROTOCOL = 1024
    const val RESPONSE_REQUIRED = 0x1.toByte()
    const val ACK_REQUIRED = 0x2.toByte()
}

fun Header.setBroadcast() {
    target = 0
    protocolOriginTagged = LifxDefaults.LIFX_PROTOCOL.or(1.shl(13)).or(1.shl(12)).toShort()
}

fun Header.setTarget(target: Long) {
    this.target = target
    protocolOriginTagged = LifxDefaults.LIFX_PROTOCOL.or(1.shl(12)).toShort()
}

fun Header.setResponseRequired(status: Boolean) {
    if (status) {
        flags = flags.or(LifxDefaults.RESPONSE_REQUIRED)
    } else {
        flags = flags.and(LifxDefaults.RESPONSE_REQUIRED.inv())
    }
}

fun Header.getResponseRequired(): Boolean {
    return (flags.and(LifxDefaults.RESPONSE_REQUIRED)) != 0.toByte()
}


fun Header.setAckRequired(status: Boolean) {
    if (status) {
        flags = flags.or(LifxDefaults.ACK_REQUIRED)
    } else {
        flags = flags.and(LifxDefaults.ACK_REQUIRED.inv())
    }
}

fun Header.getAckRequired(): Boolean {
    return (flags.and(LifxDefaults.ACK_REQUIRED)) != 0.toByte()
}

fun Flowable<SourcedLifxMessage<LifxMessage<LifxMessagePayload>>>.discardBroadcasts():Flowable<SourcedLifxMessage<LifxMessage<LifxMessagePayload>>>{
    return filter{ it.message.header.target != 0L }
}