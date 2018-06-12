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

package wo.lf.lifx.net

import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import wo.lf.lifx.domain.Lifx
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.NotYetConnectedException

interface LifxMessageParser<T> {
    fun parse(buffer: ByteBuffer, size: Int, type: Int): T?
    fun serialise(message: T): ByteBuffer
}

class TargetedLifxMessage<out T>(val message: T, val target: InetAddress)

class SourcedLifxMessage<out T>(val message: T, val source: InetAddress)

interface Transport<T> {
    val messages: Flowable<SourcedLifxMessage<T>>
    fun send(message: TargetedLifxMessage<T>): Boolean
}

interface TransportFactory {
    fun <T> create(port: Int, parser: LifxMessageParser<T>): Transport<T>
}

interface DatagramSocketFactory {
    fun create(): DatagramSocket
}

object DefaultDatagramSocketFactory : DatagramSocketFactory {
    override fun create(): DatagramSocket = DatagramSocket(null)
}

class UdpTransport<T>(val port: Int, private val parser: LifxMessageParser<T>, private val datagramSocketFactory: DatagramSocketFactory = DefaultDatagramSocketFactory) : Transport<T> {

    private val publisher = PublishSubject.create<TargetedLifxMessage<T>>()

    private var isConnected = false

    override val messages: Flowable<SourcedLifxMessage<T>> = Flowable.create({ emitter ->
        val channel = datagramSocketFactory.create().apply {
            reuseAddress = true
            bind(InetSocketAddress(this@UdpTransport.port))
        }
        val buffer = ByteBuffer.allocate(1024).apply {
            clear()
            order(ByteOrder.LITTLE_ENDIAN)
        }
        val datagram = DatagramPacket(buffer.array(), 1024)

        val disposable = publisher.observeOn(Schedulers.io()).subscribe {
            try {
                with(parser.serialise(it.message)) {
                    channel.send(DatagramPacket(array(), position(), it.target, Lifx.defaultPort))
                }
            } catch (e: NotYetConnectedException) {
                channel.disconnect()
            }
        }

        isConnected = true

        try {
            while (!emitter.isCancelled) {
                val startingPosition = buffer.position()
                buffer.mark()
                channel.receive(datagram)
                var length = datagram.length
                while (length >= 36) {
                    buffer.mark()
                    val size = buffer.getShort(buffer.position()).toInt()
                    val type = buffer.getShort(buffer.position() + 32).toInt()
                    if (length >= size) {
                        parser.parse(buffer, size, type)?.let { message ->
                            emitter.onNext(SourcedLifxMessage(message, datagram.address))
                        }
                    }
                    if (buffer.position() == length + startingPosition) {
                        buffer.clear()
                    }
                    length -= size
                }
            }
        } catch (e: IOException) {
            if (!emitter.isCancelled) {
                emitter.onError(e)
            }
        } catch (e: Exception) {
            if (!emitter.isCancelled) {
                emitter.onError(e)
            }
        } finally {
            isConnected = false
            disposable.dispose()
            channel.disconnect()
        }


    }, BackpressureStrategy.BUFFER)

    override fun send(message: TargetedLifxMessage<T>): Boolean {
        return if (isConnected) {
            publisher.onNext(message)
            true
        } else {
            false
        }
    }

    companion object : TransportFactory {
        override fun <T> create(port: Int, parser: LifxMessageParser<T>): Transport<T> {
            return UdpTransport(port, parser)
        }
    }
}


