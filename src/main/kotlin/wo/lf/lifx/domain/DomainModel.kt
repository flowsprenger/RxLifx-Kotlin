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

package wo.lf.lifx.domain

import wo.lf.lifx.api.getString
import java.nio.ByteBuffer

class Header : LifxMessageSerializable {

    override val _size = 36


    var size: Short
    var protocolOriginTagged: Short
    var source: Int
    var target: Long
    var reserved: Array<Byte>
    var flags: Byte
    var sequence: Byte
    var reserved1: Long
    var type: Short
    var reserved2: Short

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putShort(size)
        buffer.putShort(protocolOriginTagged)
        buffer.putInt(source)
        buffer.putLong(target)
        assert(reserved.size == 6); reserved.forEach { buffer.put(it) }
        buffer.put(flags)
        buffer.put(sequence)
        buffer.putLong(reserved1)
        buffer.putShort(type)
        buffer.putShort(reserved2)
        return buffer
    }

    constructor(size: Short, protocolOriginTagged: Short, source: Int, target: Long, reserved: Array<Byte>, flags: Byte, sequence: Byte, reserved1: Long, type: Short, reserved2: Short) {
        this.size = size
        this.protocolOriginTagged = protocolOriginTagged
        this.source = source
        this.target = target
        this.reserved = reserved
        this.flags = flags
        this.sequence = sequence
        this.reserved1 = reserved1
        this.type = type
        this.reserved2 = reserved2
    }

    constructor(buffer: ByteBuffer) {
        size = buffer.getShort()
        protocolOriginTagged = buffer.getShort()
        source = buffer.getInt()
        target = buffer.getLong()
        reserved = (0 until 6).map { buffer.get() }.toTypedArray()
        flags = buffer.get()
        sequence = buffer.get()
        reserved1 = buffer.getLong()
        type = buffer.getShort()
        reserved2 = buffer.getShort()
    }

    override fun equals(other: Any?): Boolean {
        if (other is Header) {
            return true

                    && size == other.size


                    && protocolOriginTagged == other.protocolOriginTagged


                    && source == other.source


                    && target == other.target


                    && reserved == other.reserved


                    && flags == other.flags


                    && sequence == other.sequence


                    && reserved1 == other.reserved1


                    && type == other.type


                    && reserved2 == other.reserved2

        }
        return false
    }

    override fun toString(): String {
        return "Header : size:$size, protocolOriginTagged:$protocolOriginTagged, source:$source, target:$target, reserved:$reserved, flags:$flags, sequence:$sequence, reserved1:$reserved1, type:$type, reserved2:$reserved2, "
    }


}

class GetService : LifxMessagePayload {

    override val _size = 0

    override val _type = 2


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetService) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetService : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetService(buffer)
        }
    }

}

class StateService : LifxMessagePayload {

    override val _size = 5

    override val _type = 3


    var service: Service
    var port: Int

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.put((service.value))
        buffer.putInt(port)
        return buffer
    }

    constructor(service: Service, port: Int) {
        this.service = service
        this.port = port
    }

    constructor(buffer: ByteBuffer) {
        service = Service.fromValue(buffer.get())
        port = buffer.getInt()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateService) {
            return true

                    && service == other.service


                    && port == other.port

        }
        return false
    }

    override fun toString(): String {
        return "StateService : service:$service, port:$port, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateService(buffer)
        }
    }

}

class GetHostInfo : LifxMessagePayload {

    override val _size = 0

    override val _type = 12


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetHostInfo) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetHostInfo : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetHostInfo(buffer)
        }
    }

}

class StateHostInfo : LifxMessagePayload {

    override val _size = 14

    override val _type = 13


    var signal: Float
    var tx: Int
    var rx: Int
    var reserved: Short

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putFloat(signal)
        buffer.putInt(tx)
        buffer.putInt(rx)
        buffer.putShort(reserved)
        return buffer
    }

    constructor(signal: Float, tx: Int, rx: Int, reserved: Short) {
        this.signal = signal
        this.tx = tx
        this.rx = rx
        this.reserved = reserved
    }

    constructor(buffer: ByteBuffer) {
        signal = buffer.getFloat()
        tx = buffer.getInt()
        rx = buffer.getInt()
        reserved = buffer.getShort()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateHostInfo) {
            return true

                    && signal == other.signal


                    && tx == other.tx


                    && rx == other.rx


                    && reserved == other.reserved

        }
        return false
    }

    override fun toString(): String {
        return "StateHostInfo : signal:$signal, tx:$tx, rx:$rx, reserved:$reserved, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateHostInfo(buffer)
        }
    }

}

class GetHostFirmware : LifxMessagePayload {

    override val _size = 0

    override val _type = 14


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetHostFirmware) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetHostFirmware : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetHostFirmware(buffer)
        }
    }

}

