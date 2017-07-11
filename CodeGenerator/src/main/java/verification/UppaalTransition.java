package verification;

import java.awt.Point;
import java.util.*;
/**
 * Created by alexds9 on 05/06/17.
 */
public class UppaalTransition {
    public String               source_id;
    public String               target_id;
    public Point                labels_place;
    public UppaalBuilder.Side   align_to;
    public String               guard;
    public String               synchronisation;
    public String               assignment;
    public String               probability;
    public List<Point>          nails;
    public String               extra;


    public UppaalTransition( String source_id, String target_id, Point labels_place, UppaalBuilder.Side align_to, String guard, String synchronisation, String assignment, String probability, List<Point> nails )
    {
        this.source_id          = source_id;
        this.target_id          = target_id;
        this.labels_place       = labels_place;
        this.align_to           = align_to;
        this.guard              = guard;
        this.synchronisation    = synchronisation;
        this.assignment         = assignment;
        this.probability        = probability;
        this.nails              = nails;
        this.extra              = "";
    }

    public UppaalTransition( String source_id, String target_id, Point labels_place, UppaalBuilder.Side align_to, String guard, String synchronisation, String assignment, String probability, List<Point> nails, String extra )
    {
        this( source_id, target_id, labels_place, align_to, guard, synchronisation, assignment, probability, nails );
        this.extra = extra;
    }


    public void add_transition( UppaalPTA uppaal_template, Point offset )
    {
        UppaalBuilder.move_by( this.labels_place, offset );
        if ( null != this.nails )
        {
            for ( Point nail : this.nails )
            {
                UppaalBuilder.move_by( nail, offset );
            }
        }
        add_transition( uppaal_template );
    }

    public void add_transition( UppaalPTA uppaal_template )
    {
        add_transition( uppaal_template.transitions );
    }

    public void add_transition( StringBuffer transitions )
    {
        transitions.append(    "\t\t<transition>\n" +
                "\t\t\t<source ref=\"" + this.source_id + "\"/>\n" +
                "\t\t\t<target ref=\"" + this.target_id + "\"/>\n" );

        if ( null != this.guard && false == this.guard.isEmpty() ) {
            transitions.append( "\t\t\t<label kind=\"guard\" x=\"" + String.valueOf( UppaalBuilder.label_align_by_x(this.labels_place.x, this.guard, this.align_to)) + "\" y=\"" + String.valueOf(this.labels_place.y) + "\">" + this.guard + "</label>\n" );
            this.labels_place.y += UppaalBuilder.units_letter_hight * UppaalBuilder.text_lines( this.guard );
        }
        if ( null != this.synchronisation && false == this.synchronisation.isEmpty() ) {
            transitions.append( "\t\t\t<label kind=\"synchronisation\" x=\"" + String.valueOf( UppaalBuilder.label_align_by_x(this.labels_place.x, this.synchronisation, this.align_to)) + "\" y=\"" + String.valueOf(this.labels_place.y) + "\">" + this.synchronisation + "</label>\n" );
            this.labels_place.y += UppaalBuilder.units_letter_hight * UppaalBuilder.text_lines( this.synchronisation );
        }
        if ( null != this.assignment && false == this.assignment.isEmpty() ) {
            transitions.append( "\t\t\t<label kind=\"assignment\" x=\"" + String.valueOf( UppaalBuilder.label_align_by_x(this.labels_place.x, this.assignment, this.align_to)) + "\" y=\"" + String.valueOf(this.labels_place.y) + "\">" + this.assignment + "</label>\n" );
            this.labels_place.y += UppaalBuilder.units_letter_hight * UppaalBuilder.text_lines( this.assignment );
        }
        if ( null != this.probability && false == this.probability.isEmpty() ) {
            transitions.append( "\t\t\t<label kind=\"probability\" x=\"" + String.valueOf( UppaalBuilder.label_align_by_x(this.labels_place.x, this.probability, this.align_to)) + "\" y=\"" + String.valueOf(this.labels_place.y) + "\">" + this.probability + "</label>\n" );
            this.labels_place.y += UppaalBuilder.units_letter_hight * UppaalBuilder.text_lines( this.probability );
        }
        if ( null != this.extra && false == this.extra.isEmpty() ) {
            transitions.append( "\t\t\t" + this.extra );
        }

        if ( null != this.nails )
        {
            for ( Point nail : this.nails )
            {
                transitions.append( "\t\t\t<nail x=\"" + String.valueOf(nail.x) + "\" y=\"" + String.valueOf(nail.y) + "\"/>\n" );
            }
        }

        transitions.append( "\t\t</transition>\n" );
    }

}
