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

import kotlin.reflect.KProperty

interface ILightChangeDispatcher {
    fun onLightChange(light: Light, property: LightProperty, oldValue: Any?, newValue: Any?)
    fun onLightAdded(light: Light)
}

class LightChangeNotifier<T>(private val lightProperty: LightProperty, default: T, var changeDispatcher: ILightChangeDispatcher) {

    private var value: T = default

    operator fun getValue(thisRef: Light, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Light, property: KProperty<*>, value: T) {
        if (this.value != value) {
            val oldValue = this.value
            this.value = value
            changeDispatcher.onLightChange(thisRef, lightProperty, oldValue, value)
        }
    }
}