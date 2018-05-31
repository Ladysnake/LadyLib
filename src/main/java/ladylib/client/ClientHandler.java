package ladylib.client;

import ladylib.client.particle.ParticleManager;
import ladylib.client.shader.ShaderUtil;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;

public class ClientHandler implements IClientHandler {
    private ParticleManager particleManager;

    public ClientHandler() {
        particleManager = new ParticleManager();
    }

    /**
     * Initializes client-only helpers like {@link ShaderUtil} or {@link ParticleManager}
     */
    public void clientInit() {
        ShaderUtil.init();
        MinecraftForge.EVENT_BUS.register(particleManager);
    }

    @Nonnull
    public ParticleManager getParticleManager() {
        return particleManager;
    }
}
