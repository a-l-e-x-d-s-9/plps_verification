package verification;

import conditions.Condition;

import java.awt.Point;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by alexds9 on 15/06/17.
 */
public class UppaalControlNode {
    private ControlNodeInterface        control_node;
    private UppaalPTA                   control_graph_pta;
    private ControlGraph                control_graph;
    private PLPCatalog                  plp_catalog;
    private XMLtoUppaalConverter        xml_to_uppaal_converter;
    private VerificationVariableManager variable_manager;

    public UppaalControlNode( ControlNodeInterface control_node, ControlGraph control_graph,
                              PLPCatalog plp_catalog, XMLtoUppaalConverter xml_to_uppaal_converter,
                              VerificationVariableManager variable_manager )
    {
        this.variable_manager           = variable_manager;
        this.control_node               = control_node;
        this.control_graph_pta          = new UppaalPTA( this.variable_manager );
        this.control_graph              = control_graph;
        this.plp_catalog                = plp_catalog;
        this.xml_to_uppaal_converter    = xml_to_uppaal_converter;
    }

    public String uppaal_channel_notify_me()
    {
        return "control_node_notify[node_id]";
    }

    public static String uppaal_channel_notify_by_id( int node_id )
    {
        return String.format( "control_node_notify[%d]", node_id );
    }

    public String uppaal_variable_can_run_by_id( int cab_run_id )
    {
        return String.format( "control_node_can_run[%d]", cab_run_id );
    }


    private void create_transitions_for_loop( UppaalSubGraphContainer sub_graph_container, String done_name,
                                              Point done_place, StringBuffer node_preconditions,
                                              StringBuffer node_condition_can_not_start )
    {



        if ( 0 != node_preconditions.length() ) {
            List<Point> nails = new LinkedList<>();

            nails.add(new Point(UppaalBuilder.direction_left(UppaalBuilder.squares_length(6)) + (int) done_place.getX(), UppaalBuilder.direction_up(UppaalBuilder.squares_length(3)) + (int) done_place.getY()));

            UppaalTransition to_start = new UppaalTransition(done_name, "start",
                    nails.get(0),
                    UppaalBuilder.Side.bottom_right, node_preconditions.toString(), null,
                    "", "", nails);
            sub_graph_container.transitions.add(to_start);
        }

        if ( 0 != node_condition_can_not_start.length() ) {
            List<Point> nails = new LinkedList<>();

            nails.add(new Point(UppaalBuilder.direction_left(UppaalBuilder.squares_length(12)) + (int) done_place.getX(), UppaalBuilder.direction_up(UppaalBuilder.squares_length(5)) + (int) done_place.getY()));


            UppaalTransition to_init = new UppaalTransition(done_name, "init_node",
                    nails.get(0),
                    UppaalBuilder.Side.bottom_right, node_condition_can_not_start.toString(), null,
                    "", "", nails);
            sub_graph_container.transitions.add(to_init);
        }
    }

