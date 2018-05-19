package ladylib.nbt;

import com.google.gson.reflect.TypeToken;
import ladylib.capability.internal.CapabilityRegistrar;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import static ladylib.nbt.CollectionNBTTypeAdapterFactory.getElementTypeAdapter;

public class MapNBTTypeAdapterFactory implements NBTTypeAdapterFactory<Map, NBTTagList> {
    @Override
    public NBTTypeAdapter<Map, NBTTagList> create(TypeToken type, boolean allowMutating) {
        Class<?> rawType = type.getRawType();
        if (!Map.class.isAssignableFrom(rawType)) {
            return null;
        }
        NBTTypeAdapter keyAdapter = getElementTypeAdapter(type, 0);
        NBTTypeAdapter valueAdapter = getElementTypeAdapter(type, 1);

        if (allowMutating) {
            @SuppressWarnings("unchecked") NBTTypeAdapter<Map, NBTTagList> ret =
                    new MapNBTTypeAdapterFactory.MapNBTMutatingTypeAdapter<>(keyAdapter, valueAdapter);
            return ret;
        }
        Supplier<Map> supplier;
        if (!Modifier.isAbstract(rawType.getModifiers())) {
            supplier = CapabilityRegistrar.createFactory(rawType, "get", Supplier.class);
        } else if (SortedMap.class.isAssignableFrom(rawType)) {
            supplier = TreeMap::new;
        } else {
            supplier = HashMap::new;
        }
        @SuppressWarnings("unchecked") NBTTypeAdapter<Map, NBTTagList> ret =
                new MapNBTTypeAdapter(keyAdapter, valueAdapter, supplier);
        return ret;
    }

    public abstract static class MapBaseAdapter<K, V> implements NBTTypeAdapter<Map<K, V>, NBTTagList> {
        protected static final String KEY_TAG = "key";
        protected static final String VALUE_TAG = "value";
        protected final NBTTypeAdapter<K, NBTBase> keyAdapter;
        protected final NBTTypeAdapter<V, NBTBase> valueAdapter;

        public MapBaseAdapter(NBTTypeAdapter<K, NBTBase> keyAdapter, NBTTypeAdapter<V, NBTBase> valueAdapter) {
            this.keyAdapter = keyAdapter;
            this.valueAdapter = valueAdapter;
        }

        @Override
        public NBTTagList toNBT(Map<K, V> value) {
            NBTTagList ret = new NBTTagList();
            value.forEach((k, v) -> {
                NBTTagCompound pair = new NBTTagCompound();
                pair.setTag(KEY_TAG, keyAdapter.toNBT(k));
                pair.setTag(VALUE_TAG, valueAdapter.toNBT(v));
                ret.appendTag(pair);
            });
            return ret;
        }
    }
    public static class MapNBTMutatingTypeAdapter<K,V> extends MapNBTTypeAdapterFactory.MapBaseAdapter<K,V> implements NBTMutatingTypeAdapter<Map<K,V>, NBTTagList> {

        public MapNBTMutatingTypeAdapter(NBTTypeAdapter<K, NBTBase> keyAdapter, NBTTypeAdapter<V, NBTBase> valueAdapter) {
            super(keyAdapter, valueAdapter);
        }

        @Override
        public Map<K,V> fromNBT(Map<K,V> value, NBTBase list) {
            value.clear();
            for (NBTBase nbtBase : cast(list, NBTTagList.class)) {
                NBTTagCompound entry = NBTTypeAdapter.castNBT(nbtBase, NBTTagCompound.class);
                K k = keyAdapter.fromNBT(entry.getTag(KEY_TAG));
                V v = valueAdapter.fromNBT(entry.getTag(VALUE_TAG));
                value.put(k, v);
            }
            return value;
        }
    }

    public static class MapNBTTypeAdapter<K,V> extends MapNBTTypeAdapterFactory.MapBaseAdapter<K,V> implements NBTTypeAdapter<Map<K,V>, NBTTagList> {
        private final Supplier<Map<K,V>> mapSupplier;

        public MapNBTTypeAdapter(NBTTypeAdapter<K, NBTBase> keyAdapter, NBTTypeAdapter<V, NBTBase> valueAdapter, Supplier<Map<K,V>> MapSupplier) {
            super(keyAdapter, valueAdapter);
            this.mapSupplier = MapSupplier;
        }

        @Override
        public Map<K,V> fromNBT(NBTBase list) {
            Map<K,V> ret = mapSupplier.get();
            for (NBTBase nbtBase : cast(list, NBTTagList.class)) {
                NBTTagCompound entry = NBTTypeAdapter.castNBT(nbtBase, NBTTagCompound.class);
                K k = keyAdapter.fromNBT(entry.getTag(KEY_TAG));
                V v = valueAdapter.fromNBT(entry.getTag(VALUE_TAG));
                ret.put(k, v);
            }
            return ret;
        }
    }
}
