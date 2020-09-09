package site.neworld.observer

import com.mojang.datafixers.util.Either
import kotlinx.coroutines.*
import kotlinx.coroutines.future.future
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.world.ChunkHolder.Unloaded
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.thread.ThreadExecutor
import net.minecraft.world.chunk.Chunk
import java.lang.Runnable
import java.util.concurrent.CompletableFuture

interface IMixinThreadedAnvilStorageTweakExposed {
    fun storageUpgradeTag(compoundTag: CompoundTag?): CompoundTag?
    fun loadChunkTryDeserializeChunk(pos: ChunkPos, compoundTag: CompoundTag?): Either<Chunk, Unloaded>?
    fun loadChunkFallback(pos: ChunkPos): Either<Chunk, Unloaded>
    @Throws(Exception::class)
    fun loadChunkAnalyzeExceptions(pos: ChunkPos, throwable: Exception)
    @JvmDefault
    fun convertToDispatch(exec: ThreadExecutor<Runnable>) = exec.asCoroutineDispatcher()
}

private suspend fun getUpdatedChunkTag(
        stg: MixinChunkStorageTweakProvider,
        exposed: IMixinThreadedAnvilStorageTweakExposed,
        pos: ChunkPos): CompoundTag? {
    val nwPos = site.neworld.objective.utils.ChunkPos(pos.x, pos.z)
    return exposed.storageUpgradeTag(stg.getNbtAsync(nwPos))
}

@Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
fun loadChunkAsyncSupply(
        mainDispatch: CoroutineDispatcher,
        stg: MixinChunkStorageTweakProvider,
        exposed: IMixinThreadedAnvilStorageTweakExposed,
        pos: ChunkPos): CompletableFuture<Either<Chunk, Unloaded>> {
    return GlobalScope.future(mainDispatch) async@{
        try {
            val compoundTag = withContext(Dispatchers.Default) { getUpdatedChunkTag(stg, exposed, pos) }
            val chunk = exposed.loadChunkTryDeserializeChunk(pos, compoundTag)
            if (chunk != null) return@async chunk!!
        } catch (var5: java.lang.Exception) {
            exposed.loadChunkAnalyzeExceptions(pos, var5)
        }
        exposed.loadChunkFallback(pos)
    }
}
