package ladylib.nbt;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public interface BaseNBTAdapters {

    class NBTAdapter implements NBTTypeAdapter<NBTBase, NBTBase> {

        @Override
        public NBTBase toNBT(NBTBase value) {
            return value;
        }

        @Override
        public NBTBase fromNBT(NBTBase nbt) {
            return nbt;
        }
    }

    class IntAdapter implements NBTTypeAdapter<Integer, NBTTagInt> {

        @Override
        public NBTTagInt toNBT(Integer value) {
            return new NBTTagInt(value);
        }

        @Override
        public Integer fromNBT(NBTBase nbt) {
            return cast(nbt, NBTTagInt.class).getInt();
        }
    }

    class DoubleAdapter implements NBTTypeAdapter<Double, NBTTagDouble> {

        @Override
        public NBTTagDouble toNBT(Double value) {
            return new NBTTagDouble(value);
        }

        @Override
        public Double fromNBT(NBTBase nbt) {
            return cast(nbt, NBTTagDouble.class).getDouble();
        }
    }
    class FloatAdapter implements NBTTypeAdapter<Float, NBTTagFloat> {

        @Override
        public NBTTagFloat toNBT(Float value) {
            return new NBTTagFloat(value);
        }

        @Override
        public Float fromNBT(NBTBase nbt) {
            return cast(nbt, NBTTagFloat.class).getFloat();
        }
    }
    class LongAdapter implements NBTTypeAdapter<Long, NBTTagLong> {

        @Override
        public NBTTagLong toNBT(Long value) {
            return new NBTTagLong(value);
        }

        @Override
        public Long fromNBT(NBTBase nbt) {
            return cast(nbt, NBTTagLong.class).getLong();
        }
    }
    class ShortAdapter implements NBTTypeAdapter<Short, NBTTagShort> {

        @Override
        public NBTTagShort toNBT(Short value) {
            return new NBTTagShort(value);
        }

        @Override
        public Short fromNBT(NBTBase nbt) {
            return cast(nbt, NBTTagShort.class).getShort();
        }
    }
    class ByteAdapter implements NBTTypeAdapter<Byte, NBTTagByte> {

        @Override
        public NBTTagByte toNBT(Byte value) {
            return new NBTTagByte(value);
        }

        @Override
        public Byte fromNBT(NBTBase nbt) {
            return cast(nbt, NBTTagByte.class).getByte();
        }
    }
    class BooleanAdapter implements NBTTypeAdapter<Boolean, NBTTagByte> {

        @Override
        public NBTTagByte toNBT(Boolean value) {
            return new NBTTagByte((byte) (value ? 1 : 0));
        }

        @Override
        public Boolean fromNBT(NBTBase nbt) {
            return cast(nbt, NBTTagByte.class).getByte() == 1;
        }
    }
    class StringAdapter implements NBTTypeAdapter<String, NBTTagString> {

        @Override
        public NBTTagString toNBT(String value) {
            return new NBTTagString(value);
        }

        @Override
        public String fromNBT(NBTBase nbt) {
            return cast(nbt, NBTTagString.class).getString();
        }
    }
    class ItemStackAdapter implements NBTTypeAdapter<ItemStack, NBTTagCompound> {

        @Override
        public NBTTagCompound toNBT(ItemStack value) {
            return value.writeToNBT(new NBTTagCompound());
        }

        @Override
        public ItemStack fromNBT(NBTBase nbt) {
            return new ItemStack(cast(nbt, NBTTagCompound.class));
        }
    }
    class BlockPosAdapter implements NBTTypeAdapter<BlockPos, NBTTagLong> {

        @Override
        public NBTTagLong toNBT(BlockPos value) {
            return new NBTTagLong(value.toLong());
        }

        @Override
        public BlockPos fromNBT(NBTBase nbt) {
            return BlockPos.fromLong(cast(nbt, NBTTagLong.class).getLong());
        }
    }
    class UUIDAdapter implements NBTTypeAdapter<UUID, NBTTagString> {

        @Override
        public NBTTagString toNBT(UUID value) {
            return new NBTTagString(value.toString());
        }

        @Override
        public UUID fromNBT(NBTBase nbt) {
            String serialized = cast(nbt, NBTTagString.class).getString();
            // avoid IllegalArgumentException when the NBT is invalid
            return serialized.isEmpty() ? new UUID(0,0) : UUID.fromString(serialized);
        }
    }
}
