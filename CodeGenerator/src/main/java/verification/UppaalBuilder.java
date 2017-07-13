package verification;

import java.awt.Point;

/**
 * Created by alexds9 on 05/06/17.
 */
public class UppaalBuilder {

    static public final int units_square_length             = 34;
    static public final int units_location_radius           = 12;
    static public final int units_letter_hight              = 17;
    static public final int units_letter_width              = 7;
    static public final int units_buffer_size               = 10;
    static public final String STR_TRUE                     = "true";
    static public final String STR_FALSE                    = "false";
    static public final String STR_EQUAL                    = "==";
    static public final String STR_NOT_EQUAL                = "!=";
    static public final String STR_LESS_THAN                = "&lt;";
    static public final String STR_LESS_THAN_EQUAL          = "&lt;=";
    static public final String STR_GREATER_THAN             = "&gt;";
    static public final String STR_GREATER_THAN_EQUAL       = "&gt;=";
    static public final String STR_AMPERSAND                = "&amp;";
    static public final String STR_QUOT                     = "&quot;";
    static public final String STR_AND                      = STR_AMPERSAND + STR_AMPERSAND;
    static public final String STR_OR                       = "||";
    static public final String STR_NOT                      = "!";
    static public final String STR_COMMA                    = ",";
    static public final String STR_ASSIGNMENT               = "=";
    static public final String STR_NUMERIC_TRUE             = "1";
    static public final String STR_NUMERIC_FALSE            = "0";
    static public final int    INT_NUMERIC_TRUE             = 1;
    static public final int    INT_NUMERIC_FALSE            = 0;
    static public final int    INT_RESOURCE_INVALID         = -1;

    static void add_to_cumulative_assignment(StringBuffer cumulative_results_string, CharSequence addition )
    {
        add_to_cumulative_expression_with_operator( cumulative_results_string, addition, STR_COMMA );
    }

    public enum Side {
        /*
            123
            4*6
            789
         */

        none            (0),
        top_left        (1),
        top_center      (2),
        top_right       (3),
        middle_left     (4),
        middle_center   (5),
        middle_right    (6),
        bottom_left     (7),
        bottom_center   (8),
        bottom_right    (9);


        public int value;

        Side(int value)
        {
            this.value = value;
        }

        public boolean is_equal(Side other)
        {
            return this.value == other.value;
        }

    }

    static public String uppall_boolean( boolean value )
    {
        if ( false == value )
        {
            return STR_FALSE;
        }
        else
        {
            return STR_TRUE;
        }
    }

    static public String comply_string_single_line(String check_string )
    {
        return comply_string_multi_line(check_string).replace( "\n", " " );
    }

    static public String comply_string_multi_line(String check_string )
    {
        return check_string.replace( "&", STR_AMPERSAND).replace( "<", STR_LESS_THAN ).replace( ">", STR_GREATER_THAN).replace( "\"", STR_QUOT );
    }

    static public int squares_length( double squares_amount )
    {
        return (int)(Double.valueOf(units_square_length) * squares_amount);
    }

    static public int label_center_at_x( int x, int label_width )
    {//text_width( label_string )
        return x - ( ( label_width * units_letter_width ) / 2 );
    }

    static public int label_right_at_x( int x, String label )
    {
        return label_right_at_x(x, text_width(label));
    }

    static public int label_right_at_x( int x, int label_width )
    {
        return x - ( label_width * units_letter_width );
    }

    static public int label_center_at_x( int x, String label )
    {
        return label_center_at_x(x, text_width(label));
    }


    static public int direction_up( int value )
    {
        return -1 * value;
    }

    static public int direction_down( int value )
    {
        return value;
    }

    static public int direction_left( int value )
    {
        return -1 * value;
    }

    static public int direction_right( int value )
    {
        return value;
    }

    static public int text_width( String text )
    {
        int max_width = 0;

        String[] lines = text.split("\n");

        for ( String line: lines )
        {
            if ( line.length() > max_width )
            {
                max_width = line.length();
            }
        }

        return max_width;
    }

    static public int text_lines( String text )
    {
        if ( true == text.isEmpty() )
        {
            return 0;
        }
        else
        {
            return text.split("\n").length;
        }
    }

