package verification;

import java.awt.Point;

/**
 * Created by alexds9 on 05/06/17.
 */
public class UppaalLocation {
    public String               location_name;
    public UppaalBuilder.Side   name_side;
    public Point                location_center;
    public String               exp;
    public UppaalBuilder.Side   exp_side;
    public String               inv;
    public UppaalBuilder.Side   inv_side;
    public boolean              is_urgent;
    public String               extra;
    public String               location_id;

    public UppaalLocation( String location_name, UppaalBuilder.Side name_side, Point location_center, String exp, UppaalBuilder.Side exp_side, String inv, UppaalBuilder.Side inv_side, boolean is_urgent)
    {
        this.location_name      = location_name;
        this.name_side          = name_side;
        this.location_center    = location_center;
        this.exp                = exp;
        this.exp_side           = exp_side;
        this.inv                = inv;
        this.inv_side           = inv_side;
        this.is_urgent          = is_urgent;
        this.extra              = "";
        this.location_id        = null;
    }

    public UppaalLocation( String location_name, UppaalBuilder.Side name_side, Point location_center, String exp, UppaalBuilder.Side exp_side, String inv, UppaalBuilder.Side inv_side, boolean is_urgent, String extra )
    {
        this( location_name, name_side, location_center, exp, exp_side, inv, inv_side, is_urgent);
        this.extra  = extra;
    }


    public String add_location( UppaalPTA uppaal_template, Point offset )
    {
        UppaalBuilder.move_by( this.location_center, offset );
        return add_location( uppaal_template );
    }

    public String add_location( UppaalPTA uppaal_template )
    {

        /*
        X Axis: Left - ; Right  + .
        Y Axis: Top    - ;
                Bottom + .
        Square size: 34 x 34
        Location anchor point is in the center of the location.
        Location Diameter * 2 = Square Diagonal. Location Radius = ~12.
        Label anchor point in in the top-left corner.
        Label height: ~17, width: "G"=11.12; "h"=9; "i"=3.18; tentative average width set to 8.
        */
        Point position_name = UppaalBuilder.label_position( this.location_center, UppaalBuilder.units_buffer_size + UppaalBuilder.units_location_radius, this.location_name, this.name_side );

        if ( null == this.location_id || this.location_id.isEmpty() )
        {
            this.location_id = uppaal_template.get_new_location_id();
        }
        Point offset_name   = new Point(0,0);
        Point offset_inv    = new Point(0,0);
        Point offset_exp    = new Point(0,0);


        uppaal_template.append_locations(     "\t\t<location id=\"" + this.location_id + "\" x=\"" + String.valueOf(this.location_center.x) + "\" y=\"" + String.valueOf(this.location_center.y) + "\">\n" );

        if ( null != this.exp && false == this.exp.isEmpty() ) {
            if ( this.exp_side.is_equal( this.name_side ) ) {
                if ( UppaalBuilder.is_limited_up( this.exp_side ) )
                {
                    offset_exp.y += UppaalBuilder.units_letter_hight;
                }
                else
                {
                    offset_name.y -= UppaalBuilder.units_letter_hight;
                }

                if ( null != this.inv && false == this.inv.isEmpty() && this.exp_side.is_equal( this.inv_side ) )
                {
                    if ( UppaalBuilder.is_limited_up( this.exp_side ) )
                    {
                        offset_inv.y += 2 * UppaalBuilder.units_letter_hight;
                    }
                    else
                    {
                        offset_name.y -= UppaalBuilder.units_letter_hight;
                        offset_exp.y  -= UppaalBuilder.units_letter_hight;
                    }
                }
            }
            else
            {
                if ( null != this.inv && false == this.inv.isEmpty() && this.name_side.is_equal( this.inv_side ) )
                {
                    if ( UppaalBuilder.is_limited_up( this.name_side ) )
                    {
                        offset_inv.y += UppaalBuilder.units_letter_hight;
                    }
                    else
                    {
                        offset_name.y -= UppaalBuilder.units_letter_hight;
                    }
                }
            }
        }
        else
        {
            if ( null != this.inv && false == this.inv.isEmpty() && this.inv_side.is_equal( this.name_side ) )
            {
                offset_name.y  -= UppaalBuilder.units_letter_hight;
            }
        }

        uppaal_template.append_locations(     "\t\t\t<name x=\"" + String.valueOf(position_name.x + offset_name.x) + "\" y=\"" + String.valueOf(position_name.y + offset_name.y) + "\">" + this.location_name + "</name>\n" );

        if ( null != this.exp && false == exp.isEmpty() ) {
            Point position_exp = UppaalBuilder.label_position( this.location_center, UppaalBuilder.units_buffer_size + UppaalBuilder.units_location_radius, this.exp, this.exp_side);
            uppaal_template.append_locations( "\t\t\t<label kind=\"exponentialrate\" x=\"" + String.valueOf(position_exp.x + offset_exp.x) + "\" y=\"" + String.valueOf(position_exp.y + offset_exp.y) + "\">" + this.exp + "</label>\n" );
        }

        if ( null != this.inv && false == inv.isEmpty() ) {
            Point position_inv = UppaalBuilder.label_position( this.location_center, UppaalBuilder.units_buffer_size + UppaalBuilder.units_location_radius, this.inv, this.inv_side);
            uppaal_template.append_locations( "\t\t\t<label kind=\"invariant\" x=\"" + String.valueOf(position_inv.x + offset_inv.x) + "\" y=\"" + String.valueOf(position_inv.y + offset_inv.y) + "\">" + this.inv + "</label>\n" );
        }
        if ( null != this.extra && false == extra.isEmpty() ) {
            uppaal_template.append_locations( "\t\t\t" + this.extra );
        }

        if ( true == is_urgent ) {
            uppaal_template.append_locations( UppaalLabel.urgent() );
        }

        uppaal_template.append_locations( "\t\t</location>\n" );

        return this.location_id;
    }



    public void append( StringBuffer string_buffer )
    {
        string_buffer.append(this.toString());
    }
}