class StateHostFirmware : LifxMessagePayload {

    override val _size = 20

    override val _type = 15


    var build: Long
    var reserved: Long
    var version: Int

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putLong(build)
        buffer.putLong(reserved)
        buffer.putInt(version)
        return buffer
    }

    constructor(build: Long, reserved: Long, version: Int) {
        this.build = build
        this.reserved = reserved
        this.version = version
    }

    constructor(buffer: ByteBuffer) {
        build = buffer.getLong()
        reserved = buffer.getLong()
        version = buffer.getInt()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateHostFirmware) {
            return true

                    && build == other.build


                    && reserved == other.reserved


                    && version == other.version

        }
        return false
    }

    override fun toString(): String {
        return "StateHostFirmware : build:$build, reserved:$reserved, version:$version, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateHostFirmware(buffer)
        }
    }

}

class GetWifiInfo : LifxMessagePayload {

    override val _size = 0

    override val _type = 16


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetWifiInfo) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetWifiInfo : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetWifiInfo(buffer)
        }
    }

}

class StateWifiInfo : LifxMessagePayload {

    override val _size = 14

    override val _type = 17


    var signal: Float
    var tx: Int
    var rx: Int
    var reserved: Short

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putFloat(signal)
        buffer.putInt(tx)
        buffer.putInt(rx)
        buffer.putShort(reserved)
        return buffer
    }

    constructor(signal: Float, tx: Int, rx: Int, reserved: Short) {
        this.signal = signal
        this.tx = tx
        this.rx = rx
        this.reserved = reserved
    }

    constructor(buffer: ByteBuffer) {
        signal = buffer.getFloat()
        tx = buffer.getInt()
        rx = buffer.getInt()
        reserved = buffer.getShort()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateWifiInfo) {
            return true

                    && signal == other.signal


                    && tx == other.tx


                    && rx == other.rx


                    && reserved == other.reserved

        }
        return false
    }

    override fun toString(): String {
        return "StateWifiInfo : signal:$signal, tx:$tx, rx:$rx, reserved:$reserved, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateWifiInfo(buffer)
        }
    }

}

class GetWifiFirmware : LifxMessagePayload {

    override val _size = 0

    override val _type = 18


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetWifiFirmware) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetWifiFirmware : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetWifiFirmware(buffer)
        }
    }

}

class StateWifiFirmware : LifxMessagePayload {

    override val _size = 20

    override val _type = 19


    var build: Long
    var reserved: Long
    var version: Int

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putLong(build)
        buffer.putLong(reserved)
        buffer.putInt(version)
        return buffer
    }

    constructor(build: Long, reserved: Long, version: Int) {
        this.build = build
        this.reserved = reserved
        this.version = version
    }

    constructor(buffer: ByteBuffer) {
        build = buffer.getLong()
        reserved = buffer.getLong()
        version = buffer.getInt()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateWifiFirmware) {
            return true

                    && build == other.build


                    && reserved == other.reserved


                    && version == other.version

        }
        return false
    }

    override fun toString(): String {
        return "StateWifiFirmware : build:$build, reserved:$reserved, version:$version, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateWifiFirmware(buffer)
        }
    }

}

class GetPower : LifxMessagePayload {

    override val _size = 0

    override val _type = 20


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetPower) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetPower : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetPower(buffer)
        }
    }

}

class SetPower : LifxMessagePayload {

    override val _size = 2

    override val _type = 21


    var level: Short

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putShort(level)
        return buffer
    }

    constructor(level: Short) {
        this.level = level
    }

    constructor(buffer: ByteBuffer) {
        level = buffer.getShort()
    }

    override fun equals(other: Any?): Boolean {
        if (other is SetPower) {
            return true

                    && level == other.level

        }
        return false
    }

    override fun toString(): String {
        return "SetPower : level:$level, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return SetPower(buffer)
        }
    }

}

class StatePower : LifxMessagePayload {

    override val _size = 2

    override val _type = 22


    var level: Short

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putShort(level)
        return buffer
    }

    constructor(level: Short) {
        this.level = level
    }

    constructor(buffer: ByteBuffer) {
        level = buffer.getShort()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StatePower) {
            return true

                    && level == other.level

        }
        return false
    }

    override fun toString(): String {
        return "StatePower : level:$level, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StatePower(buffer)
        }
    }

}

class GetLabel : LifxMessagePayload {

    override val _size = 0

    override val _type = 23


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetLabel) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetLabel : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetLabel(buffer)
        }
    }

}

class SetLabel : LifxMessagePayload {

    override val _size = 32

    override val _type = 24


