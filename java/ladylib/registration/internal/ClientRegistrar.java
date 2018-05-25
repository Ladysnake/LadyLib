package ladylib.registration.internal;

import ladylib.LadyLib;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Loader;
import org.objectweb.asm.Type;

/**
 * Handles some client-specific registration
 */
public class ClientRegistrar {
    static <T extends TileEntity> void registerTESR(Class<T> tileClass, Type tesrType) {
        if (tesrType == null) return;
        try {
            @SuppressWarnings("unchecked") Class<? extends TileEntitySpecialRenderer<T>> tesrClass =
                    (Class<? extends TileEntitySpecialRenderer<T>>) Class.forName(tesrType.getClassName(), true, Loader.instance().getModClassLoader());
            if (tesrClass.equals(TileEntitySpecialRenderer.class)) return;
            TileEntitySpecialRenderer<T> tesr = tesrClass.newInstance();
            ClientRegistry.bindTileEntitySpecialRenderer(tileClass, tesr);
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            LadyLib.LOGGER.error("Error while registering a TESR", e);
        }
    }
}
