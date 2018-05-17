package ladylib.nbt;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public interface BaseTypeAdapters {

    class IntAdapter implements NBTTypeAdapter<Integer, NBTTagInt> {

        @Override
        public NBTTagInt toNBT(Integer value) {
            return new NBTTagInt(value);
        }

        @Override
        public Integer fromNBT(NBTTagInt nbtTagInt) {
            return nbtTagInt.getInt();
        }
    }

    class DoubleAdapter implements NBTTypeAdapter<Double, NBTTagDouble> {

        @Override
        public NBTTagDouble toNBT(Double value) {
            return new NBTTagDouble(value);
        }

        @Override
        public Double fromNBT(NBTTagDouble nbtTagDouble) {
            return nbtTagDouble.getDouble();
        }
    }
    class FloatAdapter implements NBTTypeAdapter<Float, NBTTagFloat> {

        @Override
        public NBTTagFloat toNBT(Float value) {
            return new NBTTagFloat(value);
        }

        @Override
        public Float fromNBT(NBTTagFloat nbtTagFloat) {
            return nbtTagFloat.getFloat();
        }
    }
    class LongAdapter implements NBTTypeAdapter<Long, NBTTagLong> {

        @Override
        public NBTTagLong toNBT(Long value) {
            return new NBTTagLong(value);
        }

        @Override
        public Long fromNBT(NBTTagLong nbtTagLong) {
            return nbtTagLong.getLong();
        }
    }
    class ShortAdapter implements NBTTypeAdapter<Short, NBTTagShort> {

        @Override
        public NBTTagShort toNBT(Short value) {
            return new NBTTagShort(value);
        }

        @Override
        public Short fromNBT(NBTTagShort nbtTagShort) {
            return nbtTagShort.getShort();
        }
    }
    class ByteAdapter implements NBTTypeAdapter<Byte, NBTTagByte> {

        @Override
        public NBTTagByte toNBT(Byte value) {
            return new NBTTagByte(value);
        }

        @Override
        public Byte fromNBT(NBTTagByte nbtTagByte) {
            return nbtTagByte.getByte();
        }
    }
    class BooleanAdapter implements NBTTypeAdapter<Boolean, NBTTagByte> {

        @Override
        public NBTTagByte toNBT(Boolean value) {
            return new NBTTagByte((byte) (value ? 1 : 0));
        }

        @Override
        public Boolean fromNBT(NBTTagByte nbtTagByte) {
            return nbtTagByte.getByte() == 1;
        }
    }
    class StringAdapter implements NBTTypeAdapter<String, NBTTagString> {

        @Override
        public NBTTagString toNBT(String value) {
            return new NBTTagString(value);
        }

        @Override
        public String fromNBT(NBTTagString nbtTagString) {
            return nbtTagString.getString();
        }
    }
    class ItemStackAdapter implements NBTTypeAdapter<ItemStack, NBTTagCompound> {

        @Override
        public NBTTagCompound toNBT(ItemStack value) {
            return value.writeToNBT(new NBTTagCompound());
        }

        @Override
        public ItemStack fromNBT(NBTTagCompound nbtTagCompound) {
            return new ItemStack(nbtTagCompound);
        }
    }
    class BlockPosAdapter implements NBTTypeAdapter<BlockPos, NBTTagLong> {

        @Override
        public NBTTagLong toNBT(BlockPos value) {
            return new NBTTagLong(value.toLong());
        }

        @Override
        public BlockPos fromNBT(NBTTagLong nbtTagLong) {
            return BlockPos.fromLong(nbtTagLong.getLong());
        }
    }
    class UUIDAdapter implements NBTTypeAdapter<UUID, NBTTagString> {

        @Override
        public NBTTagString toNBT(UUID value) {
            return new NBTTagString(value.toString());
        }

        @Override
        public UUID fromNBT(NBTTagString nbtTagString) {
            String serialized = nbtTagString.getString();
            // avoid IllegalArgumentException when the NBT is invalid
            return serialized.isEmpty() ? new UUID(0,0) : UUID.fromString(serialized);
        }
    }
}
