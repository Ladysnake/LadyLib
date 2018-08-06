package ladylib.nbt.serialization.adapter;

import com.google.common.collect.ImmutableCollection;
import com.google.gson.reflect.TypeToken;
import ladylib.LadyLib;
import ladylib.misc.ReflectionUtil;
import ladylib.nbt.serialization.NBTDeserializationException;
import ladylib.nbt.serialization.NBTTypeAdapter;
import ladylib.nbt.serialization.NBTTypeAdapterFactory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;

import java.util.Collection;
import java.util.function.Supplier;

import static ladylib.nbt.serialization.adapter.CollectionNBTTypeAdapterFactory.getElementTypeAdapter;

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
            Supplier builderFactory = ReflectionUtil.createFactory(builder, "get", Supplier.class);
            @SuppressWarnings("unchecked") NBTTypeAdapter<ImmutableCollection, NBTTagList> ret =
                    new ImmutableCollectionNBTAdapter(elementAdapter, builderFactory);
            return ret;
        } catch (ClassNotFoundException | ReflectionUtil.UnableToGetFactoryException e) {
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
        public Collection<E> fromNBT(Collection<E> value, NBTBase nbt) {
            // impossible to modify the existing one as it is immutable
            return fromNBT(nbt);
        }

        @Override
        public ImmutableCollection<E> fromNBT(NBTBase nbt) {
            ImmutableCollection.Builder<E> ret = builderSupplier.get();
            cast(nbt, NBTTagList.class).ifPresent(list -> {
                for (NBTBase nbtBase : list) {
                    E element = elementAdapter.fromNBT(nbtBase);
                    if (element != null) {
                        ret.add(element);
                    } else throw new NBTDeserializationException("An element of an immutable list was null");
                }
            });
            return ret.build();
        }
    }

}
