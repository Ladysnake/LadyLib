package ladylib.client.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.resource.VanillaResourceType;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fml.client.FMLClientHandler;

public class ShaderReloadCommand extends CommandBase {
    @Override
    public String getName() {
        return "ladylib_shader_reload";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "ladylib.command.shader_reload";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!ForgeModContainer.selectiveResourceReloadEnabled) {
            sender.sendMessage(new TextComponentTranslation("ladylib.warning.no_selective_reload").setStyle(new Style().setColor(TextFormatting.YELLOW)));
        }
        FMLClientHandler.instance().refreshResources(VanillaResourceType.SHADERS);
    }
}
