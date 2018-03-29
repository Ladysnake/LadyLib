package ladylib.client;

import ladylib.client.particle.ParticleManager;

import javax.annotation.Nonnull;

public interface IClientHandler {
    @Nonnull
    ParticleManager getParticleManager();
}