    public UppaalPTA generate() throws VerificationException
    {
        UppaalSubGraphContainer sub_graph_container             = new UppaalSubGraphContainer();
        StringBuffer            node_preconditions              = new StringBuffer();
        StringBuffer            node_condition_can_not_start    = new StringBuffer();
        String                  preconditions_operator          = null;
        String                  complementary_operator          = null;
        StringBuffer            nullify_all_predecessors            = new StringBuffer();

        this.control_graph_pta.set_name( control_node.get_node_name() );
        this.control_graph_pta.parameters.append( "int node_id, int concurrent_process_id" );

        if ( ControlNodeInterface.StartPolicyType.all_predecessor_done == this.control_node.get_start_policy() )
        {
            preconditions_operator = UppaalBuilder.STR_AND;
            complementary_operator = UppaalBuilder.STR_OR;
        }
        else if ( ControlNodeInterface.StartPolicyType.any_predecessor_done == this.control_node.get_start_policy() )
        {
            preconditions_operator = UppaalBuilder.STR_OR;
            complementary_operator = UppaalBuilder.STR_AND;
        }

        List<String> predecessors = this.control_node.get_predecessors();

        if ( null != predecessors ) {
            for ( String predecessor : predecessors ) {
                int transition_id = this.control_graph.get_transition_id( predecessor, this.control_node.get_node_name() );
                String variable_for_predecessor = this.uppaal_variable_can_run_by_id( transition_id );
                String expression_positive = UppaalBuilder.make_uppaal_condition_is_true_variable(  variable_for_predecessor );
                String expression_negative = UppaalBuilder.make_uppaal_condition_is_false_variable( variable_for_predecessor );

                UppaalBuilder.add_to_cumulative_expression_with_operator( node_preconditions          , expression_positive, preconditions_operator);
                UppaalBuilder.add_to_cumulative_expression_with_operator( node_condition_can_not_start, expression_negative, complementary_operator);

                String nullify_predecessor = UppaalBuilder.binary_expression_enclosed( variable_for_predecessor, UppaalBuilder.STR_ASSIGNMENT, "0"  );

                UppaalBuilder.add_to_cumulative_expression_with_operator( nullify_all_predecessors, nullify_predecessor, UppaalBuilder.STR_COMMA );
            }
        }

        final int level_squares         = 4;
        final int level_init            = 0;
        final int level_start           = 1;
        final int level_ready           = 2;
        final double level_tran_init_to_start  = level_init + 0.5;
        final double level_tran_start_to_ready = level_start + 0.5;

        this.control_graph_pta.append_declarations( "clock local_time;\n" );


        // *** LOCATION


        UppaalLocation loc_init_node = new UppaalLocation( "init_node", UppaalBuilder.Side.middle_left,
                new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_squares * level_init ))),
                "", UppaalBuilder.Side.none,
                "", UppaalBuilder.Side.none,
                false);
        sub_graph_container.locations.add( loc_init_node );


        // *** LOCATION



        UppaalLocation loc_start = new UppaalLocation( "start", UppaalBuilder.Side.middle_left,
                new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_squares * level_start ))),
                "", UppaalBuilder.Side.none,
                "", UppaalBuilder.Side.none,
                true);
        sub_graph_container.locations.add( loc_start );

        sub_graph_container.init_location = "init_node";
        this.control_graph_pta.set_init_location_id( loc_start.location_name );


        // *** TRANSITION

        UppaalTransition to_start = new UppaalTransition( "init_node", "start",
                new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_squares * level_tran_init_to_start ))),
                UppaalBuilder.Side.bottom_right, node_preconditions.toString(), UppaalSystem.uppaal_sync_signal_receive( uppaal_channel_notify_me() ),
                "", "",null );
        sub_graph_container.transitions.add( to_start );

        // *** TRANSITION

        boolean is_location_ready_urgent    = false;
        String  ready_invariant             = "";

        if (ControlNodeInterface.ControlNodeType.node_probability == this.control_node.get_node_kind()) {
            ready_invariant = UppaalBuilder.binary_expression_enclosed( "local_time", UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf(((ControlNodeProbability)this.control_node).wait_time) );
        } else if (ControlNodeInterface.ControlNodeType.node_concurrent == this.control_node.get_node_kind()) {
            is_location_ready_urgent = true;
        } else if (ControlNodeInterface.ControlNodeType.node_sequential == this.control_node.get_node_kind()) {
            is_location_ready_urgent = true;
        } else if (ControlNodeInterface.ControlNodeType.node_condition == this.control_node.get_node_kind()) {
            ready_invariant = UppaalBuilder.binary_expression_enclosed( "local_time", UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf(((ControlNodeCondition)this.control_node).wait_time) );
        }

        UppaalLocation loc_ready = new UppaalLocation( "ready", UppaalBuilder.Side.middle_left,
                new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_squares * level_ready ))),
                "", UppaalBuilder.Side.none,
                ready_invariant, UppaalBuilder.Side.bottom_right,
                is_location_ready_urgent);
        String locations_id_ready = "";
        if (ControlNodeInterface.ControlNodeType.node_concurrent == this.control_node.get_node_kind()) {
            locations_id_ready      = control_graph_pta.get_new_location_id();
            loc_ready.location_id   = locations_id_ready;
         }
        sub_graph_container.locations.add( loc_ready );

        UppaalBuilder.add_to_cumulative_expression_with_operator( nullify_all_predecessors, UppaalBuilder.binary_expression_enclosed( "local_time", UppaalBuilder.STR_ASSIGNMENT, "0"  ), UppaalBuilder.STR_COMMA );

        UppaalTransition to_ready = new UppaalTransition( "start", "ready",
                new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_squares * level_tran_start_to_ready ))),
                UppaalBuilder.Side.bottom_right, "", "",
                 nullify_all_predecessors.toString(), "",null );
        sub_graph_container.transitions.add( to_ready );

        if (ControlNodeInterface.ControlNodeType.node_probability == this.control_node.get_node_kind()) {
            ControlNodeProbability node_probability = (ControlNodeProbability) this.control_node;

            int node_index          = 0;
            int level_base_current  = level_squares * ( level_ready + 1 );

            UppaalBuilder.Side transition_label_side = UppaalBuilder.Side.middle_right;

            final int level_branchpoint = 0;

            if ( false == node_probability.probability_for_successor_nodes.isEmpty() ) {
                String location_branchpoint_id = this.control_graph_pta.get_new_location_id();
                UppaalBranchpoint branchpoint_main = new UppaalBranchpoint(location_branchpoint_id,
                        new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(0)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_branchpoint)))));
                sub_graph_container.branchpoints.add(branchpoint_main);

                UppaalTransition to_branchpoint = new UppaalTransition("ready", location_branchpoint_id,
                        new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(0)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_branchpoint) - (0.5 * level_squares)))),
                        transition_label_side,
                        UppaalBuilder.binary_expression_enclosed("local_time", UppaalBuilder.STR_EQUAL, String.valueOf(node_probability.wait_time)),
                        "",
                        "", "", null);
                sub_graph_container.transitions.add(to_branchpoint);

                final int x_offset_squares = 30;
                final double level_nail = 0.5;
                final double level_chosen_path = 1.5;
                final double level_done = 2.5;
                final double level_concurrent = 1.5;

                for (ControlProbabilityForSuccessor probability_for_successor_node : node_probability.probability_for_successor_nodes) {

                    int current_x_offset = x_offset_squares * node_index;

                    String location_label_chosen_path = String.format("path_%d", node_index);
                    String location_label_done = String.format("done_%d", node_index);

                    UppaalLocation loc_chosen_path = new UppaalLocation(location_label_chosen_path, UppaalBuilder.Side.middle_left,
                            new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(current_x_offset)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_chosen_path)))),
                            "", UppaalBuilder.Side.none,
                            "", UppaalBuilder.Side.none,
                            true);
                    String locations_id_current_choose_path = control_graph_pta.get_new_location_id();
                    loc_chosen_path.location_id = locations_id_current_choose_path;
                    sub_graph_container.locations.add(loc_chosen_path);


                    List<Integer> update_variables_id = new LinkedList<>();
                    List<String> update_values = new LinkedList<>();

                    try {
                        //this.xml_to_uppaal_converter.convert_xml_conditions_to_uppaal_assignment( -1, update_for_successor, probability_for_successor_node.updates );
                        this.xml_to_uppaal_converter.convert_xml_condition_for_assignments_to_list_of_variable_and_values(-1, probability_for_successor_node.updates, update_variables_id, update_values);
                    } catch (VerificationException exception) {
                        throw new VerificationException("Control node \"" + this.control_node.get_node_name() + "\", invalid assignment.\n" +
                                exception.get_message() + "\n" +
                                "Note: New variables should be defined in configuration file.");
                    }


                    String successor_transition_notify = "";
                    String successor_synchronization = "";

                    if (false == probability_for_successor_node.node_name.isEmpty()) {
                        int transition_id_to_next_node = this.control_graph.get_transition_id(this.control_node.get_node_name(), probability_for_successor_node.node_name);
                        String variable_to_next_node = this.uppaal_variable_can_run_by_id(transition_id_to_next_node);
                        successor_transition_notify = UppaalBuilder.binary_expression(variable_to_next_node, UppaalBuilder.STR_ASSIGNMENT, "1");

                        int next_node_id = this.control_graph.node_name_get_id(probability_for_successor_node.node_name);
                        successor_synchronization = UppaalSystem.uppaal_sync_signal_send(uppaal_channel_notify_by_id(next_node_id));
                    }

                    StringBuffer update_for_successor = new StringBuffer();
                    UppaalBuilder.add_to_cumulative_assignment(update_for_successor,
                            successor_transition_notify);


                    if (0 < update_variables_id.size()) {
                        ConcurrentCommandCollector concurrent_commands_collector = new ConcurrentCommandCollector(String.format("_%d", node_index));
                        for (int i = 0; i < update_variables_id.size(); i++) {
                            concurrent_commands_collector.commands.add((new ConcurrentCommand()).make_write(update_variables_id.get(i), update_values.get(i)));
                        }

                        concurrent_commands_collector.generate_requests(this.control_graph_pta,
                                location_branchpoint_id, locations_id_current_choose_path,
                                new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(current_x_offset)),
                                        UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_concurrent)))),
                                false,
                                "concurrent_process_id",
                                null,
                                null,
                                null,
                                null,
                                String.valueOf(probability_for_successor_node.probability),
                                update_for_successor.toString(),
                                null);
                    } else {
                        List<Point> nails = new LinkedList<>();
                        nails.add(new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(current_x_offset)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_nail)))));


                        UppaalTransition to_chosen_path = new UppaalTransition(location_branchpoint_id, location_label_chosen_path,
                                new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(current_x_offset)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_nail)))),
                                transition_label_side,
                                "",
                                "",
                                update_for_successor.toString(),
                                String.valueOf(probability_for_successor_node.probability), nails);
                        sub_graph_container.transitions.add(to_chosen_path);
                    }

                    Point place_loc_done = new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(current_x_offset)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_done))));
                    UppaalLocation loc_done = new UppaalLocation(location_label_done, UppaalBuilder.Side.middle_left,
                            place_loc_done,
                            "", UppaalBuilder.Side.none,
                            "", UppaalBuilder.Side.none,
                            0 != node_preconditions.length());
                    sub_graph_container.locations.add(loc_done);

                    create_transitions_for_loop(sub_graph_container, location_label_done,
                            place_loc_done, node_preconditions, node_condition_can_not_start);

                    int next_node_id = this.control_graph.node_name_get_id(probability_for_successor_node.node_name);

                    UppaalTransition to_done = new UppaalTransition(location_label_chosen_path, location_label_done,
                            new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(current_x_offset)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_done) - (0.5 * level_squares)))),
                            transition_label_side,
                            "",
                            successor_synchronization,
                            "", "", null);
                    sub_graph_container.transitions.add(to_done);

                    node_index++;
                }
            }
        } else if (ControlNodeInterface.ControlNodeType.node_concurrent == this.control_node.get_node_kind()) {
            ControlNodeConcurrent node_concurrent = (ControlNodeConcurrent) this.control_node;

            int node_index          = 0;
            int level_base_current  = level_squares * ( level_ready + 1 );

            UppaalBuilder.Side transition_label_side = UppaalBuilder.Side.middle_right;


            UppaalLocation loc_all_can_run = new UppaalLocation( "all_can_run", UppaalBuilder.Side.middle_left,
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current ))),
                    "", UppaalBuilder.Side.none,
                    UppaalBuilder.binary_expression_enclosed( "local_time", UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf(node_concurrent.wait_time) ), UppaalBuilder.Side.bottom_right,
                    false);
            String locations_id_all_can_run = control_graph_pta.get_new_location_id();
            loc_all_can_run.location_id     = locations_id_all_can_run;
            sub_graph_container.locations.add( loc_all_can_run );


            List<Integer>   update_variables_id = new LinkedList<>();
            List<String>    update_values       = new LinkedList<>();


            try {
                //this.xml_to_uppaal_converter.convert_xml_conditions_to_uppaal_assignment( -1, update_string_buffer, node_concurrent.updates );
                this.xml_to_uppaal_converter.convert_xml_condition_for_assignments_to_list_of_variable_and_values( -1, node_concurrent.updates, update_variables_id, update_values );
            } catch (VerificationException exception) {
                throw new VerificationException("Control node \"" + this.control_node.get_node_name() + "\", invalid assignment.\n" +
                        exception.get_message() + "\n" +
                        "Note: New variables should be defined in configuration file." );
            }


            StringBuffer update_for_successor = new StringBuffer();

            for (String run_node : node_concurrent.run_nodes) {
                int     transition_id_to_next_node  = this.control_graph.get_transition_id( this.control_node.get_node_name(), run_node );
                String  variable_to_next_node       = this.uppaal_variable_can_run_by_id( transition_id_to_next_node );

                UppaalBuilder.add_to_cumulative_assignment( update_for_successor,
                        UppaalBuilder.binary_expression( variable_to_next_node, UppaalBuilder.STR_ASSIGNMENT, "1" ) );
            }

            if ( 0 < update_variables_id.size() ) {
                ConcurrentCommandCollector concurrent_commands_collector = new ConcurrentCommandCollector(String.format("_%d", node_index));
                for (int i = 0; i < update_variables_id.size(); i++) {
                    concurrent_commands_collector.commands.add((new ConcurrentCommand()).make_write(update_variables_id.get(i), update_values.get(i)));
                }

                concurrent_commands_collector.generate_requests(this.control_graph_pta,
                        locations_id_ready, locations_id_all_can_run,
                        new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(0)),
                                UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_squares * (0.5 + level_ready )))),
                        false,
                        "concurrent_process_id",
                        null,
                        null,
                        null,
                        null,
                        "",
                        update_for_successor.toString(),
                        null );
            }
            else
            {
                UppaalTransition to_all_can_run = new UppaalTransition("ready", "all_can_run",
                        new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(0)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current - (0.5 * level_squares)))),
                        transition_label_side,
                        "",
                        "",
                        update_for_successor.toString(), "", null);
                sub_graph_container.transitions.add(to_all_can_run);
            }
            level_base_current += level_squares;

            String previous_location_name = "all_can_run";

            for (String run_node : node_concurrent.run_nodes) {

                String location_run       = String.format( "run_%d", node_index );

                UppaalLocation loc_run = new UppaalLocation( location_run, UppaalBuilder.Side.middle_left,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current  ))),
                        "", UppaalBuilder.Side.none,
                        "", UppaalBuilder.Side.none,
                        true);
                sub_graph_container.locations.add( loc_run );

                UppaalTransition to_run = new UppaalTransition( previous_location_name, location_run,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current  - ( 0.5 * level_squares ) ))),
                        transition_label_side,
                        "",
                        UppaalSystem.uppaal_sync_signal_send( uppaal_channel_notify_by_id( this.control_graph.node_name_get_id(run_node) ) ),
                        "", "",null );
                sub_graph_container.transitions.add( to_run );

                level_base_current     += level_squares;
                previous_location_name  = location_run;
                node_index++;

            }

            Point place_loc_done = new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current  )));

            UppaalLocation loc_done = new UppaalLocation( "done", UppaalBuilder.Side.middle_left,
                    place_loc_done,
                    "", UppaalBuilder.Side.none,
                    "", UppaalBuilder.Side.none,
                    0 != node_preconditions.length());
            sub_graph_container.locations.add( loc_done );

            create_transitions_for_loop( sub_graph_container, "done",
                    place_loc_done, node_preconditions, node_condition_can_not_start );

            UppaalTransition to_done = new UppaalTransition( previous_location_name, "done",
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current  - ( 0.5 * level_squares ) ))),
                    transition_label_side,
                    "",
                    "",
                    "", "",null );
            sub_graph_container.transitions.add( to_done );

        } else if (ControlNodeInterface.ControlNodeType.node_sequential == this.control_node.get_node_kind()) {
            ControlNodeSequential node_sequential = (ControlNodeSequential) this.control_node;

            String previous_location_name = "ready";
            int update_index = 0;

            final int level_wait            = 0;
            final int level_plp_running     = 1;
            final int level_plp_done        = 2;
            final int levels_in_iteration   = level_plp_done + 1;

            int level_base_current          = level_squares * ( level_ready + 1 );

            UppaalBuilder.Side transition_label_side = UppaalBuilder.Side.middle_right;

            for ( ControlUpdateForPLP update_for_plp : node_sequential.update_for_plp )
            {
                String location_label_wait           = String.format( "wait_%d",         update_index );
                String location_label_plp_running    = String.format( "plp_running_%d",  update_index );
                String location_label_plp_done       = String.format( "plp_done_%d",     update_index );

                UppaalLocation loc_wait = new UppaalLocation( location_label_wait, UppaalBuilder.Side.middle_left,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_wait ) ))),
                        "", UppaalBuilder.Side.none,
                        UppaalBuilder.binary_expression_enclosed( "local_time", UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf(update_for_plp.wait_time) ), UppaalBuilder.Side.bottom_right,
                        false);
                String locations_id_wait = control_graph_pta.get_new_location_id();
                loc_wait.location_id     = locations_id_wait;
                sub_graph_container.locations.add( loc_wait );

                UppaalTransition to_wait = new UppaalTransition( previous_location_name, location_label_wait,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_wait ) - ( 0.5 * level_squares ) ))),
                        transition_label_side,
                        "", "",
                        UppaalBuilder.binary_expression( "local_time", UppaalBuilder.STR_ASSIGNMENT, "0" ), "",null );
                sub_graph_container.transitions.add( to_wait );

                UppaalLocation loc_plp_running = new UppaalLocation( location_label_plp_running, UppaalBuilder.Side.middle_left,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_plp_running ) ))),
                        "", UppaalBuilder.Side.none,
                        "", UppaalBuilder.Side.none,
                        false);
                String locations_id_plp_running = control_graph_pta.get_new_location_id();
                loc_plp_running.location_id     = locations_id_plp_running;
                sub_graph_container.locations.add( loc_plp_running );

                int plp_id = this.plp_catalog.find_plp_id_by_name( update_for_plp.plp_name );
                // StringBuffer results_string_buffer = new StringBuffer();
                List<Integer>   update_variables_id = new LinkedList<>();
                List<String>    update_values       = new LinkedList<>();

                try {
                    // this.xml_to_uppaal_converter.convert_xml_conditions_to_uppaal_assignment(plp_id, results_string_buffer, update_for_plp.updates);
                    this.xml_to_uppaal_converter.convert_xml_condition_for_assignments_to_list_of_variable_and_values( plp_id, update_for_plp.updates, update_variables_id, update_values );
                } catch (VerificationException exception) {
                    throw new VerificationException("Control node \"" + this.control_node.get_node_name() + "\", invalid assignment.\n" + exception.get_message());
                }


                String local_time_initialize = UppaalBuilder.binary_expression_enclosed( "local_time", UppaalBuilder.STR_EQUAL, String.valueOf(update_for_plp.wait_time) );

                if ( 0 < update_variables_id.size() ) {
                    ConcurrentCommandCollector concurrent_commands_collector = new ConcurrentCommandCollector( String.format( "_%d", update_index ) );
                    for (int i = 0; i < update_variables_id.size(); i++) {
                        concurrent_commands_collector.commands.add((new ConcurrentCommand()).make_write(update_variables_id.get(i), update_values.get(i)));
                    }

                    concurrent_commands_collector.generate_requests(this.control_graph_pta,
                            locations_id_wait, locations_id_plp_running,
                            new Point( UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ),
                                       UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * ( 0.5 + level_wait ) ) ))),
                            false,
                            "concurrent_process_id",
                            null,
                            null,
                            local_time_initialize,
                            null,
                            null,
                            null,
                            UppaalSystem.uppaal_sync_signal_send(UppaalPLP.get_plp_start_channel(plp_id)) );
                }
                else
                {
                    UppaalTransition to_plp_running = new UppaalTransition( location_label_wait, location_label_plp_running,
                            new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_plp_running ) - ( 0.5 * level_squares ) ))),
                            transition_label_side,
                            local_time_initialize,
                            UppaalSystem.uppaal_sync_signal_send(UppaalPLP.get_plp_start_channel(plp_id)),
                            "", "",null );
                    sub_graph_container.transitions.add( to_plp_running );
                }


                UppaalLocation loc_plp_done = new UppaalLocation( location_label_plp_done, UppaalBuilder.Side.middle_left,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_plp_done ) ))),
                        "", UppaalBuilder.Side.none,
                        "", UppaalBuilder.Side.none,
                        true);
                sub_graph_container.locations.add( loc_plp_done );


                UppaalTransition to_plp_done = new UppaalTransition( location_label_plp_running, location_label_plp_done,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_plp_done ) - ( 0.5 * level_squares ) ))),
                        transition_label_side,
                        "",
                        UppaalSystem.uppaal_sync_signal_receive(UppaalPLP.get_plp_done_channel(plp_id)),
                        "", "",null );
                sub_graph_container.transitions.add( to_plp_done );

                previous_location_name = location_label_plp_done;

                level_base_current += level_squares * levels_in_iteration;

                update_index++;
            }

            final int level_all_plps_done   = 0;
            final int level_done            = 1;

            UppaalLocation loc_all_plps_done = new UppaalLocation( "all_plps_done", UppaalBuilder.Side.middle_left,
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_all_plps_done ) ))),
                    "", UppaalBuilder.Side.none,
                    "", UppaalBuilder.Side.none,
                    true);
            sub_graph_container.locations.add( loc_all_plps_done );


            String successor_transition_notify  = "";
            String successor_synchronization    = "";
            
            if ( false == node_sequential.next_node_name.isEmpty() ) {
                int transition_id_to_next_node = this.control_graph.get_transition_id(this.control_node.get_node_name(), node_sequential.next_node_name);
                String variable_to_next_node = this.uppaal_variable_can_run_by_id(transition_id_to_next_node);
                successor_transition_notify = UppaalBuilder.binary_expression( variable_to_next_node, UppaalBuilder.STR_ASSIGNMENT, "1" );

                int next_node_id = this.control_graph.node_name_get_id( node_sequential.next_node_name );
                successor_synchronization = UppaalSystem.uppaal_sync_signal_send( uppaal_channel_notify_by_id(next_node_id) );
            }
            
            UppaalTransition to_all_plps_done = new UppaalTransition( previous_location_name, "all_plps_done",
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_all_plps_done ) - ( 0.5 * level_squares ) ))),
                    transition_label_side,
                    "", "",
                    successor_transition_notify,
                    "",null );
            sub_graph_container.transitions.add( to_all_plps_done );

            Point place_loc_done = new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_done ) )));
            UppaalLocation loc_done = new UppaalLocation( "done", UppaalBuilder.Side.middle_left,
                    place_loc_done,
                    "", UppaalBuilder.Side.none,
                    "", UppaalBuilder.Side.none,
                    0 != node_preconditions.length());
            sub_graph_container.locations.add( loc_done );

            create_transitions_for_loop( sub_graph_container, "done",
                    place_loc_done, node_preconditions, node_condition_can_not_start );

            UppaalTransition to_done = new UppaalTransition( "all_plps_done", "done",
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_done ) - ( 0.5 * level_squares ) ))),
                    transition_label_side,
                    "",
                    successor_synchronization,
                    "",
                    "",null );
            sub_graph_container.transitions.add( to_done );

        } else if (ControlNodeInterface.ControlNodeType.node_condition == this.control_node.get_node_kind()) {
            ControlNodeCondition node_condition = (ControlNodeCondition) this.control_node;

            int node_index          = 0;
            int level_base_current  = level_squares * ( level_ready + 1 );

            UppaalBuilder.Side transition_label_side = UppaalBuilder.Side.middle_right;

            final int level_main = 0;



            UppaalLocation loc_main = new UppaalLocation( "main", UppaalBuilder.Side.middle_left,
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_main ) ))),
                    "", UppaalBuilder.Side.none,
                    "", UppaalBuilder.Side.none,
                    false == node_condition.condition_of_successor_nodes.isEmpty() );
            String locations_id_main = control_graph_pta.get_new_location_id();
            loc_main.location_id     = locations_id_main;
            sub_graph_container.locations.add( loc_main );


            UppaalTransition to_main = new UppaalTransition( "ready", "main",
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( 0 ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_main ) - ( 0.5 * level_squares ) ))),
                    transition_label_side,
                    UppaalBuilder.binary_expression_enclosed( "local_time", UppaalBuilder.STR_EQUAL, String.valueOf(node_condition.wait_time) ),
                    "",
                    "", "",null );
            sub_graph_container.transitions.add( to_main );


            final int x_offset_squares      = 30;
            final double level_nail         = 0.5;
            final double level_chosen_path  = 1.5;
            final double level_done         = 2.5;


            for (ControlConditionOfSuccessor condition_of_successor : node_condition.condition_of_successor_nodes) {

                int current_x_offset = x_offset_squares * node_index;

                String location_label_chosen_path   = String.format( "path_%d", node_index );
                String location_label_done          = String.format( "done_%d", node_index );


                UppaalLocation loc_chosen_path = new UppaalLocation( location_label_chosen_path, UppaalBuilder.Side.middle_left,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( current_x_offset ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_chosen_path ) ))),
                        "", UppaalBuilder.Side.none,
                        "", UppaalBuilder.Side.none,
                        true );
                String locations_id_chosen_path = control_graph_pta.get_new_location_id();
                loc_chosen_path.location_id     = locations_id_chosen_path;
                sub_graph_container.locations.add( loc_chosen_path );


                String successor_transition_notify  = "";
                String successor_synchronization    = "";

                if ( false == condition_of_successor.node_name.isEmpty() ) {
                    int transition_id_to_next_node = this.control_graph.get_transition_id(this.control_node.get_node_name(), condition_of_successor.node_name );
                    String variable_to_next_node = this.uppaal_variable_can_run_by_id(transition_id_to_next_node);
                    successor_transition_notify = UppaalBuilder.binary_expression( variable_to_next_node, UppaalBuilder.STR_ASSIGNMENT, "1" );

                    int next_node_id = this.control_graph.node_name_get_id( condition_of_successor.node_name );
                    successor_synchronization = UppaalSystem.uppaal_sync_signal_send( uppaal_channel_notify_by_id(next_node_id) );
                }

                StringBuffer update_for_successor = new StringBuffer();
                UppaalBuilder.add_to_cumulative_assignment( update_for_successor,
                        successor_transition_notify );


                StringBuffer condition_string_buffer;

                try {
                    condition_string_buffer = this.xml_to_uppaal_converter.convert_xml_conditions_to_uppaal( -1, condition_of_successor.condition, UppaalBuilder.STR_AND, null );
                } catch (VerificationException exception) {
                    throw new VerificationException("Control node \"" + this.control_node.get_node_name() + "\", invalid condition.\n" +
                            exception.get_message() + "\n" +
                            "Note: New variables should be defined in configuration file." );
                }


                List<Integer>   update_variables_id = new LinkedList<>();
                List<String>    update_values       = new LinkedList<>();

                try {
                    //this.xml_to_uppaal_converter.convert_xml_conditions_to_uppaal_assignment( -1, update_string_buffer, condition_of_successor.updates );
                    this.xml_to_uppaal_converter.convert_xml_condition_for_assignments_to_list_of_variable_and_values( -1, condition_of_successor.updates, update_variables_id, update_values );

                } catch (VerificationException exception) {
                    throw new VerificationException("Control node \"" + this.control_node.get_node_name() + "\", invalid assignment.\n" +
                            exception.get_message() + "\n" +
                            "Note: New variables should be defined in configuration file." );
                }


                if ( 0 < update_variables_id.size() ) {
                    ConcurrentCommandCollector concurrent_commands_collector = new ConcurrentCommandCollector(String.format("_%d", node_index));
                    for (int i = 0; i < update_variables_id.size(); i++) {
                        concurrent_commands_collector.commands.add((new ConcurrentCommand()).make_write(update_variables_id.get(i), update_values.get(i)));
                    }

                    concurrent_commands_collector.generate_requests(this.control_graph_pta,
                            locations_id_main, locations_id_chosen_path,
                            new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( current_x_offset ) ),
                                    UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_chosen_path ) ))),
                            false,
                            "concurrent_process_id",
                            null,
                            null,
                            condition_string_buffer.toString(),
                            null,
                            "",
                            update_for_successor.toString(),
                            null );
                }
                else
                {
                    List<Point> nails = new LinkedList<>();
                    nails.add(new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(current_x_offset)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_nail)))));


                    UppaalTransition to_chosen_path = new UppaalTransition("main", location_label_chosen_path,
                            new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(current_x_offset)), UppaalBuilder.direction_down(UppaalBuilder.squares_length(level_base_current + (level_squares * level_nail)))),
                            transition_label_side,
                            condition_string_buffer.toString(),
                            "",
                            update_for_successor.toString(),
                            "", nails);
                    sub_graph_container.transitions.add(to_chosen_path);
                }


                Point place_loc_done = new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( current_x_offset ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_done ) )));
                UppaalLocation loc_done = new UppaalLocation( location_label_done, UppaalBuilder.Side.middle_left,
                        place_loc_done,
                        "", UppaalBuilder.Side.none,
                        "", UppaalBuilder.Side.none,
                        0 != node_preconditions.length() );
                sub_graph_container.locations.add( loc_done );


                create_transitions_for_loop( sub_graph_container, location_label_done,
                        place_loc_done, node_preconditions, node_condition_can_not_start );


                int next_node_id = this.control_graph.node_name_get_id( condition_of_successor.node_name );

                UppaalTransition to_done = new UppaalTransition( location_label_chosen_path, location_label_done,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( current_x_offset ) ), UppaalBuilder.direction_down(UppaalBuilder.squares_length( level_base_current + ( level_squares * level_done ) - ( 0.5 * level_squares ) ))),
                        transition_label_side,
                        "",
                        successor_synchronization,
                        "", "", null );
                sub_graph_container.transitions.add( to_done );

                node_index++;
            }
        }

        sub_graph_container.add_sub_graph( this.control_graph_pta, new Point(0,0), "" );

        return this.control_graph_pta;
    }


}