    var label: ByteArray

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        (0 until 32).forEach { buffer.put(label[it]) }
        return buffer
    }

    constructor(label: ByteArray) {
        this.label = label
    }

    constructor(buffer: ByteBuffer) {
        label = buffer.getString(32)
    }

    override fun equals(other: Any?): Boolean {
        if (other is SetLabel) {
            return true

                    && label.contentEquals(other.label)

        }
        return false
    }

    override fun toString(): String {
        return "SetLabel : label:$label, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return SetLabel(buffer)
        }
    }

}

class StateLabel : LifxMessagePayload {

    override val _size = 32

    override val _type = 25


    var label: ByteArray

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        (0 until 32).forEach { buffer.put(label[it]) }
        return buffer
    }

    constructor(label: ByteArray) {
        this.label = label
    }

    constructor(buffer: ByteBuffer) {
        label = buffer.getString(32)
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateLabel) {
            return true

                    && label.contentEquals(other.label)

        }
        return false
    }

    override fun toString(): String {
        return "StateLabel : label:$label, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateLabel(buffer)
        }
    }

}

class GetVersion : LifxMessagePayload {

    override val _size = 0

    override val _type = 32


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetVersion) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetVersion : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetVersion(buffer)
        }
    }

}

class StateVersion : LifxMessagePayload {

    override val _size = 12

    override val _type = 33


    var vendor: Int
    var product: Int
    var version: Int

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putInt(vendor)
        buffer.putInt(product)
        buffer.putInt(version)
        return buffer
    }

    constructor(vendor: Int, product: Int, version: Int) {
        this.vendor = vendor
        this.product = product
        this.version = version
    }

    constructor(buffer: ByteBuffer) {
        vendor = buffer.getInt()
        product = buffer.getInt()
        version = buffer.getInt()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateVersion) {
            return true

                    && vendor == other.vendor


                    && product == other.product


                    && version == other.version

        }
        return false
    }

    override fun toString(): String {
        return "StateVersion : vendor:$vendor, product:$product, version:$version, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateVersion(buffer)
        }
    }

}

class GetInfo : LifxMessagePayload {

    override val _size = 0

    override val _type = 34


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetInfo) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetInfo : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetInfo(buffer)
        }
    }

}

class StateInfo : LifxMessagePayload {

    override val _size = 24

    override val _type = 35


    var time: Long
    var uptime: Long
    var downtime: Long

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putLong(time)
        buffer.putLong(uptime)
        buffer.putLong(downtime)
        return buffer
    }

    constructor(time: Long, uptime: Long, downtime: Long) {
        this.time = time
        this.uptime = uptime
        this.downtime = downtime
    }

    constructor(buffer: ByteBuffer) {
        time = buffer.getLong()
        uptime = buffer.getLong()
        downtime = buffer.getLong()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateInfo) {
            return true

                    && time == other.time


                    && uptime == other.uptime


                    && downtime == other.downtime

        }
        return false
    }

    override fun toString(): String {
        return "StateInfo : time:$time, uptime:$uptime, downtime:$downtime, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateInfo(buffer)
        }
    }

}

class Acknowledgement : LifxMessagePayload {

    override val _size = 0

    override val _type = 45


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is Acknowledgement) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "Acknowledgement : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return Acknowledgement(buffer)
        }
    }

}

class GetLocation : LifxMessagePayload {

    override val _size = 0

    override val _type = 48


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetLocation) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetLocation : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetLocation(buffer)
        }
    }

}

class SetLocation : LifxMessagePayload {

    override val _size = 56

    override val _type = 49


    var location: Array<Byte>
    var label: ByteArray
    var updated_at: Long

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        assert(location.size == 16); location.forEach { buffer.put(it) }
        (0 until 32).forEach { buffer.put(label[it]) }
        buffer.putLong(updated_at)
        return buffer
    }

    constructor(location: Array<Byte>, label: ByteArray, updated_at: Long) {
        this.location = location
        this.label = label
        this.updated_at = updated_at
    }

    constructor(buffer: ByteBuffer) {
        location = (0 until 16).map { buffer.get() }.toTypedArray()
        label = buffer.getString(32)
        updated_at = buffer.getLong()
    }

    override fun equals(other: Any?): Boolean {
        if (other is SetLocation) {
            return true

                    && location == other.location


                    && label.contentEquals(other.label)


                    && updated_at == other.updated_at

        }
        return false
    }

    override fun toString(): String {
        return "SetLocation : location:$location, label:$label, updated_at:$updated_at, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return SetLocation(buffer)
        }
    }

}

class StateLocation : LifxMessagePayload {

    override val _size = 56

    override val _type = 50


