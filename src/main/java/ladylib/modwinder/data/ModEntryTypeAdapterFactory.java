package ladylib.modwinder.data;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import javax.annotation.Nullable;
import java.io.IOException;

public class ModEntryTypeAdapterFactory implements TypeAdapterFactory {
    @Nullable
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (ModEntry.class.isAssignableFrom(type.getRawType())) {
            return new ModEntryTypeAdapter<>(gson.getDelegateAdapter(this, type));
        }
        return null;
    }

    private static class ModEntryTypeAdapter<T> extends TypeAdapter<T> {
        private TypeAdapter<T> delegate;

        public ModEntryTypeAdapter(TypeAdapter<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(JsonWriter out, T value) throws IOException {
            delegate.write(out, value);
        }

        @Override
        public T read(JsonReader in) throws IOException {
            T ret = delegate.read(in);
            ((ModEntry)ret).init();
            return ret;
        }
    }
}
