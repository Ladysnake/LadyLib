package ladylib.client;

import ladylib.client.particle.LLParticleManager;
import ladylib.client.shader.ShaderUtil;
import ladylib.misc.ReflectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResourcePack;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import java.util.List;

public class ClientHandler implements IClientHandler {
    private LLParticleManager particleManager;
    private static ResourceProxy resourceProxy;

    public static void hookResourceProxy() {
        // the resource proxy needs to be registered here to exist when Minecraft looks for resource packs
        List<IResourcePack> packs = ReflectionUtil.getPrivateValue(Minecraft.class, Minecraft.getMinecraft(), "field_110449_ao", List.class);
        packs.add(resourceProxy = new ResourceProxy("minecraft"));
    }

    public ClientHandler() {
        particleManager = new LLParticleManager();
    }

    /**
     * Initializes client-only helpers like {@link ShaderUtil} or {@link LLParticleManager}
     */
    public void clientInit() {
        ShaderUtil.init();
        MinecraftForge.EVENT_BUS.register(particleManager);
    }

    @Nonnull
    @Override
    public LLParticleManager getParticleManager() {
        return particleManager;
    }

    @Override
    public void addResourceOverride(String owner, String dir, String... files) {
        resourceProxy.addResource(owner, "minecraft", dir, files);
    }
}
