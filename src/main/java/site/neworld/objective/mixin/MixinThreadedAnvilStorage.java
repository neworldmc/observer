package site.neworld.objective.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Either;
import kotlinx.coroutines.CoroutineDispatcher;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.WorldGenerationProgressListener;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.structure.StructureManager;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.thread.ThreadExecutor;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.storage.VersionedChunkStorage;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import site.neworld.objective.IMixinChunkStorageTweakProviderConsumer;
import site.neworld.objective.IMixinThreadedAnvilStorageTweakExposed;
import site.neworld.objective.MixinChunkStorageTweakProvider;
import site.neworld.objective.MixinThreadedAnvilStorageKtAuxKt;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;


@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class MixinThreadedAnvilStorage extends VersionedChunkStorage implements
        IMixinThreadedAnvilStorageTweakExposed,
        IMixinChunkStorageTweakProviderConsumer {
    @Shadow
    private static Logger LOGGER;

    private CoroutineDispatcher mainDispatch;
    private MixinChunkStorageTweakProvider theStorageX;

    public MixinThreadedAnvilStorage(File file, DataFixer dataFixer, boolean bl) {
        super(file, dataFixer, bl);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(
            ServerWorld serverWorld, LevelStorage.Session session,
            DataFixer dataFixer, StructureManager structureManager,
            Executor workerExecutor, ThreadExecutor<Runnable> mainThreadExecutor,
            ChunkProvider chunkProvider, ChunkGenerator chunkGenerator,
            WorldGenerationProgressListener worldGenerationProgressListener,
            Supplier<PersistentStateManager> supplier, int i, boolean bl, CallbackInfo ci) {
        mainDispatch = convertToDispatch(mainThreadExecutor);
    }

    @Override
    public void consumeIMixinChunkStorageTweakProvider(@NotNull MixinChunkStorageTweakProvider stg) {
        theStorageX = stg;
    }

    @Shadow
    private ServerWorld world;
    @Shadow
    private StructureManager structureManager;
    @Shadow
    private PointOfInterestStorage pointOfInterestStorage;
    @Shadow
    private ThreadExecutor<Runnable> mainThreadExecutor;
    @Shadow
    private Supplier<PersistentStateManager> persistentStateManagerFactory;

    @Shadow
    private void method_27054(ChunkPos pos) {}

    @Shadow
    private byte method_27053(ChunkPos chunkPos, ChunkStatus.ChunkType chunkType) { return 0; }

    @Nullable
    @Override
    public CompoundTag storageUpgradeTag(CompoundTag compoundTag) {
        return compoundTag == null ? null : this.updateChunkTag(this.world.getRegistryKey(), this.persistentStateManagerFactory, compoundTag);
    }

    @Overwrite
    private CompletableFuture<Either<Chunk, ChunkHolder.Unloaded>> loadChunk(ChunkPos pos) {
        return MixinThreadedAnvilStorageKtAuxKt.loadChunkAsyncSupply(mainDispatch, theStorageX, this, pos);
    }

    @NotNull
    @Override
    public Either<Chunk, ChunkHolder.Unloaded> loadChunkFallback(@NotNull ChunkPos pos) {
        this.method_27054(pos);
        return Either.left(new ProtoChunk(pos, UpgradeData.NO_UPGRADE_DATA));
    }

    @Nullable
    @Override
    public Either<Chunk, ChunkHolder.Unloaded> loadChunkTryDeserializeChunk(@NotNull ChunkPos pos, CompoundTag compoundTag) {
        this.world.getProfiler().visit("chunkLoad");
        if (compoundTag != null) {
            boolean bl = compoundTag.contains("Level", 10) && compoundTag.getCompound("Level").contains("Status", 8);
            if (bl) {
                Chunk chunk = ChunkSerializer.deserialize(this.world, this.structureManager, this.pointOfInterestStorage, pos, compoundTag);
                chunk.setLastSaveTime(this.world.getTime());
                this.method_27053(pos, chunk.getStatus().getChunkType());
                return Either.left(chunk);
            }
            LOGGER.error("Chunk file at {} is missing level data, skipping", pos);
        }
        return null;
    }

    @Override
    public void loadChunkAnalyzeExceptions(@NotNull ChunkPos pos, @NotNull Exception throwable) throws Exception {
        if (throwable instanceof CrashException) {
            Throwable inner = throwable.getCause();
            if (!(inner instanceof IOException)) {
                this.method_27054(pos);
                throw throwable;
            }
        }
        LOGGER.error("Couldn't load chunk {}", pos, throwable);
    }
}
