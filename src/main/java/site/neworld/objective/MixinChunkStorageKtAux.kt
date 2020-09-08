package site.neworld.objective

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import site.neworld.objective.data.LevelStorage
import site.neworld.objective.utils.ChunkPos
import java.io.*

class MixinChunkStorageTweakProvider {
    private lateinit var theStorage: LevelStorage

    fun setup(file: File) {
        theStorage = Controller.open(file)
    }

    fun destroy() {
        Controller.close(theStorage)
    }

    fun getNbtNow(pos: ChunkPos): CompoundTag? {
        return runBlocking { getNbtAsync(pos) }
    }

    fun setNbtNow(pos: ChunkPos, compoundTag: CompoundTag) {
        GlobalScope.launch { setNbtAsync(pos, compoundTag) }
    }

    suspend fun getNbtAsync(pos: ChunkPos): CompoundTag? {
        val bytes = theStorage.get(pos)
        return if (bytes != null) NbtIo.read(DataInputStream(ByteArrayInputStream(bytes))) else null
    }

    suspend fun setNbtAsync(pos: ChunkPos, compoundTag: CompoundTag) {
        val outputStream = ByteArrayOutputStream()
        DataOutputStream(outputStream).use { stream -> NbtIo.write(compoundTag, stream) }
        theStorage.set(pos, outputStream.toByteArray())
    }
}

interface IMixinChunkStorageTweakProviderConsumer {
    fun consumeIMixinChunkStorageTweakProvider(stg: MixinChunkStorageTweakProvider)
}
