package verification;

import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

/**
 * Created by alexds9 on 07/06/17.
 */
public class ConcurrentCommandCollector {
    public  String                  locations_suffix = "";
    public  List<ConcurrentCommand> commands;
    private StringBuffer            initial_assignments;
    public  List<String>            nondeterministic_assignments;

    public ConcurrentCommandCollector()
    {
        this.commands                       = new LinkedList<>();
        this.locations_suffix               = "";
        this.initial_assignments            = new StringBuffer();
        this.nondeterministic_assignments   = null;
    }

    public ConcurrentCommandCollector( String locations_suffix )
    {
        this.commands                       = new LinkedList<>();
        this.locations_suffix               = locations_suffix;
        this.initial_assignments            = new StringBuffer();
        this.nondeterministic_assignments   = null;
    }

    public void add_to_initial_assignments( CharSequence additional_assignments )
    {
        UppaalBuilder.add_to_cumulative_assignment( this.initial_assignments, additional_assignments );
    }

    public void add_request( ConcurrentCommand command )
    {
        this.commands.add(command);
    }

    public void generate_requests( UppaalPTA plp_uppaal,
                                   String id_source, String id_target,
                                   Point reference, boolean is_vertical, String process_id,
                                   List<String> nondeterministic_assignments,
                                   List<Integer> additional_write_variable_ids,
                                   String first_transition_guard,
                                   String first_transition_assignment,
                                   String first_transition_probability,
                                   String last_transition_assignment,
                                   String last_transition_synchronisation)
    {
        final int structer_height_level_2;
        final int structer_height_level_1;
        final int structer_height_level_15;
        final int structer_height_label_below;
        final int structer_width;
        final int structer_width_half;
        final int structer_width_quarter;
        final int structer_padding_sides;
        final int structer_padding_up;
        final int structer_padding_write;

        Map<String, LocationTransitionPlace> places;
        if ( true == is_vertical )
        {
            structer_height_level_2       = 10;
            structer_height_level_1       = 6;
            structer_height_label_below   = 1;
            structer_width                = 12;
            structer_width_half           = structer_width / 2;
            structer_width_quarter        = structer_width_half / 2;
            structer_padding_sides        = 1;
            structer_padding_up           = 2;
            structer_padding_write        = 4;

            Map<String, LocationTransitionPlace> places_vertical = new HashMap<>();
            places_vertical.put( "concurrent_waiting", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_left( UppaalBuilder.squares_length(structer_width_half ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_1))),
                    UppaalBuilder.Side.middle_left ));
            places_vertical.put( "concurrent_got_lock", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_left( UppaalBuilder.squares_length(structer_width_half ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_2))),
                    UppaalBuilder.Side.middle_left ));
            places_vertical.put( "concurrent_unlock", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length(structer_width_half ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_2))),
                    UppaalBuilder.Side.middle_right ));
            places_vertical.put( "concurrent_continue", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length(structer_width_half ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_1))),
                    UppaalBuilder.Side.middle_right ));
            places_vertical.put( id_source + "concurrent_waiting", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_left(UppaalBuilder.squares_length(structer_width_half)),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_1 - structer_height_label_below))),
                    UppaalBuilder.Side.bottom_right ));
            places_vertical.put( "concurrent_waiting" + "concurrent_got_lock", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_left(UppaalBuilder.squares_length(structer_width_half + structer_padding_sides)),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_2 - structer_height_label_below))),
                    UppaalBuilder.Side.bottom_right ));

            places_vertical.put( "from" + "concurrent_got_lock", new LocationTransitionPlace(
                    new Point( UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_width_quarter)),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_2 + structer_padding_up))),
                    UppaalBuilder.Side.bottom_right ));

            places_vertical.put( "concurrent_unlock" + "concurrent_continue", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_width_half + structer_padding_sides)),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_2 - structer_height_label_below))),
                    UppaalBuilder.Side.bottom_left ));

            places_vertical.put( "concurrent_continue" + id_target, new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_width_half)),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_1 - structer_height_label_below))),
                    UppaalBuilder.Side.bottom_left ));

            places = places_vertical;
        }
        else
        {
            structer_height_level_2       = 14;
            structer_height_level_1       = 8;
            structer_height_level_15      = structer_height_level_2 - (structer_height_level_2 - structer_height_level_1) / 2;
            structer_height_label_below   = 1;
            structer_width                = 6;
            structer_width_half           = structer_width / 2;
            structer_width_quarter        = structer_width_half / 2;
            structer_padding_sides        = 1;
            structer_padding_up           = 2;
            structer_padding_write        = 6;

            Map<String, LocationTransitionPlace> places_horizontal = new HashMap<>();
            places_horizontal.put( "concurrent_waiting", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_height_level_1)),
                            UppaalBuilder.direction_up( UppaalBuilder.squares_length(structer_width_half ) )),
                    UppaalBuilder.Side.top_center ));
            places_horizontal.put( "concurrent_got_lock", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_height_level_2)),
                            UppaalBuilder.direction_up( UppaalBuilder.squares_length(structer_width_half ) )),
                    UppaalBuilder.Side.top_center ));
            places_horizontal.put( "concurrent_unlock", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length(structer_height_level_2) ),
                            UppaalBuilder.direction_down( UppaalBuilder.squares_length(structer_width_half ) )),
                    UppaalBuilder.Side.bottom_center ));
            places_horizontal.put( "concurrent_continue", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_height_level_1) ),
                            UppaalBuilder.direction_down( UppaalBuilder.squares_length(structer_width_half ) )),
                    UppaalBuilder.Side.bottom_center ));

            places_horizontal.put( id_source + "concurrent_waiting", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_height_level_1) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_width_half - structer_height_label_below ))),
                    UppaalBuilder.Side.top_right ));

            places_horizontal.put( "concurrent_waiting" + "concurrent_got_lock", new LocationTransitionPlace(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_height_level_15) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_width_half + structer_padding_up))),
                    UppaalBuilder.Side.top_center ));

            places_horizontal.put( "from" + "concurrent_got_lock", new LocationTransitionPlace(
                    new Point(  UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_height_level_2 + structer_padding_sides)),
                                UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_width_half - structer_height_label_below))),
                    UppaalBuilder.Side.bottom_left ));

            places_horizontal.put( "concurrent_unlock" + "concurrent_continue", new LocationTransitionPlace(
                    new Point( UppaalBuilder.direction_right(UppaalBuilder.squares_length( structer_height_level_15 )),
                               UppaalBuilder.direction_down(UppaalBuilder.squares_length(structer_width_half + structer_padding_up)) ),
                    UppaalBuilder.Side.bottom_center ));

            places_horizontal.put( "concurrent_continue" + id_target, new LocationTransitionPlace(
                    new Point( UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_height_level_1 - structer_height_label_below)),
                            UppaalBuilder.direction_down(UppaalBuilder.squares_length(structer_width_half)) ),
                    UppaalBuilder.Side.bottom_right ));


            places = places_horizontal;
        }

        UppaalSubGraphContainer generated_sub_graph = new UppaalSubGraphContainer();

        UppaalLocation loc_waiting = new UppaalLocation( "concurrent_waiting", places.get("concurrent_waiting").labels_side,
                places.get("concurrent_waiting").place,
                "", UppaalBuilder.Side.none,
                "", UppaalBuilder.Side.none,
                false);
        generated_sub_graph.locations.add( loc_waiting  );

        UppaalLocation loc_got_lock = new UppaalLocation( "concurrent_got_lock", places.get("concurrent_got_lock").labels_side,
                places.get("concurrent_got_lock").place,
                "", UppaalBuilder.Side.none,
                "", UppaalBuilder.Side.none,
                true);
        generated_sub_graph.locations.add( loc_got_lock );


        Point got_lock_next_location = places.get( "concurrent_unlock" ).place;

        List<Integer> write_variable_ids = new LinkedList<Integer>();

        if ( null != additional_write_variable_ids )
        {
            write_variable_ids.addAll( additional_write_variable_ids );
        }

        for ( ConcurrentCommand command : this.commands )
        {
            if (ConcurrentCommand.CommandType.type_write == command.type) {
                write_variable_ids.add( command.global_variable_id_to_watch );
            }
        }


        int writes_amount   = 0;
        for ( Integer write_variable_id : write_variable_ids )
        {

                LocationTransitionPlace current_place;
                if ( true == is_vertical )
                {
                    current_place = new LocationTransitionPlace(
                            new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_width_quarter)),
                                    UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_2 + (writes_amount * structer_padding_write)))),
                            UppaalBuilder.Side.bottom_right);
                }
                else
                {
                    current_place = new LocationTransitionPlace(
                            new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_height_level_2 + (writes_amount * structer_padding_write))),
                                    UppaalBuilder.direction_down(UppaalBuilder.squares_length(structer_width_quarter)) ),
                            UppaalBuilder.Side.bottom_center);
                }

                UppaalLocation loc_notify = new UppaalLocation("concurrent_notify_" + String.valueOf(writes_amount), current_place.labels_side,
                        current_place.place,
                        "", UppaalBuilder.Side.none,
                        "", UppaalBuilder.Side.none,
                        true);
                generated_sub_graph.locations.add( loc_notify  );

                if ( 0 == writes_amount )
                {
                    got_lock_next_location = current_place.place;
                }

                writes_amount++;
        }





        UppaalLocation loc_unlock = new UppaalLocation( "concurrent_unlock", places.get("concurrent_unlock").labels_side,
                places.get("concurrent_unlock").place,
                "", UppaalBuilder.Side.none,
                "", UppaalBuilder.Side.none,
                true);
        generated_sub_graph.locations.add( loc_unlock   );

        UppaalLocation loc_continue = new UppaalLocation( "concurrent_continue", places.get("concurrent_continue").labels_side,
                places.get("concurrent_continue").place,
                "", UppaalBuilder.Side.none,
                "", UppaalBuilder.Side.none,
                true);
        generated_sub_graph.locations.add( loc_continue );


        StringBuffer to_waiting_update = new StringBuffer( "concurrent_signal_access( " + process_id + ", true )" );
        for ( ConcurrentCommand command : this.commands ) {
            if (ConcurrentCommand.CommandType.type_watch_add == command.type) {
                String request_variable = command.variable_with_request_data;//plp_uppaal.local_concurrent_request_add();

                String message = "";
                if ( ( null  != command.message           ) &&
                     ( false == command.message.isEmpty() ) )
                {
                    message = command.message;
                }

                to_waiting_update.append( String.format(",\n" +
                        message + "\n" +
                        request_variable + ".bound_lower        = %s,\n" +
                        request_variable + ".bound_upper        = %s,\n" +
                        request_variable + ".is_single_range    = %s,\n" +
                        request_variable + ".variable_id        = %d",
                        command.request_bound_lower, command.request_bound_upper,
                        UppaalBuilder.uppall_boolean(command.request_is_single_range),
                        command.global_variable_id_to_watch) );
            }
        }

        if ( null == first_transition_guard ){
            first_transition_guard = "";
        }
        if ( null == first_transition_assignment ){
            first_transition_assignment = "";
        }
        if ( null == first_transition_probability ){
            first_transition_probability = "";
        }

        UppaalBuilder.add_to_cumulative_assignment( to_waiting_update, this.initial_assignments );
        UppaalBuilder.add_to_cumulative_assignment( to_waiting_update, first_transition_assignment );
        UppaalTransition to_waiting = new UppaalTransition( id_source, "concurrent_waiting",
                places.get(id_source + "concurrent_waiting").place, places.get(id_source + "concurrent_waiting").labels_side,
                first_transition_guard, "concurrent_can_run!", to_waiting_update.toString(), first_transition_probability,null );
        generated_sub_graph.transitions.add( to_waiting );

        UppaalTransition to_got_lock = new UppaalTransition( "concurrent_waiting", "concurrent_got_lock",
                places.get("concurrent_waiting" + "concurrent_got_lock").place, places.get("concurrent_waiting" + "concurrent_got_lock").labels_side,
                "", "concurrent_lock_process[" + process_id + "]?", "", "", null );
        generated_sub_graph.transitions.add( to_got_lock    );

        String  got_lock_next           = "concurrent_unlock";
        int     write_command_current   = 0;
        for ( Integer write_variable_id : write_variable_ids )
        {
            List<Point> nails               = null;
            String      location_previous   = "concurrent_notify_" + String.valueOf(write_command_current);
            String      location_next       = "concurrent_notify_" + String.valueOf(write_command_current + 1);
            LocationTransitionPlace current_place;



            if ( writes_amount == write_command_current + 1 ) {
                got_lock_next = "concurrent_notify_0";
                location_next = "concurrent_unlock";

                nails = new LinkedList<>();

                if ( 1 < writes_amount ) {
                    if (true == is_vertical) {
                        nails.add(new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_width_half)),
                                UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_2 + (write_command_current * structer_padding_write)))));
                    } else {
                        nails.add(new Point(UppaalBuilder.direction_down(UppaalBuilder.squares_length(structer_height_level_2 + (write_command_current * structer_padding_write))),
                                UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_width_half)) ));
                    }
                }

            }


            if ( true == is_vertical )
            {
                current_place = new LocationTransitionPlace(new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_width_quarter + structer_padding_sides)),
                        UppaalBuilder.direction_up(UppaalBuilder.squares_length(structer_height_level_2 + structer_padding_write / 2 + (write_command_current * structer_padding_write)))),
                        UppaalBuilder.Side.bottom_left);
            }
            else
            {
                current_place = new LocationTransitionPlace(
                        new Point(UppaalBuilder.direction_down(UppaalBuilder.squares_length(structer_height_level_2 + structer_padding_write / 2 + (write_command_current * structer_padding_write))),
                                UppaalBuilder.direction_right(UppaalBuilder.squares_length(structer_width_quarter)) ),
                        UppaalBuilder.Side.bottom_center);
            }



            UppaalTransition to_notify = new UppaalTransition( location_previous, location_next,
                    current_place.place, current_place.labels_side,
                    "", "concurrent_notify[" + write_variable_id.intValue() + "]!", "", "", nails );
            generated_sub_graph.transitions.add(to_notify);

            write_command_current++;
        }

        StringBuffer to_unlock_update = new StringBuffer();
        for ( ConcurrentCommand command : this.commands ) {
            String message = "";
            if ( ( null  != command.message           ) &&
                    ( false == command.message.isEmpty() ) )
            {
                message = command.message + "\n";
            }

            if (ConcurrentCommand.CommandType.type_read == command.type) {
                UppaalBuilder.add_to_cumulative_assignment( to_unlock_update, message + command.general_io_variable + " = concurrent_read( " + command.global_variable_id_to_watch + " )" );
            } else if (ConcurrentCommand.CommandType.type_watch_add == command.type) {
                UppaalBuilder.add_to_cumulative_assignment( to_unlock_update,  message + command.variable_for_request_id + " = concurrent_request_add( " + command.global_variable_id_to_watch + ", " + command.variable_with_request_data + " )" );
            } else if (ConcurrentCommand.CommandType.type_watch_remove == command.type) {
                UppaalBuilder.add_to_cumulative_assignment( to_unlock_update,  message + command.general_io_variable + " = concurrent_request_remove_request( " + command.variable_for_request_id + " )" );
            } else if (ConcurrentCommand.CommandType.type_write == command.type) {
                UppaalBuilder.add_to_cumulative_assignment( to_unlock_update,  message + "concurrent_write( " + command.global_variable_id_to_watch + ", " + command.general_io_variable + " )" );
            }
        }


        if ( ( null != nondeterministic_assignments    ) &&
             ( 0 < nondeterministic_assignments.size() ) )
        {
            List<UppaalTransition> transitions = VerificationGenerator.generate_nondeterministic_transitions(
                "concurrent_got_lock", places.get("concurrent_got_lock").place,
                got_lock_next, got_lock_next_location,
                to_unlock_update.toString(),
                nondeterministic_assignments );

            generated_sub_graph.transitions.addAll( transitions );
        }
        else
        {
            UppaalTransition from_got_lock = new UppaalTransition( "concurrent_got_lock", got_lock_next,
                    places.get("from" + "concurrent_got_lock").place, places.get("from" + "concurrent_got_lock").labels_side,
                    "", "", to_unlock_update.toString(), "", null );
            generated_sub_graph.transitions.add( from_got_lock  );
        }

        UppaalTransition to_continue = new UppaalTransition( "concurrent_unlock", "concurrent_continue",
                places.get("concurrent_unlock" + "concurrent_continue").place, places.get("concurrent_unlock" + "concurrent_continue").labels_side,
                "", "concurrent_lock_release!", "concurrent_signal_access( " + process_id + ", false )", "", null );
        generated_sub_graph.transitions.add( to_continue    );

        if ( null == last_transition_assignment ){
            last_transition_assignment = "";
        }
        if ( null == last_transition_synchronisation ){
            last_transition_synchronisation = "";
        }
        UppaalTransition to_end = new UppaalTransition( "concurrent_continue", id_target,
                places.get("concurrent_continue" + id_target).place, places.get("concurrent_continue" + id_target).labels_side,
                "", last_transition_synchronisation, last_transition_assignment, "", null );
        generated_sub_graph.transitions.add( to_end         );


        generated_sub_graph.add_sub_graph( plp_uppaal, reference, this.locations_suffix );

    }

    private class LocationTransitionPlace{
        Point               place;
        UppaalBuilder.Side  labels_side;

        public LocationTransitionPlace( Point place, UppaalBuilder.Side labels_side )
        {
            this.place          = place;
            this.labels_side    = labels_side;
        }
    }

}
