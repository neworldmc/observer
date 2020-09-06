package site.neworld.objective

import kotlinx.coroutines.runBlocking
import site.neworld.objective.utils.ChunkPos
import java.io.File

class ChunkStorageSync(file: File) {
    private var storage = Controller.open(file)

    fun getBytes(pos: ChunkPos) = runBlocking { storage.get(pos) }

    fun setBytes(pos: ChunkPos, array: ByteArray) = runBlocking { storage.set(pos, array) }

    fun close() = Controller.close(storage)
}