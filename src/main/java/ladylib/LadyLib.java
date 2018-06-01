package ladylib;

import com.google.common.base.Preconditions;
import com.google.common.collect.SetMultimap;
import com.google.gson.reflect.TypeToken;
import ladylib.client.ClientHandler;
import ladylib.client.particle.LLParticleManager;
import ladylib.nbt.NBTDeserializationException;
import ladylib.nbt.NBTTypeAdapter;
import ladylib.nbt.TagAdapters;
import ladylib.nbt.internal.DefaultValuesSearch;
import ladylib.registration.BlockRegistrar;
import ladylib.registration.ItemRegistrar;
import ladylib.registration.internal.AutoRegistrar;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Mod(
        modid = LadyLib.MOD_ID,
        name = LadyLib.MOD_NAME,
        version = LadyLib.VERSION,
        dependencies = "before:all"
)
public class LadyLib {
    public static final String MOD_ID = "ladylib";
    public static final String MOD_NAME = "LadyLib";
    public static final String VERSION = "@VERSION@";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    private static final PrintStream DEBUG_STREAM = new TracingPrintStream(LogManager.getLogger("DEBUG"), System.out);
    private static final Map<String, LLibContainer> allInstances = new HashMap<>();

    @Mod.Instance
    public static LadyLib instance;

    private AutoRegistrar registrar;

    @SideOnly(Side.CLIENT)
    private ClientHandler clientHandler;