    static public Point label_position(Point place_center, int place_radius, String label_string, Side label_side )
    {
        Point location      = new Point();
        int label_width     = text_width( label_string );
        int label_lines     = text_lines( label_string );

        if ( label_side.is_equal( Side.top_left     ) ||
             label_side.is_equal( Side.middle_left  ) ||
             label_side.is_equal( Side.bottom_left  ) ){
            location.x = place_center.x - ( place_radius + ( label_width * units_letter_width ) );
        }else if ( label_side.is_equal( Side.top_right    ) ||
                   label_side.is_equal( Side.middle_right ) ||
                   label_side.is_equal( Side.bottom_right ) ){
            location.x = place_center.x + ( place_radius );
        }else if ( label_side.is_equal( Side.top_center    ) ||
                   label_side.is_equal( Side.bottom_center ) ){
            location.x = label_center_at_x( place_center.x, label_width );
        }

        if ( label_side.is_equal( Side.top_left   ) ||
             label_side.is_equal( Side.top_center ) ||
             label_side.is_equal( Side.top_right  ) ){
            location.y = place_center.y - ( place_radius + (label_lines * units_letter_hight) );
        }else if ( label_side.is_equal( Side.middle_left   ) ||
                   label_side.is_equal( Side.middle_right  ) ){
            location.y = place_center.y - ( (label_lines * units_letter_hight) / 2 );
        }else if ( label_side.is_equal( Side.bottom_left   ) ||
                   label_side.is_equal( Side.bottom_center ) ||
                   label_side.is_equal( Side.bottom_right  ) ){
            location.y = place_center.y + ( place_radius );
        }

        return location;
    }

    /*
    public Point labels_interference_resolve( Side label_move_side, int lable_move_length, Side label_stay_side, int lable_stay_length )
    {
        if (  )
    }
    */

    static public boolean is_limited_up( Side side )
    {
        return side.is_equal(Side.bottom_right ) ||
               side.is_equal(Side.bottom_center) ||
               side.is_equal(Side.bottom_left  );
    }

    static public void move_by( Point point_to_move, Point offset )
    {
        if ( ( null != point_to_move ) && ( null != offset ) ) {
            point_to_move.x += offset.x;
            point_to_move.y += offset.y;
        }
    }


    static int label_align_by_x( int x, String label, Side alignment )
    {
        if ( false == label.isEmpty() )
        {
            if ( alignment.is_equal(Side.bottom_right ) ||
                 alignment.is_equal(Side.middle_right ) ||
                 alignment.is_equal(Side.top_right    ) )
            {
                return label_right_at_x(x, label);
            }
            else if ( alignment.is_equal(Side.bottom_center ) ||
                      alignment.is_equal(Side.top_center) )
            {
                return label_center_at_x( x,label);
            }
            else
            {
                return x;
            }
        }
        else
        {
            return x;
        }
    }

    public static void add_to_cumulative_condition( StringBuffer cumulative_results_string, String addition )
    {
        add_to_cumulative_expression_with_operator( cumulative_results_string, addition, STR_AND );
    }

    public static void add_to_cumulative_expression_with_operator(StringBuffer cumulative_results_string, CharSequence addition, String operator )
    {
        if ( 0 != addition.length() ) {
            if ( 0 != cumulative_results_string.length() ) {
                cumulative_results_string.append( " " );
                cumulative_results_string.append( operator );
                cumulative_results_string.append( "\n" );
            }

            cumulative_results_string.append(addition);
        }
    }

    public static String add_brackets( String expression )
    {
        return "( " + expression + " )";
    }

    public static String binary_expression_enclosed(String left, String operator, String right )
    {
        return add_brackets( binary_expression( left, operator, right ) );
    }

    public static String binary_expression(String left, String operator, String right )
    {
        return left + " " + operator + " " + right;
    }

    public static String make_uppaal_condition_is_true_variable( String uppaal_variable )
    {
        return binary_expression_enclosed( uppaal_variable, UppaalBuilder.STR_GREATER_THAN, "0"  );
    }

    public static String uppaal_variable_write( String variable_id, String value )
    {
        return String.format( "concurrent_write( %s, %s )", variable_id, value );
    }

    public static String uppaal_variable_set_range( String variable_id, String min, String max )
    {
        return String.format( "concurrent_set_range( %s, %s, %s )", variable_id, min, max );
    }

    public static String uppaal_variable_write( int variable_id, String value )
    {
        return uppaal_variable_write( String.valueOf( variable_id), value );
    }

    public static String uppaal_variable_write( String variable_id, int value )
    {
        return uppaal_variable_write( variable_id, String.valueOf( value ) );
    }

    public static String uppaal_variable_write( int variable_id, int value )
    {
        return uppaal_variable_write( String.valueOf( variable_id), String.valueOf( value ) );
    }

    public static String uppaal_variable_set_range( int variable_id, int min, int max )
    {
        return uppaal_variable_set_range( String.valueOf( variable_id), String.valueOf( min ), String.valueOf( max ) );
    }

    public static String uppaal_variable_read( String variable_id )
    {
        return String.format( "concurrent_read( %s )", variable_id );
    }

    public static String uppaal_variable_read( int variable_id )
    {
        return uppaal_variable_read( String.valueOf( variable_id ) );
    }

}
