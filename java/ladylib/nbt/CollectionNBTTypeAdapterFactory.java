package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import ladylib.LadyLib;
import ladylib.capability.internal.CapabilityRegistrar;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;
import java.util.function.Supplier;

public class CollectionNBTTypeAdapterFactory implements NBTTypeAdapterFactory<Collection, NBTTagList> {
    @Override
    public NBTTypeAdapter<Collection, NBTTagList> create(TypeToken type, boolean allowMutating) {
        Class<?> rawType = type.getRawType();
        if (!Collection.class.isAssignableFrom(rawType)) {
            return null;
        }
        NBTTypeAdapter elementAdapter = getElementTypeAdapter(type, 0);

        if (allowMutating) {
            @SuppressWarnings("unchecked") NBTTypeAdapter<Collection, NBTTagList> ret =
                    new CollectionNBTMutatingTypeAdapter<>(elementAdapter);
            return ret;
        }
        Supplier<Collection> supplier;
        if (!Modifier.isAbstract(rawType.getModifiers())) {
            supplier = CapabilityRegistrar.createFactory(rawType, "get", Supplier.class);
        } else if (List.class.isAssignableFrom(rawType)) {
            supplier = ArrayList::new;
        } else if (SortedSet.class.isAssignableFrom(rawType)) {
            supplier = TreeSet::new;
        } else if (Set.class.isAssignableFrom(rawType)) {
            supplier = HashSet::new;
        } else if (Queue.class.isAssignableFrom(rawType)) {
            supplier = ArrayDeque::new;
        } else {
            LadyLib.LOGGER.warn("Unsupported collection type: {}", rawType);
            return null;
        }
        @SuppressWarnings("unchecked") NBTTypeAdapter<Collection, NBTTagList> ret =
                new CollectionNBTTypeAdapter(elementAdapter, supplier);
        return ret;
    }

    static NBTTypeAdapter getElementTypeAdapter(TypeToken type, int index) {
        Type collectionType = type.getType();
        Type elementType = Object.class;
        if (collectionType instanceof WildcardType) {
            collectionType = ((WildcardType) collectionType).getUpperBounds()[index];
        }
        if (collectionType instanceof ParameterizedType) {
            elementType = ((ParameterizedType) collectionType).getActualTypeArguments()[index];
        }

        // cannot support mutating type adapters for the collection's elements
        return TagAdapters.getNBTAdapter(TypeToken.get(elementType), false);
    }

    public abstract static class CollectionBaseAdapter<E> implements NBTTypeAdapter<Collection<E>, NBTTagList> {
        protected final NBTTypeAdapter<E, NBTBase> elementAdapter;

        protected CollectionBaseAdapter(NBTTypeAdapter<E, NBTBase> elementAdapter) {
            this.elementAdapter = elementAdapter;
        }

        @Override
        public NBTTagList toNBT(Collection<E> value) {
            NBTTagList list = new NBTTagList();
            for (E element : value) {
                list.appendTag(elementAdapter.toNBT(element));
            }
            return list;
        }
    }

    public static class CollectionNBTMutatingTypeAdapter<E> extends CollectionBaseAdapter<E> implements NBTMutatingTypeAdapter<Collection<E>, NBTTagList> {

        public CollectionNBTMutatingTypeAdapter(NBTTypeAdapter<E, NBTBase> elementAdapter) {
            super(elementAdapter);
        }

        @Override
        public Collection<E> fromNBT(Collection<E> value, NBTBase list) {
            value.clear();
            for (NBTBase nbtBase : cast(list, NBTTagList.class)) {
                value.add(elementAdapter.fromNBT(nbtBase));
            }
            return value;
        }
    }

    public static class CollectionNBTTypeAdapter<E> extends CollectionBaseAdapter<E> implements NBTTypeAdapter<Collection<E>, NBTTagList> {
        private final Supplier<Collection<E>> collectionSupplier;

        public CollectionNBTTypeAdapter(NBTTypeAdapter<E, NBTBase> elementAdapter, Supplier<Collection<E>> collectionSupplier) {
            super(elementAdapter);
            this.collectionSupplier = collectionSupplier;
        }

        @Override
        public Collection<E> fromNBT(NBTBase list) {
            Collection<E> ret = collectionSupplier.get();
            for (NBTBase nbtBase : cast(list, NBTTagList.class)) {
                ret.add(elementAdapter.fromNBT(nbtBase));
            }
            return ret;
        }
    }
}
