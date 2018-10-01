package ladylib.capability;

import com.google.common.annotations.Beta;
import ladylib.LadyLib;
import ladylib.nbt.serialization.NBTDeserializationException;
import ladylib.nbt.serialization.NBTTypeAdapter;
import ladylib.nbt.serialization.adapter.ReflectiveNBTAdapterFactory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;

/**
 * A capability storage that can serialize any capability implementation
 * using the generic NBT serialization system.
 * @param <C> the capability type
 */
@Beta
public class ReflectiveCapabilityStorage<C> implements Capability.IStorage<C> {
    private final Class<C> capClass;
    private final NBTTypeAdapter<C, NBTTagCompound> adapter;

    public ReflectiveCapabilityStorage(Class<C> clazz) {
        if (Modifier.isAbstract(clazz.getModifiers())) {
            throw new IllegalArgumentException("Class parameter must be an implementation");
        }
        capClass = clazz;
        if (clazz.getDeclaredFields().length > 0) {
            adapter = ReflectiveNBTAdapterFactory.INSTANCE.create(clazz);
        } else {
            adapter = null;
        }
    }

    @Nullable
    @Override
    public NBTBase writeNBT(Capability capability, Object instance, EnumFacing side) {
        // return null if nothing needs saving
        if (adapter == null || !capClass.isInstance(instance)) {
            return null;
        }
        return adapter.toNBT(capClass.cast(instance));
    }

    @Override
    public void readNBT(Capability capability, Object instance, EnumFacing side, NBTBase nbt) {
        if (adapter != null && capClass.isInstance(instance)) {
            try {
                adapter.fromNBT(capClass.cast(instance), nbt);
            } catch (NBTDeserializationException e) {
                LadyLib.LOGGER.error("Could not read NBT from capability " + capability.getName(), e);
            }
        }
    }

}