    /**
     * Checks if the current minecraft instance is running in a development environment. <br>
     * Specifically, checks whether the environment is obfuscated or not.
     * @return true if the current environment is deobfuscated
     */
    public static boolean isDevEnv() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }

    /**
     * Prints a message if and only if the game is currently running in a development environment
     * @param message the message to print
     */
    public static void debug(Object message) {
        if (isDevEnv()) {
            // use a dedicated print stream to accurately display the call origin
            DEBUG_STREAM.println(message);
        }
    }

    /**
     * This method serializes the specified object into its equivalent Non Binary Tag representation.
     * This method should be used when the specified object is not a generic type. This method uses
     * {@link Class#getClass()} to get the type for the specified object, but the
     * {@code getClass()} loses the generic type information because of the Type Erasure feature
     * of Java. Note that this method works fine if the any of the object fields are of generic type,
     * just the object itself should not be of a generic type. If the object is of generic type, use
     * {@link #toNBT(Object, Type)} instead.
     *
     * @param src the object for which NBT representation is to be created
     * @return NBT representation of {@code src}.
     */
    @Nullable
    public static NBTBase toNBT(@Nullable Object src) {
        return src == null ? null : toNBT(src, src.getClass());
    }

    /**
     * This method serializes the specified object, including those of generic types, into its
     * equivalent NBT representation. This method must be used if the specified object is a generic
     * type. For non-generic objects, use {@link #toNBT(Object)} instead.
     *
     * @param src       the object for which NBT representation is to be created
     * @param typeOfSrc The specific genericized type of src. You can obtain
     *                  this type by using the {@link com.google.gson.reflect.TypeToken} class. For example,
     *                  to get the type for {@code Collection<Foo>}, you should use:
     *                  <pre>
     *                  Type typeOfSrc = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
     *                  </pre>
     * @return NBT representation of {@code src}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static NBTBase toNBT(Object src, Type typeOfSrc) {
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(typeOfSrc), false);
        return adapter.toNBT(src);
    }

    /**
     * This method deserializes the NBT read from the specified parse tree into an object of the
     * specified type. It is not suitable to use if the specified class is a generic type since it
     * will not have the generic type information because of the Type Erasure feature of Java.
     * Therefore, this method should not be used if the desired type is a generic type. Note that
     * this method works fine if the any of the fields of the specified object are generics, just the
     * object itself should not be a generic type. For the cases when the object is of generic type,
     * invoke {@link #fromNBT(NBTBase, Type)}.
     * @param <T> the type of the desired object
     * @param nbt     the root of the NBT data structure from which the object is to
     *                be deserialized
     * @param classOfT The class of T
     * @return an object of type T from the NBT. Returns {@code null} if {@code NBT} is {@code null}.
     */
    @Nullable
    public static <T> T fromNBT(@Nullable NBTBase nbt, Class<T> classOfT) throws NBTDeserializationException {
        return fromNBT(nbt, (Type) classOfT);
    }

    /**
     * This method deserializes the specified NBT data structure into an object of the
     * specified type. This method is useful if the specified object is a generic type.
     *
     * @param <T>     the type of the desired object
     * @param nbt     the root of the NBT data structure from which the object is to
     *                be deserialized
     * @param typeOfT The specific genericized type of src. You can obtain this type by using the
     *                {@link com.google.gson.reflect.TypeToken} class. <br>
     *                For example, to get the type for
     *                {@code Collection<Foo>}, you should use:
     *                <pre>
     *                Type typeOfT = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
     *                </pre>
     * @return an object of type T from the NBT. Returns {@code null} if {@code nbt} is {@code null}.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T fromNBT(@Nullable NBTBase nbt, Type typeOfT) throws NBTDeserializationException {
        if (nbt == null) {
            return null;
        }
        Preconditions.checkNotNull(typeOfT);
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(typeOfT), false);
        return (T) adapter.fromNBT(nbt);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static NBTBase serializeNBT(@Nonnull Object src) {
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(src.getClass()), true);
        return adapter.toNBT(src);
    }

    @SuppressWarnings("unchecked")
    public static void deserializeNBT(@Nonnull Object target, @Nullable NBTBase nbt) throws NBTDeserializationException {
        if (nbt == null) {
            return;
        }
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(target.getClass()), true);
        adapter.fromNBT(target, nbt);
    }

    /**
     * @return LadyLib's custom particle manager
     */
    @SideOnly(Side.CLIENT)
    public static LLParticleManager getParticleManager() {
        return instance.clientHandler.getParticleManager();
    }

    /**
     * LadyLib pre-initialization
     */
    @Mod.EventHandler
    public void preInit(@Nonnull FMLPreInitializationEvent event) {
        ASMDataTable dataTable = event.getAsmData();
        registrar = new AutoRegistrar(dataTable);
        MinecraftForge.EVENT_BUS.register(registrar);
        MinecraftForge.EVENT_BUS.register(registrar.getItemRegistrar());
        MinecraftForge.EVENT_BUS.register(registrar.getBlockRegistrar());
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            clientHandler = new ClientHandler();
            clientHandler.clientInit();
        }
        registrar.autoRegisterTileEntities(dataTable);
        injectContainers(dataTable);
        DefaultValuesSearch.searchDefaultValues(dataTable);

        NBTTagString nbtTagString = new NBTTagString();
        LadyLib.fromNBT(nbtTagString, int.class);
    }

    /**
     * LadyLib initialization
     */
    @Mod.EventHandler
    public void init(@Nonnull FMLInitializationEvent event) {

    }

    /**
     * Injects LLibContainer into their respective instance fields
     */
    private void injectContainers(ASMDataTable asmData) {
        try {
            // hook into forge's already existing mechanism
            Method parseSimpleFieldAnnotation = FMLModContainer.class.getDeclaredMethod("parseSimpleFieldAnnotation", SetMultimap.class, String.class, Function.class);
            parseSimpleFieldAnnotation.setAccessible(true);
            for (ModContainer container : Loader.instance().getModList()) {
                SetMultimap<String, ASMDataTable.ASMData> annotations = asmData.getAnnotationsFor(container);
                if (container instanceof FMLModContainer) {
                    parseSimpleFieldAnnotation.invoke(container, annotations, LLInstance.class.getName(), (Function<ModContainer, Object>) mc -> getContainer(mc.getModId()));
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return the list of every mod wrapper created by LadyLib
     */
    public Collection<LLibContainer> getAllInstances() {
        return allInstances.values();
    }

    /**
     * Gets LadyLib's wrapper container used to provide mod-specific behaviour.
     * @param modid the mod id owning the container
     * @return the mod's container
     */
    public LLibContainer getContainer(String modid) {
        return allInstances.computeIfAbsent(modid, id -> new LLibContainer(Loader.instance().getIndexedModList().get(id)));
    }

    /**
     * Gets LadyLib's {@link ItemRegistrar item registrar}.
     * The item registrar offers various methods to make item registration easier.
     */
    public ItemRegistrar getItemRegistrar() {
        return registrar.getItemRegistrar();
    }

    /**
     * Gets LadyLib's {@link BlockRegistrar block registrar}.
     * The block registrar offers various methods to make block registration easier.
     */
    public BlockRegistrar getBlockRegistrar() {
        return registrar.getBlockRegistrar();
    }

    /**
     * Populate the annotated field with the LadyLib mod wrapper instance based on the specified ModId.
     * This can be used to retrieve instances of other mods.
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LLInstance {
        /**
         * The id of the mod which wrapper object is to be injected into this field
         */
        String value() default "";

        /**
         * Optional owner mod id, required if this annotation is on something
         * that is not inside the main class of a mod container.
         */
        String owner() default "";
    }
}
