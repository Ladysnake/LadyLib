package ladylib.capability.internal;

import ladylib.LadyLib;
import ladylib.capability.SimpleProvider;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.message.FormattedMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class CapabilityEventHandler {
    final List<ProviderInfo<TileEntity, ?>> teProviders = new ArrayList<>();
    final List<ProviderInfo<ItemStack, ?>> itemProviders = new ArrayList<>();
    final List<ProviderInfo<Entity, ?>> entityProviders = new ArrayList<>();

    @SubscribeEvent
    public void attachTileEntityCapabilities(AttachCapabilitiesEvent<TileEntity> event) {
        for (ProviderInfo<TileEntity, ?> info : teProviders) {
            if (info.predicate.test(event.getObject())) {
                event.addCapability(info.key, info.createProvider());
            }
        }
    }
    @SubscribeEvent
    public void attachItemCapabilities(AttachCapabilitiesEvent<ItemStack> event) {
        for (ProviderInfo<ItemStack, ?> info : itemProviders) {
            if (info.predicate.test(event.getObject())) {
                event.addCapability(info.key, info.createProvider());
            }
        }
    }
    @SubscribeEvent
    public void attachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
        for (ProviderInfo<Entity, ?> info : entityProviders) {
            if (info.predicate.test(event.getObject())) {
                ICapabilityProvider provider = info.createProvider();
                if (provider != null) {
                    event.addCapability(info.key, provider);
                }
            }
        }
    }

    static class ProviderInfo<C extends ICapabilitySerializable,T> {
        private final ResourceLocation key;
        private final Predicate<C> predicate;
        private final Capability<T> capability;
        private final Callable<T> instanceFactory;

        ProviderInfo(ResourceLocation key, Predicate<C> predicate, Capability<T> capability, Callable<T> instanceFactory) {
            this.key = key;
            this.predicate = predicate;
            this.capability = capability;
            this.instanceFactory = instanceFactory;
        }

        ICapabilityProvider createProvider() {
            try {
                return new SimpleProvider<>(capability, instanceFactory.call());
            } catch (Exception e) {
                LadyLib.LOGGER.error(new FormattedMessage("Could not create capability provider for capability {}", capability), e);
            }
            return null;
        }
    }
}
