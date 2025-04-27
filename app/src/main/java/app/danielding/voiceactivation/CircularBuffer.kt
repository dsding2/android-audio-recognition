package app.danielding.voiceactivation

class CircularBuffer(private val capacity: Int) {

    private val buffer = Array<Pair<Double, FloatArray>>(capacity) { 0.0 to FloatArray(13) }
    private var head = 0
    private var size = 0

    fun add(item: Pair<Double, FloatArray>) {
        buffer[head] = item
        head = (head + 1) % capacity
        if (size < capacity) size++
    }

    fun toList(): List<Pair<Double, FloatArray>> {
        val result = mutableListOf<Pair<Double, FloatArray>>()
        for (i in 0 until size) {
            val index = (head - size + i + capacity) % capacity
            buffer[index].let { result.add(it) }
        }
        return result
    }

    fun isFull(): Boolean = size == capacity

    fun clear() {
        head = 0
        size = 0
//        for (i in buffer.indices) buffer[i] = FloatArray(13)
    }
}