package verification;

import java.awt.Point;

/**
 * Created by alexds9 on 06/06/17.
 */
public class UppaalBranchpoint {
    public String id;
    public Point  place;

    public UppaalBranchpoint( String id, Point place )
    {
        this.id     = id;
        this.place  = place;
    }

    public void append( UppaalPTA uppaal_template, Point offset )
    {
        uppaal_template.append_branchpoints( this.toString( offset ) );
    }

    public String toString()
    {
        return this.toString( new Point(0,0) );
    }

    public String toString( Point offset )
    {
        return String.format( "\t\t\t<branchpoint id=\"%s\" x=\"%d\" y=\"%d\"></branchpoint>\n", this.id, this.place.x + offset.x, this.place.y + offset.y );
    }
}
