package site.neworld.observer.mixin;

import net.minecraft.util.collection.TypeFilterableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mixin(TypeFilterableList.class)
public class MixinTypeFilterableList<T> {
    private site.neworld.observer.TypeFilterableList<T> impl;
    @Shadow
    private Map<Class<?>, List<T>> elementsByType;
    @Shadow
    private Class<T> elementType;
    @Shadow
    private List<T> allElements;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(Class<T> elementType, CallbackInfo ci) {
        impl = new site.neworld.observer.TypeFilterableList<>(elementType);
        this.elementsByType.clear();
        this.allElements.clear();
    }

    @Overwrite
    public boolean add(T e) {
        return impl.add(e);
    }

    @Overwrite
    public boolean remove(Object o) {
        return impl.remove(o);
    }

    @Overwrite
    public boolean contains(Object o) {
        return impl.contains(o);
    }

    @Overwrite
    public <S> Collection<S> getAllOfType(Class<S> type) {
        return impl.getAllOfType(type);
    }

    @Overwrite
    public Iterator<T> iterator() {
        return impl.iterator();
    }

    @Overwrite
    public List<T> method_29903() {
        return impl.all();
    }
}
