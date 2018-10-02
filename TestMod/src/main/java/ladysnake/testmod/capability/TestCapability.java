package ladysnake.testmod.capability;

import ladylib.capability.AutoCapability;
import ladylib.capability.SimpleProvider;
import ladysnake.testmod.TestMod;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Objects;

@AutoCapability(TestHandler.class)
@Mod.EventBusSubscriber(modid = TestMod.MODID)
public class TestCapability implements TestHandler {
    @Override
    public void sayHello() {
        System.out.println("Hello");
    }

    private static final ResourceLocation TEST_CAP = new ResourceLocation(TestMod.MODID, "test_cap");
    @CapabilityInject(TestHandler.class)
    public static Capability<TestHandler> CAPABILITY_TEST;

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<World> event) {
        event.addCapability(TEST_CAP, new SimpleProvider<>(CAPABILITY_TEST, new TestCapability()));
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof EntityPlayerMP) {
            Objects.requireNonNull(event.getWorld().getCapability(CAPABILITY_TEST, null)).sayHello();
        }
    }
}
