package ladylib.client.lighting;

import net.minecraft.util.math.Vec3d;

import java.awt.*;

public class CheapLight {
    private Vec3d pos;
    private float radius;
    private Color color;

    public CheapLight(Vec3d pos, float radius, Color color) {
        this.pos = pos;
        this.radius = radius;
        this.color = color;
    }

    public Vec3d getPos() {
        return pos;
    }

    public float getRadius() {
        return radius;
    }

    public Color getColor() {
        return color;
    }
}
