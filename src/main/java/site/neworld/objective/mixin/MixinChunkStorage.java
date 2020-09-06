package site.neworld.objective.mixin;

import com.mojang.datafixers.DataFixer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.FeatureUpdater;
import net.minecraft.world.storage.StorageIoWorker;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.neworld.objective.ChunkStorageSync;

import java.io.*;

@Mixin(VersionedChunkStorage.class)
public class MixinChunkStorage {
    @Shadow
    @Final
    private StorageIoWorker worker;
    @Shadow
    private FeatureUpdater featureUpdater;

    private ChunkStorageSync storage;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(File file, DataFixer dataFixer, boolean bl, CallbackInfo ci) throws IOException {
        worker.close(); // not null, but closed setting to null will lead to crashes
        storage = new ChunkStorageSync(file);
    }

    @Overwrite
    public CompoundTag getNbt(ChunkPos pos) throws IOException {
        var bytes = this.storage.getBytes(new site.neworld.objective.utils.ChunkPos(pos.x, pos.z));
        return (bytes != null) ? NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes))) : null;
    }

    @Overwrite
    public void setTagAt(ChunkPos pos, CompoundTag compoundTag) throws IOException {
        var outputStream = new ByteArrayOutputStream();
        try (var stream = new DataOutputStream(outputStream)) {
            NbtIo.write(compoundTag, stream);
        }
        this.storage.setBytes(new site.neworld.objective.utils.ChunkPos(pos.x, pos.z), outputStream.toByteArray());
        if (this.featureUpdater != null) {
            this.featureUpdater.markResolved(pos.toLong());
        }
    }

    @Overwrite
    public void completeAll() {
    }

    @Overwrite
    public void close() {
        this.storage.close();
    }
}
