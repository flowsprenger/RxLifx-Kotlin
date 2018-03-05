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

import wo.lf.lifx.domain.Header
import wo.lf.lifx.domain.LifxMessagePayload
import wo.lf.lifx.domain.payloadFactories
import wo.lf.lifx.net.LifxMessageParser
import java.nio.ByteBuffer
import java.nio.ByteOrder


class LifxMessageParserImpl : LifxMessageParser<LifxMessage<LifxMessagePayload>> {
    override fun parse(buffer: ByteBuffer, size: Int, type: Int): LifxMessage<LifxMessagePayload>? {
        buffer.mark()
        val header = Header(buffer)
        payloadFactories[type]?.let {
            return LifxMessage(header, it.parse(buffer))
        }
        // unknown message skip over payload
        buffer.position(buffer.position() + header.size - header._size)
        return null
    }

    override fun serialise(message: LifxMessage<LifxMessagePayload>): ByteBuffer {
        return message.toByteBuffer()
    }

}

data class LifxMessage<out T : LifxMessagePayload>(val header: Header, val payload: T) {
    fun toByteBuffer(): ByteBuffer {
        return ByteBuffer.allocate(header._size + payload._size).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            header.addToByteBuffer(this)
            payload.addToByteBuffer(this)
        }
    }
}

fun ByteBuffer.getString(count: Int): ByteArray {
    return ByteArray(count, { this@getString.get() })
}