    var location: Array<Byte>
    var label: ByteArray
    var updated_at: Long

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        assert(location.size == 16); location.forEach { buffer.put(it) }
        (0 until 32).forEach { buffer.put(label[it]) }
        buffer.putLong(updated_at)
        return buffer
    }

    constructor(location: Array<Byte>, label: ByteArray, updated_at: Long) {
        this.location = location
        this.label = label
        this.updated_at = updated_at
    }

    constructor(buffer: ByteBuffer) {
        location = (0 until 16).map { buffer.get() }.toTypedArray()
        label = buffer.getString(32)
        updated_at = buffer.getLong()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateLocation) {
            return true

                    && location == other.location


                    && label.contentEquals(other.label)


                    && updated_at == other.updated_at

        }
        return false
    }

    override fun toString(): String {
        return "StateLocation : location:$location, label:$label, updated_at:$updated_at, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateLocation(buffer)
        }
    }

}

class GetGroup : LifxMessagePayload {

    override val _size = 0

    override val _type = 51


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetGroup) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetGroup : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetGroup(buffer)
        }
    }

}

class SetGroup : LifxMessagePayload {

    override val _size = 56

    override val _type = 52


    var group: Array<Byte>
    var label: ByteArray
    var updated_at: Long

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        assert(group.size == 16); group.forEach { buffer.put(it) }
        (0 until 32).forEach { buffer.put(label[it]) }
        buffer.putLong(updated_at)
        return buffer
    }

    constructor(group: Array<Byte>, label: ByteArray, updated_at: Long) {
        this.group = group
        this.label = label
        this.updated_at = updated_at
    }

    constructor(buffer: ByteBuffer) {
        group = (0 until 16).map { buffer.get() }.toTypedArray()
        label = buffer.getString(32)
        updated_at = buffer.getLong()
    }

    override fun equals(other: Any?): Boolean {
        if (other is SetGroup) {
            return true

                    && group == other.group


                    && label.contentEquals(other.label)


                    && updated_at == other.updated_at

        }
        return false
    }

    override fun toString(): String {
        return "SetGroup : group:$group, label:$label, updated_at:$updated_at, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return SetGroup(buffer)
        }
    }

}

class StateGroup : LifxMessagePayload {

    override val _size = 56

    override val _type = 53


    var group: Array<Byte>
    var label: ByteArray
    var updated_at: Long

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        assert(group.size == 16); group.forEach { buffer.put(it) }
        (0 until 32).forEach { buffer.put(label[it]) }
        buffer.putLong(updated_at)
        return buffer
    }

    constructor(group: Array<Byte>, label: ByteArray, updated_at: Long) {
        this.group = group
        this.label = label
        this.updated_at = updated_at
    }

    constructor(buffer: ByteBuffer) {
        group = (0 until 16).map { buffer.get() }.toTypedArray()
        label = buffer.getString(32)
        updated_at = buffer.getLong()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateGroup) {
            return true

                    && group == other.group


                    && label.contentEquals(other.label)


                    && updated_at == other.updated_at

        }
        return false
    }

    override fun toString(): String {
        return "StateGroup : group:$group, label:$label, updated_at:$updated_at, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateGroup(buffer)
        }
    }

}

class EchoRequest : LifxMessagePayload {

    override val _size = 64

    override val _type = 58


    var payload: Array<Byte>

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        assert(payload.size == 64); payload.forEach { buffer.put(it) }
        return buffer
    }

    constructor(payload: Array<Byte>) {
        this.payload = payload
    }

    constructor(buffer: ByteBuffer) {
        payload = (0 until 64).map { buffer.get() }.toTypedArray()
    }

    override fun equals(other: Any?): Boolean {
        if (other is EchoRequest) {
            return true

                    && payload == other.payload

        }
        return false
    }

    override fun toString(): String {
        return "EchoRequest : payload:$payload, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return EchoRequest(buffer)
        }
    }

}

class EchoResponse : LifxMessagePayload {

    override val _size = 64

    override val _type = 59


    var payload: Array<Byte>

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        assert(payload.size == 64); payload.forEach { buffer.put(it) }
        return buffer
    }

    constructor(payload: Array<Byte>) {
        this.payload = payload
    }

    constructor(buffer: ByteBuffer) {
        payload = (0 until 64).map { buffer.get() }.toTypedArray()
    }

    override fun equals(other: Any?): Boolean {
        if (other is EchoResponse) {
            return true

                    && payload == other.payload

        }
        return false
    }

    override fun toString(): String {
        return "EchoResponse : payload:$payload, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return EchoResponse(buffer)
        }
    }

}

class HSBK : LifxMessageSerializable {

    override val _size = 8


