package ladylib;

import com.google.common.collect.SetMultimap;
import ladylib.client.ClientHandler;
import ladylib.client.particle.ParticleManager;
import ladylib.registration.AutoRegistrar;
import ladylib.registration.BlockRegistrar;
import ladylib.registration.ItemRegistrar;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
