package ladylib.nbt.serialization;

/**
 * Thrown when a NBT serialization operation fails
 */
public class NBTDeserializationException extends RuntimeException {

    public NBTDeserializationException() {
        super();
    }

    public NBTDeserializationException(String message) {
        super(message);
    }

    public NBTDeserializationException(Throwable cause) {
        super(cause);
    }

    public NBTDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