    var hue: Short
    var saturation: Short
    var brightness: Short
    var kelvin: Short

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putShort(hue)
        buffer.putShort(saturation)
        buffer.putShort(brightness)
        buffer.putShort(kelvin)
        return buffer
    }

    constructor(hue: Short, saturation: Short, brightness: Short, kelvin: Short) {
        this.hue = hue
        this.saturation = saturation
        this.brightness = brightness
        this.kelvin = kelvin
    }

    constructor(buffer: ByteBuffer) {
        hue = buffer.getShort()
        saturation = buffer.getShort()
        brightness = buffer.getShort()
        kelvin = buffer.getShort()
    }

    override fun equals(other: Any?): Boolean {
        if (other is HSBK) {
            return true

                    && hue == other.hue


                    && saturation == other.saturation


                    && brightness == other.brightness


                    && kelvin == other.kelvin

        }
        return false
    }

    override fun toString(): String {
        return "HSBK : hue:$hue, saturation:$saturation, brightness:$brightness, kelvin:$kelvin, "
    }


}

class LightGet : LifxMessagePayload {

    override val _size = 0

    override val _type = 101


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is LightGet) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "LightGet : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return LightGet(buffer)
        }
    }

}

class LightSetColor : LifxMessagePayload {

    override val _size = 13

    override val _type = 102


    var reserved: Byte
    var color: HSBK
    var duration: Int

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.put(reserved)
        color.addToByteBuffer(buffer)
        buffer.putInt(duration)
        return buffer
    }

    constructor(reserved: Byte, color: HSBK, duration: Int) {
        this.reserved = reserved
        this.color = color
        this.duration = duration
    }

    constructor(buffer: ByteBuffer) {
        reserved = buffer.get()
        color = HSBK(buffer)
        duration = buffer.getInt()
    }

    override fun equals(other: Any?): Boolean {
        if (other is LightSetColor) {
            return true

                    && reserved == other.reserved


                    && color == other.color


                    && duration == other.duration

        }
        return false
    }

    override fun toString(): String {
        return "LightSetColor : reserved:$reserved, color:$color, duration:$duration, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return LightSetColor(buffer)
        }
    }

}

class LightSetWaveform : LifxMessagePayload {

    override val _size = 21

    override val _type = 103


    var reserved: Byte
    var transient: Byte
    var color: HSBK
    var period: Int
    var cycles: Float
    var skew_ratio: Short
    var waveform: WaveformType

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.put(reserved)
        buffer.put(transient)
        color.addToByteBuffer(buffer)
        buffer.putInt(period)
        buffer.putFloat(cycles)
        buffer.putShort(skew_ratio)
        buffer.put((waveform.value))
        return buffer
    }

    constructor(reserved: Byte, transient: Byte, color: HSBK, period: Int, cycles: Float, skew_ratio: Short, waveform: WaveformType) {
        this.reserved = reserved
        this.transient = transient
        this.color = color
        this.period = period
        this.cycles = cycles
        this.skew_ratio = skew_ratio
        this.waveform = waveform
    }

    constructor(buffer: ByteBuffer) {
        reserved = buffer.get()
        transient = buffer.get()
        color = HSBK(buffer)
        period = buffer.getInt()
        cycles = buffer.getFloat()
        skew_ratio = buffer.getShort()
        waveform = WaveformType.fromValue(buffer.get())
    }

    override fun equals(other: Any?): Boolean {
        if (other is LightSetWaveform) {
            return true

                    && reserved == other.reserved


                    && transient == other.transient


                    && color == other.color


                    && period == other.period


                    && cycles == other.cycles


                    && skew_ratio == other.skew_ratio


                    && waveform == other.waveform

        }
        return false
    }

    override fun toString(): String {
        return "LightSetWaveform : reserved:$reserved, transient:$transient, color:$color, period:$period, cycles:$cycles, skew_ratio:$skew_ratio, waveform:$waveform, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return LightSetWaveform(buffer)
        }
    }

}

class LightState : LifxMessagePayload {

    override val _size = 52

    override val _type = 107


    var color: HSBK
    var reserved: Short
    var power: Short
    var label: ByteArray
    var reserved1: Long

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        color.addToByteBuffer(buffer)
        buffer.putShort(reserved)
        buffer.putShort(power)
        (0 until 32).forEach { buffer.put(label[it]) }
        buffer.putLong(reserved1)
        return buffer
    }

    constructor(color: HSBK, reserved: Short, power: Short, label: ByteArray, reserved1: Long) {
        this.color = color
        this.reserved = reserved
        this.power = power
        this.label = label
        this.reserved1 = reserved1
    }

    constructor(buffer: ByteBuffer) {
        color = HSBK(buffer)
        reserved = buffer.getShort()
        power = buffer.getShort()
        label = buffer.getString(32)
        reserved1 = buffer.getLong()
    }

    override fun equals(other: Any?): Boolean {
        if (other is LightState) {
            return true

                    && color == other.color


                    && reserved == other.reserved


                    && power == other.power


                    && label.contentEquals(other.label)


                    && reserved1 == other.reserved1

        }
        return false
    }

    override fun toString(): String {
        return "LightState : color:$color, reserved:$reserved, power:$power, label:$label, reserved1:$reserved1, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return LightState(buffer)
        }
    }

}

