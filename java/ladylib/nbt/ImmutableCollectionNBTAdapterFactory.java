package ladylib.nbt;

import com.google.common.collect.ImmutableCollection;
import com.google.gson.reflect.TypeToken;
import ladylib.LadyLib;
import ladylib.capability.internal.CapabilityRegistrar;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;

import java.util.function.Supplier;

import static ladylib.nbt.CollectionNBTTypeAdapterFactory.getElementTypeAdapter;

public class ImmutableCollectionNBTAdapterFactory implements NBTTypeAdapterFactory<ImmutableCollection, NBTTagList> {
    @Override
    public NBTTypeAdapter<ImmutableCollection, NBTTagList> create(TypeToken type, boolean allowMutating) {
        Class<?> rawType = type.getRawType();
        if (!ImmutableCollection.class.isAssignableFrom(rawType)) {
            return null;
        }
        NBTTypeAdapter elementAdapter = getElementTypeAdapter(type, 0);
        try {
            Class<?> builder = Class.forName(rawType.getName() + "$Builder");
            Supplier builderFactory = CapabilityRegistrar.createFactory(builder, "get", Supplier.class);
            @SuppressWarnings("unchecked") NBTTypeAdapter<ImmutableCollection, NBTTagList> ret =
                    new ImmutableCollectionNBTAdapter(elementAdapter, builderFactory);
            return ret;
        } catch (ClassNotFoundException | CapabilityRegistrar.UnableToGetFactoryException e) {
            LadyLib.LOGGER.error("Unable to create builder factory", e);
        }
        return null;
    }

    public static class ImmutableCollectionNBTAdapter<E> extends CollectionNBTTypeAdapterFactory.CollectionBaseAdapter<E> {
        protected final Supplier<ImmutableCollection.Builder<E>> builderSupplier;

        public ImmutableCollectionNBTAdapter(NBTTypeAdapter<E, NBTBase> elementAdapter, Supplier<ImmutableCollection.Builder<E>> supplier) {
            super(elementAdapter);
            builderSupplier = supplier;
        }

        @Override
        public ImmutableCollection<E> fromNBT(NBTBase list) {
            ImmutableCollection.Builder<E> ret = builderSupplier.get();
            for (NBTBase nbtBase : cast(list, NBTTagList.class)) {
                ret.add(elementAdapter.fromNBT(nbtBase));
            }
            return ret.build();
        }
    }

}
