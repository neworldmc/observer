package site.neworld.objective.mixin

import com.mojang.datafixers.DataFixer
import kotlinx.coroutines.runBlocking
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.util.math.ChunkPos
import net.minecraft.world.FeatureUpdater
import net.minecraft.world.storage.StorageIoWorker
import net.minecraft.world.storage.VersionedChunkStorage
import org.spongepowered.asm.mixin.*
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo
import site.neworld.objective.Controller
import site.neworld.objective.data.LevelStorage
import java.io.*

@Mixin(VersionedChunkStorage::class)
open class MixinChunkStorage {
    @Shadow
    @Final
    private var worker: StorageIoWorker? = null

    @Shadow
    private val featureUpdater: FeatureUpdater? = null
    private var storage: LevelStorage? = null

    @Suppress("UNUSED_PARAMETER")
    @Inject(method = ["<init>"], at = [At("RETURN")])
    fun init(file: File, dataFixer: DataFixer?, bl: Boolean, ci: CallbackInfo) {
        // TODO(this is absolutely not a good way to do things, fix it)
        worker?.close()
        worker = null
        storage = Controller.open(file)
    }

    @Overwrite
    @Throws(IOException::class)
    fun getNbt(pos: ChunkPos) = runBlocking {
        val bytes = storage!!.get(site.neworld.objective.utils.ChunkPos(pos.x, pos.z))
        if (bytes != null) NbtIo.read(DataInputStream(ByteArrayInputStream(bytes))) else null
    }

    @Overwrite
    fun setTagAt(pos: ChunkPos, compoundTag: CompoundTag) {
        runBlocking {
            val outputStream = ByteArrayOutputStream()
            DataOutputStream(outputStream).use { NbtIo.write(compoundTag, it) }
            storage!!.set(site.neworld.objective.utils.ChunkPos(pos.x, pos.z), outputStream.toByteArray())
        }
        featureUpdater?.markResolved(pos.toLong())
    }

    @Overwrite
    fun completeAll() {} // TODO(currently there is no way to do this with objective engine)

    @Overwrite
    @Throws(IOException::class)
    open fun close() = storage!!.close()
}