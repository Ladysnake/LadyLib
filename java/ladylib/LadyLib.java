package ladylib;

import com.google.common.base.Preconditions;
import com.google.common.collect.SetMultimap;
import com.google.gson.reflect.TypeToken;
import ladylib.client.ClientHandler;
import ladylib.client.particle.ParticleManager;
import ladylib.nbt.MalformedNBTException;
import ladylib.nbt.NBTTypeAdapter;
import ladylib.nbt.TagAdapters;
import ladylib.networking.HTTPRequestHelper;
import ladylib.registration.AutoRegistrar;
import ladylib.registration.BlockRegistrar;
import ladylib.registration.ItemRegistrar;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.nbt.NBTBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
    private static final Map<String, LLibContainer> allInstances = new HashMap<>();

    @Mod.Instance
    public static LadyLib instance;

    private AutoRegistrar registrar;

    @SideOnly(Side.CLIENT)
    private ClientHandler clientHandler;

    public static boolean isDevEnv() {
        return (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment");
    }

    public static void debug(Object message) {
        if (isDevEnv()) {
            System.out.println(message);
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
    @Contract("null -> null;")
    public static NBTBase toNBT(Object src) {
        return src == null ? null : toNBT(src, src.getClass());
    }

    /**
     * This method serializes the specified object, including those of generic types, into its
     * equivalent Json representation. This method must be used if the specified object is a generic
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
    @SuppressWarnings("unchecked")
    @Contract("null, _ -> null; !null, null -> fail")
    public static NBTBase toNBT(Object src, Type typeOfSrc) {
        if (src == null) return null;
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(typeOfSrc), false);
        return adapter.toNBT(src);
    }

    /**
     * This method deserializes the specified NBT data structure into an object of the
     * specified type. This method is useful if the specified object is a generic type.
     *
     * @param <T>     the type of the desired object
     * @param nbt     the root of the NBT data structure from which the object is to
     *                be deserialized
     * @param typeOfT The specific genericized type of src. If src is not of generic type,
     *                you can simply pass its class. Otherwise, you can obtain this type by using the
     *                {@link com.google.gson.reflect.TypeToken} class. For example, to get the type for
     *                {@code Collection<Foo>}, you should use:
     *                <pre>
     *                Type typeOfT = new TypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
     *                </pre>
     * @return an object of type T from the NBT. Returns {@code null} if {@code nbt} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    @Contract("null, _ -> null; !null, _ -> !null; !null, null -> fail")
    public static <T> T fromNBT(NBTBase nbt, Type typeOfT) throws MalformedNBTException {
        if (nbt == null) return null;
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
    public static void deserializeNBT(@Nonnull Object target, NBTBase nbt) throws MalformedNBTException {
        if (nbt == null) return;
        NBTTypeAdapter adapter = TagAdapters.getNBTAdapter(TypeToken.get(target.getClass()), true);
        adapter.fromNBT(target, nbt);
    }

    @SideOnly(Side.CLIENT)
    public static ParticleManager getParticleManager() {
        return instance.clientHandler.getParticleManager();
    }

    /**
     * Call this during {@link net.minecraftforge.fml.common.event.FMLPreInitializationEvent}
     */
    @Mod.EventHandler
    public void preInit(@Nonnull FMLPreInitializationEvent event) {
        registrar = new AutoRegistrar(event.getAsmData());
        MinecraftForge.EVENT_BUS.register(registrar);
        MinecraftForge.EVENT_BUS.register(registrar.getItemRegistrar());
        MinecraftForge.EVENT_BUS.register(registrar.getBlockRegistrar());
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            clientHandler = new ClientHandler();
            clientHandler.clientInit();
        }
        registrar.autoRegisterTileEntities(event.getAsmData());
        injectContainers(event.getAsmData());
    }

    @Mod.EventHandler
    public void init(@Nonnull FMLInitializationEvent event) {
        HTTPRequestHelper.INSTANCE.start();
        HTTPRequestHelper.getJSON("https://ladysnake.glitch.me/gaspunk/users", System.out::println);
    }

    private void injectContainers(ASMDataTable asmData) {
        try {
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

    public Collection<LLibContainer> getAllInstances() {
        return allInstances.values();
    }

    public LLibContainer getContainer(String modid) {
        return allInstances.computeIfAbsent(modid, id -> new LLibContainer(Loader.instance().getIndexedModList().get(id)));
    }

    public ItemRegistrar getItemRegistrar() {
        return registrar.getItemRegistrar();
    }

    public BlockRegistrar getBlockRegistrar() {
        return registrar.getBlockRegistrar();
    }

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LLInstance {
        /**
         * Optional owner modid, required if this annotation is on something that is not inside the main class of a mod container.
         */
        String owner() default "";
    }
}