class LightSetWaveformOptional : LifxMessagePayload {

    override val _size = 25

    override val _type = 119


    var reserved: Byte
    var transient: Byte
    var color: HSBK
    var period: Int
    var cycles: Float
    var skew_ratio: Short
    var waveform: WaveformType
    var set_hue: Byte
    var set_saturation: Byte
    var set_brightness: Byte
    var set_kelvin: Byte

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.put(reserved)
        buffer.put(transient)
        color.addToByteBuffer(buffer)
        buffer.putInt(period)
        buffer.putFloat(cycles)
        buffer.putShort(skew_ratio)
        buffer.put((waveform.value))
        buffer.put(set_hue)
        buffer.put(set_saturation)
        buffer.put(set_brightness)
        buffer.put(set_kelvin)
        return buffer
    }

    constructor(reserved: Byte, transient: Byte, color: HSBK, period: Int, cycles: Float, skew_ratio: Short, waveform: WaveformType, set_hue: Byte, set_saturation: Byte, set_brightness: Byte, set_kelvin: Byte) {
        this.reserved = reserved
        this.transient = transient
        this.color = color
        this.period = period
        this.cycles = cycles
        this.skew_ratio = skew_ratio
        this.waveform = waveform
        this.set_hue = set_hue
        this.set_saturation = set_saturation
        this.set_brightness = set_brightness
        this.set_kelvin = set_kelvin
    }

    constructor(buffer: ByteBuffer) {
        reserved = buffer.get()
        transient = buffer.get()
        color = HSBK(buffer)
        period = buffer.getInt()
        cycles = buffer.getFloat()
        skew_ratio = buffer.getShort()
        waveform = WaveformType.fromValue(buffer.get())
        set_hue = buffer.get()
        set_saturation = buffer.get()
        set_brightness = buffer.get()
        set_kelvin = buffer.get()
    }

    override fun equals(other: Any?): Boolean {
        if (other is LightSetWaveformOptional) {
            return true

                    && reserved == other.reserved


                    && transient == other.transient


                    && color == other.color


                    && period == other.period


                    && cycles == other.cycles


                    && skew_ratio == other.skew_ratio


                    && waveform == other.waveform


                    && set_hue == other.set_hue


                    && set_saturation == other.set_saturation


                    && set_brightness == other.set_brightness


                    && set_kelvin == other.set_kelvin

        }
        return false
    }

    override fun toString(): String {
        return "LightSetWaveformOptional : reserved:$reserved, transient:$transient, color:$color, period:$period, cycles:$cycles, skew_ratio:$skew_ratio, waveform:$waveform, set_hue:$set_hue, set_saturation:$set_saturation, set_brightness:$set_brightness, set_kelvin:$set_kelvin, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return LightSetWaveformOptional(buffer)
        }
    }

}

class LightGetPower : LifxMessagePayload {

    override val _size = 0

    override val _type = 116


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is LightGetPower) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "LightGetPower : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return LightGetPower(buffer)
        }
    }

}

class LightSetPower : LifxMessagePayload {

    override val _size = 6

    override val _type = 117


    var level: Short
    var duration: Int

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putShort(level)
        buffer.putInt(duration)
        return buffer
    }

    constructor(level: Short, duration: Int) {
        this.level = level
        this.duration = duration
    }

    constructor(buffer: ByteBuffer) {
        level = buffer.getShort()
        duration = buffer.getInt()
    }

    override fun equals(other: Any?): Boolean {
        if (other is LightSetPower) {
            return true

                    && level == other.level


                    && duration == other.duration

        }
        return false
    }

    override fun toString(): String {
        return "LightSetPower : level:$level, duration:$duration, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return LightSetPower(buffer)
        }
    }

}

class LightStatePower : LifxMessagePayload {

    override val _size = 2

    override val _type = 118


    var level: Short

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putShort(level)
        return buffer
    }

    constructor(level: Short) {
        this.level = level
    }

    constructor(buffer: ByteBuffer) {
        level = buffer.getShort()
    }

    override fun equals(other: Any?): Boolean {
        if (other is LightStatePower) {
            return true

                    && level == other.level

        }
        return false
    }

    override fun toString(): String {
        return "LightStatePower : level:$level, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return LightStatePower(buffer)
        }
    }

}

class GetInfrared : LifxMessagePayload {

    override val _size = 0

    override val _type = 120


    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        return buffer
    }

    constructor() {
    }

    constructor(buffer: ByteBuffer) {
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetInfrared) {
            return true
        }
        return false
    }

    override fun toString(): String {
        return "GetInfrared : "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetInfrared(buffer)
        }
    }

}

