package ladylib.client.lighting;

import java.awt.*;

public interface MutableCheapLight extends CheapLight {

    void setPosition(double x, double y, double z);

    void setRadius(float radius);

    void setColor(Color color);

    void setExpired(boolean expired);

}
