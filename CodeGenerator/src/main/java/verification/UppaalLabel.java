package verification;

import java.awt.Point;

/**
 * Created by alexds9 on 06/06/17.
 */
public class UppaalLabel {
    public String kind;
    public Point  place;
    public String content;

    public UppaalLabel( String kind, Point place, String content )
    {
        this.kind       = kind;
        this.place      = place;
        this.content    = content;
    }

    static public String urgent()
    {
        return "\t\t\t<urgent/>\n";
    }

    public String toString()
    {
        return String.format( "\t\t\t<label kind=\"%s\" x=\"%d\" y=\"%d\">%s</label>\n", this.kind, this.place.x, this.place.y, this.content );
    }
}