class StateInfrared : LifxMessagePayload {

    override val _size = 2

    override val _type = 121


    var brightness: Short

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putShort(brightness)
        return buffer
    }

    constructor(brightness: Short) {
        this.brightness = brightness
    }

    constructor(buffer: ByteBuffer) {
        brightness = buffer.getShort()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateInfrared) {
            return true

                    && brightness == other.brightness

        }
        return false
    }

    override fun toString(): String {
        return "StateInfrared : brightness:$brightness, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateInfrared(buffer)
        }
    }

}

class SetInfrared : LifxMessagePayload {

    override val _size = 2

    override val _type = 122


    var brightness: Short

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.putShort(brightness)
        return buffer
    }

    constructor(brightness: Short) {
        this.brightness = brightness
    }

    constructor(buffer: ByteBuffer) {
        brightness = buffer.getShort()
    }

    override fun equals(other: Any?): Boolean {
        if (other is SetInfrared) {
            return true

                    && brightness == other.brightness

        }
        return false
    }

    override fun toString(): String {
        return "SetInfrared : brightness:$brightness, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return SetInfrared(buffer)
        }
    }

}

class SetColorZones : LifxMessagePayload {

    override val _size = 15

    override val _type = 501


    var start_index: Byte
    var end_index: Byte
    var color: HSBK
    var duration: Int
    var apply: ApplicationRequest

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.put(start_index)
        buffer.put(end_index)
        color.addToByteBuffer(buffer)
        buffer.putInt(duration)
        buffer.put((apply.value))
        return buffer
    }

    constructor(start_index: Byte, end_index: Byte, color: HSBK, duration: Int, apply: ApplicationRequest) {
        this.start_index = start_index
        this.end_index = end_index
        this.color = color
        this.duration = duration
        this.apply = apply
    }

    constructor(buffer: ByteBuffer) {
        start_index = buffer.get()
        end_index = buffer.get()
        color = HSBK(buffer)
        duration = buffer.getInt()
        apply = ApplicationRequest.fromValue(buffer.get())
    }

    override fun equals(other: Any?): Boolean {
        if (other is SetColorZones) {
            return true

                    && start_index == other.start_index


                    && end_index == other.end_index


                    && color == other.color


                    && duration == other.duration


                    && apply == other.apply

        }
        return false
    }

    override fun toString(): String {
        return "SetColorZones : start_index:$start_index, end_index:$end_index, color:$color, duration:$duration, apply:$apply, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return SetColorZones(buffer)
        }
    }

}

class GetColorZones : LifxMessagePayload {

    override val _size = 2

    override val _type = 502


    var start_index: Byte
    var end_index: Byte

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.put(start_index)
        buffer.put(end_index)
        return buffer
    }

    constructor(start_index: Byte, end_index: Byte) {
        this.start_index = start_index
        this.end_index = end_index
    }

    constructor(buffer: ByteBuffer) {
        start_index = buffer.get()
        end_index = buffer.get()
    }

    override fun equals(other: Any?): Boolean {
        if (other is GetColorZones) {
            return true

                    && start_index == other.start_index


                    && end_index == other.end_index

        }
        return false
    }

    override fun toString(): String {
        return "GetColorZones : start_index:$start_index, end_index:$end_index, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return GetColorZones(buffer)
        }
    }

}

class StateZone : LifxMessagePayload {

    override val _size = 10

    override val _type = 503


    var count: Byte
    var index: Byte
    var color: HSBK

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.put(count)
        buffer.put(index)
        color.addToByteBuffer(buffer)
        return buffer
    }

    constructor(count: Byte, index: Byte, color: HSBK) {
        this.count = count
        this.index = index
        this.color = color
    }

    constructor(buffer: ByteBuffer) {
        count = buffer.get()
        index = buffer.get()
        color = HSBK(buffer)
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateZone) {
            return true

                    && count == other.count


                    && index == other.index


                    && color == other.color

        }
        return false
    }

    override fun toString(): String {
        return "StateZone : count:$count, index:$index, color:$color, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateZone(buffer)
        }
    }

}

class StateMultiZone : LifxMessagePayload {

    override val _size = 66

    override val _type = 506


    var count: Byte
    var index: Byte
    var color: Array<HSBK>

    override fun addToByteBuffer(buffer: ByteBuffer): ByteBuffer {
        buffer.put(count)
        buffer.put(index)
        assert(color.size == 8); color.forEach { it.addToByteBuffer(buffer) }
        return buffer
    }

    constructor(count: Byte, index: Byte, color: Array<HSBK>) {
        this.count = count
        this.index = index
        this.color = color
    }

    constructor(buffer: ByteBuffer) {
        count = buffer.get()
        index = buffer.get()
        color = (0 until 8).map { HSBK(buffer) }.toTypedArray()
    }

