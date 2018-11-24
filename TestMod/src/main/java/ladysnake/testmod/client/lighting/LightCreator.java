package ladysnake.testmod.client.lighting;

import ladylib.LadyLib;
import ladylib.client.lighting.AttachedCheapLight;
import ladylib.client.lighting.CheapLightManager;
import ladylib.client.lighting.MutableCheapLight;
import ladylib.client.lighting.SimpleCheapLight;
import ladylib.compat.EnhancedBusSubscriber;
import ladysnake.testmod.TestMod;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.awt.*;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

@EnhancedBusSubscriber(value = TestMod.MODID, side = Side.CLIENT)
public class LightCreator {
    private final Set<MutableCheapLight> summonedLights = Collections.newSetFromMap(new WeakHashMap<>());

    @SubscribeEvent
    public void onItemToss(ItemTossEvent event) {
        if (!LadyLib.isDevEnv()) return;
        Item item = event.getEntityItem().getItem().getItem();
        if (item == Items.GLOWSTONE_DUST || item == Items.COAL || item == Items.BLAZE_POWDER) {
            // This is disgusting but screw packets, this is a test mod
            Minecraft.getMinecraft().addScheduledTask(() -> {
                if (item == Items.GLOWSTONE_DUST) {
                    SimpleCheapLight light = new SimpleCheapLight(event.getPlayer().getPositionVector(), 0.8f, new Color(0.5f, 0.3f, 1.0f, 1.0f));
                    summonedLights.add(light);
                    CheapLightManager.INSTANCE.addLight(light);
                } else if (item == Items.BLAZE_POWDER) {
                    SimpleCheapLight light = new SimpleCheapLight(event.getPlayer().getPositionVector(), 1.4f, new Color(1f, 0.6f, 0.2f));
                    summonedLights.add(light);
                    CheapLightManager.INSTANCE.addLight(new AttachedCheapLight(light, event.getPlayer()));
                } else {
                    summonedLights.forEach(l -> l.setExpired(true));
                }
            });
            event.setCanceled(true);
        }
    }
}
