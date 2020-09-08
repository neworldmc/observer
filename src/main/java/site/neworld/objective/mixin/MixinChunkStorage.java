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
import site.neworld.objective.IMixinChunkStorageTweakProviderConsumer;
import site.neworld.objective.MixinChunkStorageTweakProvider;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

@Mixin(VersionedChunkStorage.class)
public class MixinChunkStorage {
    @Shadow
    @Final
    private StorageIoWorker worker;
    @Shadow
    private FeatureUpdater featureUpdater;

    public final MixinChunkStorageTweakProvider theStorage = new MixinChunkStorageTweakProvider();

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(File file, DataFixer dataFixer, boolean bl, CallbackInfo ci) throws IOException {
        worker.close(); // not null, but closed setting to null will lead to crashes
        theStorage.setup(file);
        if (this instanceof IMixinChunkStorageTweakProviderConsumer) {
            ((IMixinChunkStorageTweakProviderConsumer)this).consumeIMixinChunkStorageTweakProvider(theStorage);
        }
    }

    @Overwrite
    public CompoundTag getNbt(ChunkPos pos) {
        return theStorage.getNbtNow(new site.neworld.objective.utils.ChunkPos(pos.x, pos.z));
    }

    @Overwrite
    public void setTagAt(ChunkPos pos, CompoundTag compoundTag) {
        theStorage.setNbtNow(new site.neworld.objective.utils.ChunkPos(pos.x, pos.z), compoundTag);
        if (this.featureUpdater != null) {
            this.featureUpdater.markResolved(pos.toLong());
        }
    }

    @Overwrite
    public void completeAll() {
    }

    @Overwrite
    public void close() {
        theStorage.destroy();
    }
}
