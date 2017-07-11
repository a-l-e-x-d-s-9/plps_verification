package verification;

import java.awt.Point;

/**
 * Created by alexds9 on 06/06/17.
 */
public class UppaalName {
    public Point  place;
    public String content;

    public UppaalName( Point place, String content )
    {
        this.place      = place;
        this.content    = content;
    }

    public String toString()
    {
        return String.format( "\t\t\t<name x=\"%d\" y=\"%d\">%s</name>\n", this.place.x, this.place.y, this.content );
    }
}
