package ladylib.client.command;

import ladylib.client.shader.ShaderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

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
        ShaderUtil.loadShaders(Minecraft.getMinecraft().getResourceManager());
    }
}
