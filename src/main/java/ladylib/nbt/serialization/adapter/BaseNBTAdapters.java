package ladylib.nbt.serialization.adapter;

import ladylib.nbt.serialization.DefaultValue;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

/**
 * Default adapters for standard NBT objects
 */
public interface BaseNBTAdapters {

    @DefaultValue({byte.class, Byte.class})
    byte DEFAULT_BYTE = 0;

    @DefaultValue({short.class, Short.class})
    short DEFAULT_SHORT = 0;

    @DefaultValue({int.class, Integer.class})
    int DEFAULT_INT = 0;

    @DefaultValue({float.class, Float.class})
    float DEFAULT_FLOAT = 0F;

    @DefaultValue({long.class, Long.class})
    long DEFAULT_LONG = 0L;

    @DefaultValue({double.class, Double.class})
    double DEFAULT_DOUBLE = 0D;

    @DefaultValue({boolean.class, Boolean.class})
    boolean DEFAULT_BOOLEAN = false;

    @DefaultValue(String.class)
    String DEFAULT = "";

    class IntAdapter extends AbstractNBTTypeAdapter<Integer, NBTTagInt> {

        @Override
        public NBTTagInt toNBT(Integer value) {
            return new NBTTagInt(value);
        }

        @Override
        public Integer fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagInt.class, NBTTagInt::getInt);
        }
    }

    class DoubleAdapter extends AbstractNBTTypeAdapter<Double, NBTTagDouble> {
        @Override
        public NBTTagDouble toNBT(Double value) {
            return new NBTTagDouble(value);
        }

        @Override
        public Double fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagDouble.class, NBTTagDouble::getDouble);
        }
    }

    class FloatAdapter extends AbstractNBTTypeAdapter<Float, NBTTagFloat> {

        @Override
        public NBTTagFloat toNBT(Float value) {
            return new NBTTagFloat(value);
        }

        @Override
        public Float fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagFloat.class, NBTTagFloat::getFloat);
        }
    }

    class LongAdapter extends AbstractNBTTypeAdapter<Long, NBTTagLong> {
        @Override
        public NBTTagLong toNBT(Long value) {
            return new NBTTagLong(value);
        }

        @Override
        public Long fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagLong.class, NBTTagLong::getLong);
        }
    }

    class ShortAdapter extends AbstractNBTTypeAdapter<Short, NBTTagShort> {
        @Override
        public NBTTagShort toNBT(Short value) {
            return new NBTTagShort(value);
        }

        @Override
        public Short fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagShort.class, NBTTagShort::getShort);
        }
    }

    class ByteAdapter extends AbstractNBTTypeAdapter<Byte, NBTTagByte> {
        @Override
        public NBTTagByte toNBT(Byte value) {
            return new NBTTagByte(value);
        }

        @Override
        public Byte fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagByte.class, NBTTagByte::getByte);
        }
    }

    class BooleanAdapter extends AbstractNBTTypeAdapter<Boolean, NBTTagByte> {

        @Override
        public NBTTagByte toNBT(Boolean value) {
            return new NBTTagByte((byte) (value ? 1 : 0));
        }

        @Override
        public Boolean fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagByte.class, tag -> tag.getByte() == 1);
        }
    }

    class StringAdapter extends AbstractNBTTypeAdapter<String, NBTTagString> {

        @Override
        public NBTTagString toNBT(String value) {
            return new NBTTagString(value);
        }

        @Override
        public String fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagString.class, NBTTagString::getString);
        }
    }

    class ItemStackAdapter extends AbstractNBTTypeAdapter<ItemStack, NBTTagCompound> {

        @DefaultValue(ItemStack.class)
        public static final ItemStack DEFAULT = ItemStack.EMPTY;

        @Override
        public NBTTagCompound toNBT(ItemStack value) {
            return value.writeToNBT(new NBTTagCompound());
        }

        @Override
        public ItemStack fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagCompound.class, ItemStack::new);
        }
    }

    class BlockPosAdapter extends AbstractNBTTypeAdapter<BlockPos, NBTTagLong> {

        @DefaultValue(BlockPos.class)
        public static final BlockPos DEFAULT = BlockPos.ORIGIN;

        @Override
        public NBTTagLong toNBT(BlockPos value) {
            return new NBTTagLong(value.toLong());
        }

        @Override
        public BlockPos fromNBT(NBTBase nbt) {
            return castAnd(nbt, NBTTagLong.class, tag -> BlockPos.fromLong(tag.getLong()));
        }
    }

    class UUIDAdapter extends AbstractNBTTypeAdapter<UUID, NBTTagString> {

        @DefaultValue(UUID.class)
        public static final UUID DEFAULT = new UUID(0, 0);

        @Override
        public NBTTagString toNBT(UUID value) {
            return new NBTTagString(value.toString());
        }

        @Override
        public UUID fromNBT(NBTBase nbt) {
            // avoid IllegalArgumentException when the NBT is invalid
            return castAnd(nbt, NBTTagString.class, tag -> {
                String serialized = tag.getString();
                return serialized.isEmpty() ? new UUID(0, 0) : UUID.fromString(serialized);
            });
        }
    }
}
