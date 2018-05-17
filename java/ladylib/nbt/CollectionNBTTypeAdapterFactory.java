package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagList;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Collection;

public class CollectionNBTTypeAdapterFactory implements NBTTypeAdapterFactory<Collection, NBTTagList> {
    @Override
    public NBTTypeAdapter<Collection, NBTTagList> create(TypeToken type) {
        Type collectionType = type.getType();
        Type elementType = Object.class;
        if (collectionType instanceof WildcardType) {
            collectionType = ((WildcardType) collectionType).getUpperBounds()[0];
        }
        if (collectionType instanceof ParameterizedType) {
            elementType = ((ParameterizedType) collectionType).getActualTypeArguments()[0];
        }

        NBTTypeAdapter elementAdapter = TagAdapters.getNBTAdapter(TypeToken.get(elementType));
        // cannot support mutating type adapters for the collection's element
        if (elementAdapter instanceof NBTMutatingTypeAdapter) {
            return null;
        }
        @SuppressWarnings("unchecked") NBTTypeAdapter<Collection, NBTTagList> ret =
                new CollectionNBTTypeAdapter<>(elementAdapter);
        return ret;
    }

    public static class CollectionNBTTypeAdapter<E> implements NBTMutatingTypeAdapter<Collection<E>, NBTTagList> {
        private final NBTTypeAdapter<E, NBTBase> elementAdapter;

        public CollectionNBTTypeAdapter(NBTTypeAdapter<E, NBTBase> elementAdapter) {
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

        @Override
        public Collection<E> fromNBT(Collection<E> value, NBTTagList list) {
            for (NBTBase nbtBase : list) {
                value.add(elementAdapter.fromNBT(nbtBase));
            }
            return value;
        }
    }
}