    override fun equals(other: Any?): Boolean {
        if (other is StateMultiZone) {
            return true

                    && count == other.count


                    && index == other.index


                    && color == other.color

        }
        return false
    }

    override fun toString(): String {
        return "StateMultiZone : count:$count, index:$index, color:$color, "
    }


    companion object : LifxMessageDeserialzable {
        override fun parse(buffer: ByteBuffer): LifxMessagePayload {
            return StateMultiZone(buffer)
        }
    }

}

enum class Service(val value: Byte) {
    UDP(1.toByte()),

    UNKNOWN(0);

    companion object {
        private val ordinals = enumValues<Service>().associateBy { it.value }

        fun fromValue(value: Byte): Service {
            return ordinals.getOrElse(value, { Service.UNKNOWN })
        }
    }
}

enum class PowerState(val value: Short) {
    OFF(0.toShort()),
    ON(65535.toShort()),

    UNKNOWN(0);

    companion object {
        private val ordinals = enumValues<PowerState>().associateBy { it.value }

        fun fromValue(value: Short): PowerState {
            return ordinals.getOrElse(value, { PowerState.UNKNOWN })
        }
    }
}

enum class WaveformType(val value: Byte) {
    SAW(0.toByte()),
    SINE(1.toByte()),
    HALF_SINE(2.toByte()),
    TRIANGLE(3.toByte()),
    PULSE(4.toByte()),

    UNKNOWN(0);

    companion object {
        private val ordinals = enumValues<WaveformType>().associateBy { it.value }

        fun fromValue(value: Byte): WaveformType {
            return ordinals.getOrElse(value, { WaveformType.UNKNOWN })
        }
    }
}

enum class ApplicationRequest(val value: Byte) {
    NO_APPLY(0.toByte()),
    APPLY(1.toByte()),
    APPLY_ONLY(2.toByte()),

    UNKNOWN(0);

    companion object {
        private val ordinals = enumValues<ApplicationRequest>().associateBy { it.value }

        fun fromValue(value: Byte): ApplicationRequest {
            return ordinals.getOrElse(value, { ApplicationRequest.UNKNOWN })
        }
    }
}

enum class MessageType(val value: Short) {
    GetService(2),
    StateService(3),
    GetHostInfo(12),
    StateHostInfo(13),
    GetHostFirmware(14),
    StateHostFirmware(15),
    GetWifiInfo(16),
    StateWifiInfo(17),
    GetWifiFirmware(18),
    StateWifiFirmware(19),
    GetPower(20),
    SetPower(21),
    StatePower(22),
    GetLabel(23),
    SetLabel(24),
    StateLabel(25),
    GetVersion(32),
    StateVersion(33),
    GetInfo(34),
    StateInfo(35),
    Acknowledgement(45),
    GetLocation(48),
    SetLocation(49),
    StateLocation(50),
    GetGroup(51),
    SetGroup(52),
    StateGroup(53),
    EchoRequest(58),
    EchoResponse(59),
    LightGet(101),
    LightSetColor(102),
    LightSetWaveform(103),
    LightState(107),
    LightSetWaveformOptional(119),
    LightGetPower(116),
    LightSetPower(117),
    LightStatePower(118),
    GetInfrared(120),
    StateInfrared(121),
    SetInfrared(122),
    SetColorZones(501),
    GetColorZones(502),
    StateZone(503),
    StateMultiZone(506),
}

val payloadFactories = hashMapOf<Int, LifxMessageDeserialzable>(
        2 to GetService,
        3 to StateService,
        12 to GetHostInfo,
        13 to StateHostInfo,
        14 to GetHostFirmware,
        15 to StateHostFirmware,
        16 to GetWifiInfo,
        17 to StateWifiInfo,
        18 to GetWifiFirmware,
        19 to StateWifiFirmware,
        20 to GetPower,
        21 to SetPower,
        22 to StatePower,
        23 to GetLabel,
        24 to SetLabel,
        25 to StateLabel,
        32 to GetVersion,
        33 to StateVersion,
        34 to GetInfo,
        35 to StateInfo,
        45 to Acknowledgement,
        48 to GetLocation,
        49 to SetLocation,
        50 to StateLocation,
        51 to GetGroup,
        52 to SetGroup,
        53 to StateGroup,
        58 to EchoRequest,
        59 to EchoResponse,
        101 to LightGet,
        102 to LightSetColor,
        103 to LightSetWaveform,
        107 to LightState,
        119 to LightSetWaveformOptional,
        116 to LightGetPower,
        117 to LightSetPower,
        118 to LightStatePower,
        120 to GetInfrared,
        121 to StateInfrared,
        122 to SetInfrared,
        501 to SetColorZones,
        502 to GetColorZones,
        503 to StateZone,
        506 to StateMultiZone
)

