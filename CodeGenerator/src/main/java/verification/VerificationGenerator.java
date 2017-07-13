package verification;

import conditions.Condition;
import conditions.Formula;
import distributions.*;
import distributions.GammaDistribution;
import distributions.NormalDistribution;
import effects.AssignmentEffect;
import effects.Effect;
import modules.*;
import plpEtc.Range;
import plpFields.*;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * Created by alexds9 on 09/06/17.
 */
public class VerificationGenerator {

    private List<String>                plp_processes;

    public StringBuffer                 uppaal_file_declarations;
    public StringBuffer                 uppaal_file_modules;
    public StringBuffer                 uppaal_file_system_declarations;

    public StringBuffer                 verification_declarations_start;
    public String                       verification_declarations_end;
    public String                       verification_modules_start;
    public StringBuffer                 verification_modules_end;
    public String                       verification_system_declarations_start;
    public String                       verification_system_declarations_end;
    public String                       verification_queries;
    public String                       verification_plp_achieve;
    public String                       verification_plp_detect;
    public String                       verification_plp_maintain;
    public String                       verification_plp_observe;

    private VerificationVariableManager variable_manager;
    private VerificationSettings        settings;
    private XMLtoUppaalConverter        xml_to_uppaal_converter;
    private VerificationReports         reports;
    private PLPCatalog                  plp_catalog;

    public VerificationGenerator( VerificationVariableManager variable_manager, VerificationSettings settings,
                                  XMLtoUppaalConverter xml_to_uppaal_converter, VerificationReports reports,
                                  PLPCatalog plp_catalog )
    {
        this.variable_manager                   = variable_manager;
        this.settings                           = settings;
        this.xml_to_uppaal_converter            = xml_to_uppaal_converter;
        this.reports                            = reports;
        this.plp_catalog                        = plp_catalog;

        this.plp_processes                      = new LinkedList<>();

        this.uppaal_file_declarations           = new StringBuffer();
        this.uppaal_file_modules                = new StringBuffer();
        this.uppaal_file_system_declarations    = new StringBuffer();

    }

    public void uppaal_file_add_to_declarations(String add_text) {
        this.uppaal_file_declarations.append( add_text );
    }

    public void uppaal_file_add_to_declarations(StringBuffer add_text) {
        this.uppaal_file_declarations.append( add_text );
    }

    public void uppaal_file_add_to_modules(String add_text) {
        this.uppaal_file_modules.append( add_text );
    }

    public void uppaal_file_add_to_modules(StringBuffer add_text) {
        this.uppaal_file_modules.append( add_text );
    }

    public void uppaal_file_add_to_system_declarations(String add_text) {
        this.uppaal_file_system_declarations.append( add_text );
    }

    public void uppaal_file_add_to_system_declarations(StringBuffer add_text) {
        this.uppaal_file_system_declarations.append( add_text );
    }


    public void uppaal_file_store( String uppaal_file_path ) {


        BufferedWriter out = null;
        try {
            Files.deleteIfExists(Paths.get( uppaal_file_path ));

            FileWriter fstream = new FileWriter( uppaal_file_path, true); //true tells to append data.
            out = new BufferedWriter(fstream);

            out.write(this.verification_declarations_start.toString()    ); out.newLine();
            out.write(this.uppaal_file_declarations.toString()           ); out.newLine();
            out.write(this.verification_declarations_end                 ); out.newLine();
            out.write(this.verification_modules_start                    ); out.newLine();
            out.write(this.uppaal_file_modules.toString()                ); out.newLine();
            out.write(this.verification_modules_end.toString()           ); out.newLine();
            out.write(this.verification_system_declarations_start        ); out.newLine();
            out.write(this.uppaal_file_system_declarations.toString()    ); out.newLine();
            out.write(this.verification_system_declarations_end          ); out.newLine();
            out.write(this.verification_queries                          ); out.newLine();

            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }


    }

    public double xml_probability_to_double( String in_probability )
    {
        return Double.parseDouble(in_probability);
    }

    public double probability_make_proper( double in_probability )
    {
        if ( in_probability < 0 )
        {
            return 0;
        }
        else
        {
            if ( in_probability > 1 )
            {
                return 1;
            }
            else
            {
                return in_probability;
            }
        }
    }

    public boolean is_equal_conditions( Condition con_a, Condition con_b )
    {
        if ( con_a == con_b )
        {
            return true;
        }
        else if ( (null == con_a) || (null == con_b) )
        {
            return false;
        }
        else
        {
            return con_a.equals(con_b);
        }
    }

    public List<String> plp_observation_to_assignments(int plp_id, ObservePLP plp_observe, List<Integer> return_observation_variable_id ) throws VerificationException
    {

        ObservationGoal observation_goal = plp_observe.getGoal();

        if ( PLPParameter.class.isInstance(observation_goal) )
        {
            PLPParameter observation_goal_parameter = (PLPParameter)observation_goal;
            //String goal_string = convert_xml_condition_to_uppaal( plp_id, "", (Condition)plp_observe.getGoal(), observation_goal_variables );

            int variable_id                     = this.variable_manager.variable_or_parameter_get_variable_id( plp_id, XMLtoUppaalConverter.parameter_get_first_field( observation_goal_parameter ) );
            VerificationVariable variable_data  = this.variable_manager.global_variable_get_data(variable_id);


            return_observation_variable_id.add( variable_id );


            // TODO: Make user know if range for observed variable hasn’t been configured.
            if ( false == variable_data.is_in_range )
            {
                throw new VerificationException("Variable to observe needs to has min and max values defined.");
            }
            Vector<Integer> sample_values = new Vector<>();
            int max_samples = settings.get_int(settings.SETTING_OBSERVE_VARIABLE_SAMPLES);
            int values_range = variable_data.max_value - variable_data.min_value + 1;

            if ( values_range <= max_samples )
            {
                for (int i = variable_data.min_value; i <= variable_data.max_value; i++ )
                {
                    sample_values.add(new Integer(i));
                }
            }
            else
            {
                final int SAMPLES_USED_1 = 1;
                final int SAMPLES_USED_2 = 2;

                if ( SAMPLES_USED_1 == max_samples )
                {
                    sample_values.add(new Integer(variable_data.min_value));
                }
                else if ( SAMPLES_USED_2 <= max_samples )
                {
                    sample_values.add(new Integer(variable_data.min_value));
                    sample_values.add(new Integer(variable_data.max_value));

                    int remained_samples        = max_samples  - SAMPLES_USED_2;
                    int remained_values_range   = values_range - SAMPLES_USED_2;
                    int remained_intervals      = remained_samples + 1;
                    int sample_interval = remained_values_range / remained_intervals;

                    for (int i = 1; i <= remained_samples; i++ )
                    {
                        sample_values.add(new Integer(variable_data.min_value + sample_interval * i));
                    }


                }
            }


            List<String> assignments = new LinkedList<>();

            for ( Integer sample_value : sample_values ) {
                assignments.add(String.format("concurrent_write( %d, %d )", variable_id, sample_value.intValue() ));
            }

            return assignments;
        }
        else
        {
            throw new VerificationException("Problem with observation.");
        }

    }

    static public List<UppaalTransition> generate_nondeterministic_transitions( String source_id, Point source_place,
                                                       String target_id, Point target_place,
                                                       String common_assignment,
                                                       List<String> nondeterministic_assignments )
    {
        List<UppaalTransition> transitions = new LinkedList<>();

        int side_distance_to_nail_from_nearest_point    = UppaalBuilder.squares_length(1);
        int assignments_amount                          = nondeterministic_assignments.size();
        int height_padding                              = UppaalBuilder.units_letter_hight / 2;
        int distance_between_transitions                = (UppaalBuilder.text_lines(common_assignment) + 1 ) * ( UppaalBuilder.units_letter_hight ) + ( 2 * height_padding );
        int structure_height                            = assignments_amount * distance_between_transitions;
        int structure_height_half                       = structure_height / 2;

        Point place_nail_left = new Point(
                source_place.x + side_distance_to_nail_from_nearest_point,
                source_place.y - structure_height_half );

        Point place_nail_right = new Point(
                target_place.x - side_distance_to_nail_from_nearest_point,
                target_place.y - structure_height_half );

        Point place_label = new Point(
                source_place.x + (target_place.x - source_place.x) / 2,
                source_place.y - place_nail_left.y - (UppaalBuilder.units_letter_hight * 2) );


        for ( String nondeterministic_assignment : nondeterministic_assignments ) {

            List<Point> nails = new LinkedList<>();
            nails.add( new Point(place_nail_left)  );
            nails.add( new Point(place_nail_right) );

            StringBuffer update = new StringBuffer( common_assignment );

            UppaalBuilder.add_to_cumulative_assignment( update, "// Observation:\n" + nondeterministic_assignment );

            place_label.y = place_nail_left.y + (height_padding);

            UppaalTransition transition = new UppaalTransition(
                    source_id, target_id, new Point(place_label), UppaalBuilder.Side.bottom_center,
                    "", "",
                    update.toString(),
                    "", nails );

            transitions.add( transition );

            place_nail_left.y   += distance_between_transitions;
            place_nail_right.y  += distance_between_transitions;

        }

        return transitions;
    }

    public int failure_levels_height_in_squares( int level )
    {
        return 3 + ( 3 * level );
    }

    public void  plp_add_success_and_failure_probabilities( int plp_id, PLP plp, UppaalPLP plp_uppaal ) throws VerificationException
    {
        List<FailureMode>       failure_modes;
        List<ConditionalProb>   success_conditions;
        String location_source_id;
        String location_target_id;

        Point base_top_left = new Point();
        List <ConditionalDist> failure_distributions = new LinkedList<>();
        boolean has_failure_distributions = true;

        if ( AchievePLP.class.isInstance(plp) )
        {
            failure_modes       = ((AchievePLP)plp).getFailureModes();
            success_conditions  = ((AchievePLP)plp).getSuccessProb();
            location_source_id  = "id33";
            location_target_id  = "id30";
            base_top_left.x     = -1023;
            base_top_left.y     = -612;

            failure_distributions       = ((AchievePLP)plp).getFailRuntime();
            has_failure_distributions   = false == failure_distributions.isEmpty();
        }
        else if ( MaintainPLP.class.isInstance(plp) )
        {
            failure_modes       = ((MaintainPLP)plp).getFailureModes();
            success_conditions  = ((MaintainPLP)plp).getSuccessProb();
            location_source_id  = "id70";
            location_target_id  = "id74";
            base_top_left.x     = 306;
            base_top_left.y     = -170;

            failure_distributions       = ((MaintainPLP)plp).getFailRuntime();
            has_failure_distributions   = false == failure_distributions.isEmpty();
        }
        else if ( ObservePLP.class.isInstance(plp) )
        {
            failure_modes       = new LinkedList<>();
            success_conditions  = new LinkedList<>();

            List<ConditionalProb> failure_to_observe_probs = ((ObservePLP)plp).getFailureToObserveProb();
            Condition always_true = new Formula( "1", "1", XMLtoUppaalConverter.STR_XML_EQUAL );

            for ( ConditionalProb failure_to_observe_prob : failure_to_observe_probs )
            {
                String failure_probability_string = failure_to_observe_prob.getProb();

                FailureMode failure_mode = new FailureMode(always_true);
                failure_mode.addProb( new ConditionalProb( failure_probability_string, failure_to_observe_prob.getCondition() ) );
                failure_modes.add(failure_mode);

                String success_probability_string = String.valueOf( 1 - probability_make_proper(xml_probability_to_double(failure_probability_string)));
                ConditionalProb success_probability = new ConditionalProb(success_probability_string, failure_to_observe_prob.getCondition() );
                success_conditions.add(success_probability);
            }

            location_source_id  = "id101";
            location_target_id  = "id102";
            base_top_left.x     = -382;
            base_top_left.y     = -272;

            failure_distributions       = ((ObservePLP)plp).getFailureRuntime();
            has_failure_distributions   = false == failure_distributions.isEmpty();
        }
        else if ( DetectPLP.class.isInstance(plp) )
        {
            failure_modes       = new LinkedList<>();
            success_conditions  = ((DetectPLP)plp).getSuccessProbGivenCondition();
            Condition always_true = new Formula( "1", "1", XMLtoUppaalConverter.STR_XML_EQUAL );

            for ( ConditionalProb success_condition : success_conditions )
            {
                String success_probability_string = success_condition.getProb();
                String failure_probability_string = String.valueOf( 1 - probability_make_proper(xml_probability_to_double(success_probability_string)));

                FailureMode failure_mode = new FailureMode(always_true);
                failure_mode.addProb( new ConditionalProb( failure_probability_string, success_condition.getCondition() ) );
                failure_modes.add(failure_mode);
            }

            success_conditions  = ((DetectPLP)plp).getSuccessProbGivenCondition();
            location_source_id  = "id134";
            location_target_id  = "id131";
            base_top_left.x = -288;
            base_top_left.y = -136;

            has_failure_distributions = false;
        }
        else
        {
            throw new VerificationException("Unknown PLP type.");
        }

        int success_id = 0;
        final int structure_x_squares_nail          = 2;
        final int structure_x_squares_branchpoint   = 5;
        final int structure_x_squares_fail          = 9;
        final int structure_x_squares_fail_done     = 13;

        Vector<String> failure_case_id_to_failure_location_id = new Vector<>();

        UppaalSubGraphContainer sub_graph_probabilities = new UppaalSubGraphContainer();

        for ( ConditionalProb success_condition : success_conditions )
        {

            String location_branchpoint_id = plp_uppaal.get_new_location_id();

            sub_graph_probabilities.branchpoints.add( new UppaalBranchpoint(location_branchpoint_id,
                    new Point( UppaalBuilder.direction_right(UppaalBuilder.squares_length(structure_x_squares_branchpoint)), UppaalBuilder.direction_down(UppaalBuilder.squares_length( failure_levels_height_in_squares( success_id ) )) )) );

            StringBuffer success_condition_buffer = new StringBuffer();
            this.xml_to_uppaal_converter.convert_xml_condition_to_uppaal( plp_id, success_condition_buffer, success_condition.getCondition(), null );


            List<Point> nails_success = new LinkedList<>();
            nails_success.add( new Point( UppaalBuilder.direction_right(UppaalBuilder.squares_length(structure_x_squares_nail)),   UppaalBuilder.direction_down(UppaalBuilder.squares_length( failure_levels_height_in_squares( success_id ) )) ) );

            String success_comment = "";
            if ( null != success_condition.getCondition() )
            {
                success_comment = "// " + UppaalBuilder.comply_string_single_line( success_condition.getCondition().toString()  ) + "\n";
            }

            sub_graph_probabilities.transitions.add( new UppaalTransition( location_source_id, location_branchpoint_id,
                    new Point( UppaalBuilder.direction_right(UppaalBuilder.squares_length(structure_x_squares_nail - 1)),
                               UppaalBuilder.direction_down(UppaalBuilder.squares_length(failure_levels_height_in_squares( success_id ) - 3 )) ), UppaalBuilder.Side.bottom_left,
                    success_comment +
                    success_condition_buffer.toString(), "", "", "", nails_success ) );

            sub_graph_probabilities.transitions.add( new UppaalTransition( location_branchpoint_id, location_target_id,
                    new Point( UppaalBuilder.direction_down(UppaalBuilder.squares_length(structure_x_squares_branchpoint)),
                               UppaalBuilder.direction_down(UppaalBuilder.squares_length(failure_levels_height_in_squares( success_id ) - 1)) ), UppaalBuilder.Side.bottom_left,
                    "", "", "",
                    String.valueOf( this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int( success_condition.getProb() ) ), null ) );



            int failure_id = 0;
            for ( FailureMode failure_mode : failure_modes )
            {

                for ( ConditionalProb failure_case : failure_mode.getProbList() ) {
                    if ( is_equal_conditions( failure_case.getCondition(), success_condition.getCondition() ) ) {
                        String location_failure_id;
                        String location_failure_done_id = "";

                        if ( failure_case_id_to_failure_location_id.size() > failure_id ) {
                            location_failure_id = failure_case_id_to_failure_location_id.get(failure_id);
                        }
                        else
                        {

                            location_failure_id = "failure_" + String.valueOf(failure_id);
                            failure_case_id_to_failure_location_id.add( failure_id, location_failure_id );

                            String extra = "";
                            if ( has_failure_distributions ){
                                extra = "[<[main_success_labels]>]";
                            }

                            sub_graph_probabilities.locations.add( new UppaalLocation( location_failure_id, UppaalBuilder.Side.bottom_center,
                                    new Point( UppaalBuilder.direction_right(UppaalBuilder.squares_length(structure_x_squares_fail)),
                                            UppaalBuilder.direction_down(UppaalBuilder.squares_length( failure_levels_height_in_squares( failure_id ) ))  ),
                                    "", UppaalBuilder.Side.none,
                                    "", UppaalBuilder.Side.none,
                                    false,
                                    extra ) );

                            if ( has_failure_distributions ) {

                                location_failure_done_id = "failure_done_" + String.valueOf(failure_id);
                                sub_graph_probabilities.locations.add( new UppaalLocation( location_failure_done_id, UppaalBuilder.Side.bottom_center,
                                        new Point( UppaalBuilder.direction_right(UppaalBuilder.squares_length(structure_x_squares_fail_done)),
                                                   UppaalBuilder.direction_down(UppaalBuilder.squares_length( failure_levels_height_in_squares( failure_id ) ))),
                                        "", UppaalBuilder.Side.none,
                                        "", UppaalBuilder.Side.none,
                                        false ) );
                            }

                        }

                        String extra = "";
                        if ( has_failure_distributions ) {
                            extra = "[<[transition_to_main_success_labels]>]";
                        }

                        sub_graph_probabilities.transitions.add( new UppaalTransition( location_branchpoint_id, location_failure_id,
                                new Point( UppaalBuilder.direction_right(UppaalBuilder.squares_length( 0.5 + structure_x_squares_branchpoint )),
                                           UppaalBuilder.direction_down(UppaalBuilder.squares_length( failure_levels_height_in_squares( success_id ) - 0.5 + ( 0.5 * failure_id )  )) ),
                                UppaalBuilder.Side.top_center,
                                "", "", "",
                                String.valueOf( this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int(failure_case.getProb()) ),
                                null, extra ) );



                        if ( has_failure_distributions ) {
                            StringBuffer transition_labels_before_main  = new StringBuffer();
                            StringBuffer labels_at_main                 = new StringBuffer();

                            plp_add_time_distribution( plp_id, plp_uppaal, sub_graph_probabilities, failure_distributions.get(0),
                                    location_failure_id, location_failure_done_id,
                                    transition_labels_before_main, labels_at_main,
                                    new Point(
                                            base_top_left.x + UppaalBuilder.direction_right(UppaalBuilder.squares_length(structure_x_squares_fail)),
                                            base_top_left.y + UppaalBuilder.direction_down(UppaalBuilder.squares_length( failure_levels_height_in_squares( failure_id ) )) ),
                                    new Point(  UppaalBuilder.direction_right(UppaalBuilder.squares_length(structure_x_squares_fail_done)) - UppaalBuilder.direction_right(UppaalBuilder.squares_length(structure_x_squares_fail)),
                                                0  ) );

                            sub_graph_probabilities.replace_extra_locations(    "[<[main_success_labels]>]",               labels_at_main.toString() );
                            sub_graph_probabilities.replace_extra_transitions(  "[<[transition_to_main_success_labels]>]", transition_labels_before_main.toString()     );
                        }

                        break;
                    }

                }

                failure_id++;
            }

            success_id++;
        }

        if ( ( true == success_conditions.isEmpty() ) &&
             ( true == failure_modes.isEmpty()      ) )
        {
            sub_graph_probabilities.transitions.add( new UppaalTransition( location_source_id, location_target_id,
                    base_top_left,
                    UppaalBuilder.Side.top_left,
                    "", "", "", "", null, null ) );
        }

        sub_graph_probabilities.add_sub_graph( plp_uppaal, new Point( base_top_left.x, base_top_left.y ), "" );

    }

    private ConcurrentCommand convert_plp_condition_restriction_to_concurrent_command(int plp_id, Condition condition ) throws VerificationException
    {
        ConcurrentCommand command = null;

        if ( Formula.class.isInstance(condition) ) {
            Formula formula = (Formula) condition;

            if (formula.getRange() != null) // Right part containing range
            {
                String variable_name = formula.getLeftExpr();

                if ( this.variable_manager.variable_or_parameter_is_exist(plp_id, variable_name) ) {
                    int variable_id = this.variable_manager.variable_or_parameter_get_variable_id(plp_id, variable_name);
                    Range range = formula.getRange();



                    int min;
                    int max;

                    try {
                        min = xml_to_uppaal_converter.convert_xml_value_to_uppaal_int(range.getMinValue());
                        max = xml_to_uppaal_converter.convert_xml_value_to_uppaal_int(range.getMinValue());
                    } catch (VerificationException exception) {
                        throw new VerificationException("problem in concurrent condition: \"" + condition.toString() + "\".\n" + exception.get_message());
                    }

                    if ( false == range.isMinInclusive()) {
                        min++;
                    }

                    if ( false == range.isMaxInclusive()) {
                        max--;
                    }

                    command = ((ConcurrentCommand)new ConcurrentCommand()).make_watch_add(variable_id,
                            "", min, max, true, "" );

                } else {
                    throw new VerificationException("Formula, variable \"" + variable_name + "\" does not exist.");
                }


            }
            else {
                String operand_left_xml = formula.getLeftExpr();
                String operand_right_xml = formula.getRightExpr();
                boolean left_is_numeric = false;
                int left_int_value = -1;
                boolean left_is_variable = false;
                int left_variable_id = -1;
                boolean right_is_numeric = false;
                int right_int_value = -1;
                boolean right_is_variable = false;
                int right_variable_id = -1;


                if (XMLtoUppaalConverter.xml_is_numeric_or_boolean(operand_left_xml)) {
                    try {
                        left_int_value = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int(operand_left_xml);
                        left_is_numeric = true;
                    } catch (VerificationException exception) {
                        throw new VerificationException("Concurrent condition \"" + condition.toString() + "\", can not read left value.\n" + exception.get_message());
                    }

                } else {
                    if (this.variable_manager.variable_or_parameter_is_exist(plp_id, operand_left_xml)) {
                        left_variable_id = this.variable_manager.variable_or_parameter_get_variable_id(plp_id, operand_left_xml);
                        left_is_variable = true;
                    } else {
                        throw new VerificationException("Concurrent condition , variable \"" + operand_left_xml + "\" does not exist.");
                    }
                }


                if (XMLtoUppaalConverter.xml_is_numeric_or_boolean(operand_right_xml)) {
                    try {
                        right_int_value = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int(operand_right_xml);
                        right_is_numeric = true;
                    } catch (VerificationException exception) {
                        throw new VerificationException("Concurrent condition \"" + condition.toString() + "\", can not read right value.\n" + exception.get_message());
                    }

                } else {
                    if (this.variable_manager.variable_or_parameter_is_exist(plp_id, operand_right_xml)) {
                        right_variable_id = this.variable_manager.variable_or_parameter_get_variable_id(plp_id, operand_right_xml);
                        right_is_variable = true;
                    } else {
                        throw new VerificationException("Concurrent condition, variable \"" + operand_right_xml + "\" does not exist.");
                    }
                }
                
                if ( left_is_numeric && !right_is_numeric && !left_is_variable &&  right_is_variable )
                {
                    command = new ConcurrentCommand();
                    if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_EQUAL ) )
                    {
                        command.make_watch_add( right_variable_id,
                                "", left_int_value, left_int_value, true, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_NOT_EQUAL ) ) {
                        command.make_watch_add( right_variable_id,
                                "", left_int_value - 1, left_int_value + 1, false, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_LESS_THAN ) ) {
                        command.make_watch_add( right_variable_id,
                                "", -1, left_int_value + 1, false, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_LESS_THAN_EQUAL ) ) {
                        command.make_watch_add( right_variable_id,
                                "", -1, left_int_value, false, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_GREATER_THAN ) ) {
                        command.make_watch_add( right_variable_id,
                                "", 0, left_int_value - 1, true, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_GREATER_THAN_EQUAL ) ) {
                        command.make_watch_add( right_variable_id,
                                "", 0, left_int_value, true, "" );
                    }
                } else if ( !left_is_numeric &&  right_is_numeric &&  left_is_variable && !right_is_variable )
                {
                    command = new ConcurrentCommand();
                    if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_EQUAL ) )
                    {
                        command.make_watch_add( left_variable_id,
                                "", right_int_value, right_int_value, true, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_NOT_EQUAL ) ) {
                        command.make_watch_add( left_variable_id,
                                "", right_int_value - 1, right_int_value + 1, false, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_LESS_THAN ) ) {
                        command.make_watch_add( left_variable_id,
                                "", 0, right_int_value - 1, true, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_LESS_THAN_EQUAL ) ) {
                        command.make_watch_add( left_variable_id,
                                "", 0, right_int_value, true, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_GREATER_THAN ) ) {
                        command.make_watch_add( left_variable_id,
                                "", -1, right_int_value + 1, false, "" );
                    } else if ( formula.getOperator().equals( XMLtoUppaalConverter.STR_XML_GREATER_THAN_EQUAL ) ) {
                        command.make_watch_add( left_variable_id,
                                "", -1, right_int_value, false, "" );
                    }
                }
            }
        }

        if ( null == command )
        {
            this.reports.add_warning("Concurrent condition only support formula with range or single variable and value, ignoring condition \"" + condition.toString() + "\"");
        }

        return command;
    }

    public List<String> add_concurrent_conditions( int plp_id, UppaalPLP plp_uppaal, List<ModuleRestriction> module_restrictions,
                                                   List<Condition> conditions_restrictions,
                                                   Condition condition_to_maintain,
                                                   String location_id_first_source,  String location_id_first_target,  Point place_first,
                                                   String location_id_second_source, String location_id_second_target, Point place_second,
                                                   Condition second_assignments, List<String> nondeterministic_assignments,
                                                   List<Integer> second_additional_write_variable_ids,
                                                   List<RequiredResource> required_resources,
                                                   List<Effect> side_effects ) throws VerificationException{

        ConcurrentCommandCollector concurrent_commands_collector_0 = new ConcurrentCommandCollector("_0");
        int this_module_variable_id = this.variable_manager.concurrent_module_index_to_variable_id(plp_id);

        List<String> uppaal_variables_for_requests_ids     = new LinkedList<>();

        for ( ModuleRestriction module_restriction : module_restrictions )
        {
            String uppaal_variable_for_request_id           = plp_uppaal.local_variable_add();
            String uppaal_variable_for_request_data         = plp_uppaal.local_concurrent_request_add();
            if ( true == this.plp_catalog.plp_name_is_exist(module_restriction.getModuleName()) ) {
                String other_concurrent_module_variable_name = this.variable_manager.concurrent_module_variable_name( module_restriction.getModuleName() );
                int other_concurrent_module_variable_id = this.variable_manager.global_variable_get_id(other_concurrent_module_variable_name);

                if (ModuleRestriction.ConcurrencyType.Mutex == module_restriction.getType()) {
                    concurrent_commands_collector_0.add_request((new ConcurrentCommand()).make_watch_add(other_concurrent_module_variable_id,
                            uppaal_variable_for_request_id, UppaalBuilder.INT_NUMERIC_FALSE, UppaalBuilder.INT_NUMERIC_FALSE, true, uppaal_variable_for_request_data, "// " + module_restriction.getModuleName().toString()));
                } else {
                    concurrent_commands_collector_0.add_request((new ConcurrentCommand()).make_watch_add(other_concurrent_module_variable_id,
                            uppaal_variable_for_request_id, UppaalBuilder.INT_NUMERIC_TRUE, UppaalBuilder.INT_NUMERIC_TRUE, true, uppaal_variable_for_request_data, "// " + module_restriction.getModuleName().toString()));
                }

                uppaal_variables_for_requests_ids.add( uppaal_variable_for_request_id );
            }
            else
            {
                throw new VerificationException("Concurrent module \"" + module_restriction.getModuleName() + "\" does not exist.");
            }
        }

        for ( Condition condition_restriction : conditions_restrictions )
        {

            ConcurrentCommand command = convert_plp_condition_restriction_to_concurrent_command( plp_id, condition_restriction );

            if ( null != command ) {
                String uppaal_variable_for_request_id   = plp_uppaal.local_variable_add();
                String uppaal_variable_for_request_data = plp_uppaal.local_concurrent_request_add();

                command.variable_for_request_id         = uppaal_variable_for_request_id;
                command.variable_with_request_data      = uppaal_variable_for_request_data;
                command.message                         = "// " + condition_restriction.toString();

                concurrent_commands_collector_0.add_request( command );

                uppaal_variables_for_requests_ids.add(uppaal_variable_for_request_id);
            }
        }

        if ( null != condition_to_maintain )
        {
            List<Integer> variables_id  = new LinkedList<>();
            List<String> values         = new LinkedList<>();

            this.xml_to_uppaal_converter.convert_xml_condition_for_assignments_to_list_of_variable_and_values( plp_id,
                    condition_to_maintain, variables_id, values );

            int assignment_amount = variables_id.size();

            for ( int i = 0 ; i < assignment_amount ; i++ )
            {
                int     variable_id = variables_id.get(i).intValue();
                String  value       = values.get(i);

                VerificationVariable variable_data = this.variable_manager.global_variable_get_data( variable_id );

                concurrent_commands_collector_0.add_request( (new ConcurrentCommand()).make_write( variable_id , value, "// " + condition_to_maintain.toString() ) );

                if ( true == variable_data.is_exclusive_access )
                {
                    String uppaal_variable_for_request_id     = plp_uppaal.local_variable_add();
                    String uppaal_variable_for_request_data   = plp_uppaal.local_concurrent_request_add();

                    concurrent_commands_collector_0.add_request(
                            (new ConcurrentCommand()).make_watch_add(
                                    variable_id,
                                    uppaal_variable_for_request_id,
                                    value,
                                    value,
                                    true,
                                    uppaal_variable_for_request_data ));

                    uppaal_variables_for_requests_ids.add( uppaal_variable_for_request_id );
                }


            }

        }


        for ( RequiredResource required_resource : required_resources )
        {
            if ( RequiredResource.RequirementStatus.Exclusive == required_resource.getReqStatus() )
            {
                boolean is_defined = false;

                if ( true == this.variable_manager.global_variable_is_exist( required_resource.getName() ) )
                {
                    int                     resource_variable_id    = this.variable_manager.global_variable_get_id( required_resource.getName() );
                    VerificationVariable    resource_variable       = this.variable_manager.global_variable_get_data( resource_variable_id );

                    if ( VerificationVariable.VerificationVariableType.type_resource == resource_variable.variable_type )
                    {
                        is_defined = true;

                        if ( UppaalBuilder.INT_RESOURCE_INVALID != required_resource.getQuantity() ) {
                            int required_amount = this.xml_to_uppaal_converter.convert_xml_double_to_uppaal_int(required_resource.getQuantity());

                            String uppaal_variable_for_request_id_minimum_value     = plp_uppaal.local_variable_add();
                            String uppaal_variable_for_request_data_minimum_value   = plp_uppaal.local_concurrent_request_add();

                            concurrent_commands_collector_0.add_request(
                                    (new ConcurrentCommand()).make_watch_add(
                                            resource_variable_id,
                                            uppaal_variable_for_request_id_minimum_value,
                                            -1,
                                            required_amount,
                                            false,
                                            uppaal_variable_for_request_data_minimum_value,
                                            "// Required resource: " + required_resource.getName() + " at least: " + required_resource.getQuantity()));

                            uppaal_variables_for_requests_ids.add(uppaal_variable_for_request_id_minimum_value);
                        }

                        String uppaal_variable_for_request_id_not_changed           = plp_uppaal.local_variable_add();
                        String uppaal_variable_for_request_data_not_changed         = plp_uppaal.local_concurrent_request_add();

                        concurrent_commands_collector_0.add_request(
                                (new ConcurrentCommand()).make_watch_add(
                                        resource_variable_id,
                                        uppaal_variable_for_request_id_not_changed,
                                        resource_variable.value,
                                        resource_variable.value,
                                        true,
                                        uppaal_variable_for_request_data_not_changed,
                                        "// Make sure resource: " + UppaalBuilder.comply_string_single_line( required_resource.getName() ) + " not changing."));

                        uppaal_variables_for_requests_ids.add( uppaal_variable_for_request_id_not_changed );

                    }
                }

                if ( false == is_defined )
                {
                    this.reports.add_warning("In PLP: \"" + plp_uppaal.get_name() + "\", resource: \"" + required_resource.getName() + "\", was not defined.");
                }
            }
            else
            {
                this.reports.add_warning("In PLP: \"" + plp_uppaal.get_name() + "\", resource: \"" + required_resource.getName() + "\", Frequency type unsupported, ignoring.");
            }
        }


        concurrent_commands_collector_0.add_request( (new ConcurrentCommand()).make_write( this_module_variable_id , UppaalBuilder.STR_NUMERIC_TRUE, "// This module is running.") );
        concurrent_commands_collector_0.generate_requests( plp_uppaal, location_id_first_source, location_id_first_target,
                place_first, true, "id",null, null,
                null, null, null,
                null, null);



        ConcurrentCommandCollector concurrent_commands_collector_1 = new ConcurrentCommandCollector("_1");

        for ( String uppaal_variables_for_request_id : uppaal_variables_for_requests_ids )
        {
            concurrent_commands_collector_1.add_request( (new ConcurrentCommand()).make_watch_remove( uppaal_variables_for_request_id, uppaal_variables_for_request_id ) );
        }


        //this.xml_to_uppaal_converter.convert_xml_formula_to_uppaal( plp_id, distribution_uniform.getLowerBound() )

        for ( Effect side_effect : side_effects )
        {
            if ( true == AssignmentEffect.class.isInstance( side_effect ) )
            {
                AssignmentEffect assignment_effect = (AssignmentEffect)side_effect;


                if ( true == this.variable_manager.variable_or_parameter_is_exist( plp_id, assignment_effect.getParam().getName() ) )
                {
                    int side_effect_variable_id = this.variable_manager.variable_or_parameter_get_variable_id( plp_id, assignment_effect.getParam().getName() );

                    String uppaal_variable_for_request_id_side_effect = plp_uppaal.local_variable_add();

                    String side_effect_initialize_value = UppaalBuilder.binary_expression(
                            uppaal_variable_for_request_id_side_effect,
                            UppaalBuilder.STR_ASSIGNMENT,
                            this.xml_to_uppaal_converter.convert_xml_formula_to_uppaal( plp_id, assignment_effect.getExpression().toString() ) );

                            concurrent_commands_collector_1.add_to_initial_assignments( side_effect_initialize_value );

                            concurrent_commands_collector_1.add_request(
                                (new ConcurrentCommand()).make_write(
                                        side_effect_variable_id,
                                        uppaal_variable_for_request_id_side_effect,
                                        "// Effect: " + assignment_effect.toString() ));


                }
                else
                {

                    this.reports.add_warning("In PLP: \"" + plp_uppaal.get_name() + "\", side_effect: \"" + side_effect.simpleString() + "\", variable \"" + assignment_effect.getParam().getName() + "\" not found.");
                }
            }
            else
            {
                this.reports.add_warning("In PLP: \"" + plp_uppaal.get_name() + "\", side_effect: \"" + side_effect.simpleString() + "\", is not assignment_effect, ignoring.");
            }

        }

        if ( null != second_assignments ) {
            List<Integer> variables_id  = new LinkedList<>();
            List<String> values         = new LinkedList<>();

            this.xml_to_uppaal_converter.convert_xml_condition_for_assignments_to_list_of_variable_and_values(plp_id, second_assignments, variables_id, values);
            int last_index = variables_id.size() - 1;

            for (int i = 0; i <= last_index; i++) {
                String uppaal_variable_for_value = plp_uppaal.local_variable_add();

                String message_for_initial_assignments = "";
                if ( 0 == i )
                {
                    message_for_initial_assignments = "// For Goal:\n";
                }

                concurrent_commands_collector_1.add_to_initial_assignments(
                        message_for_initial_assignments +
                        UppaalBuilder.binary_expression( uppaal_variable_for_value, UppaalBuilder.STR_ASSIGNMENT, values.get(i)));

                String message_for_request = "";

                if ( 0 == i )
                {
                    message_for_request += "// For Goal:\n";
                }
                message_for_request += "// " + UppaalBuilder.comply_string_single_line( this.variable_manager.global_variable_get_name( variables_id.get(i).intValue() ) );

                concurrent_commands_collector_1.add_request((new ConcurrentCommand()).make_write(
                        variables_id.get(i).intValue(), uppaal_variable_for_value,
                        message_for_request ) );
            }
        }

        concurrent_commands_collector_1.add_request( (new ConcurrentCommand()).make_write(
                this_module_variable_id , UppaalBuilder.STR_NUMERIC_FALSE,
                "// This module finished running.") );
        concurrent_commands_collector_1.generate_requests( plp_uppaal, location_id_second_source, location_id_second_target,
                place_second, true,  "id", nondeterministic_assignments, second_additional_write_variable_ids,
                null, null, null,
                null, null);

        List<String> uppaal_variables_with_results_from_requests = uppaal_variables_for_requests_ids;

        return uppaal_variables_with_results_from_requests;
    }

    static private String from_plp_name_to_repeat_variable( String original_plp_name )
    {
        return String.format( "_%s_repeat", original_plp_name );
    }

    public String get_plp_repeat_variable( int plp_id )
    {
        return from_plp_name_to_repeat_variable( this.plp_catalog.find_plp_name_by_id(plp_id) );
    }

    public void add_sorting_of_result_from_concurrent_requests(  int plp_id, UppaalPLP plp_uppaal,
                                                                 String source_id, Point source_place,
                                                                 String target_id,
                                                                 List<String> uppall_result_variables )
    {
        UppaalSubGraphContainer sub_graph       = new UppaalSubGraphContainer();
        StringBuffer            all_fails_false = new StringBuffer();

        final int x_place_squares_nail      = 2;
        final int x_place_squares_location  = 9;
        final int y_offset_squares          = 2;
        final int y_squares_all_false       = 0;
        int y_current_position_squares      = 2;
        int case_counter                    = 0;


        for ( String uppall_result_variable : uppall_result_variables )
        {
            String current_fail_location_name = String.format( "fail_%d", case_counter);


            UppaalLocation loc_current_fail = new UppaalLocation( current_fail_location_name,
                UppaalBuilder.Side.top_center,
                new Point(
                    UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_location ) ),
                    UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares   ) ) ),
                "", UppaalBuilder.Side.none,
                "", UppaalBuilder.Side.none,
                false);
            sub_graph.locations.add( loc_current_fail  );


            List<Point> nails = new LinkedList<>();
            nails.add(
                    new Point(
                            UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_nail ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ) ) ) );

            UppaalTransition to_current_fail = new UppaalTransition( source_id, current_fail_location_name,
                new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_nail ) ),
                        UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ))),
                        UppaalBuilder.Side.top_left,
                        UppaalBuilder.binary_expression_enclosed( uppall_result_variable, UppaalBuilder.STR_EQUAL, UppaalBuilder.STR_FALSE ),
                        null, null,null,
                        nails );
            sub_graph.transitions.add( to_current_fail );


            UppaalBuilder.add_to_cumulative_expression_with_operator( all_fails_false,
                    UppaalBuilder.binary_expression_enclosed( uppall_result_variable, UppaalBuilder.STR_EQUAL, UppaalBuilder.STR_TRUE ),
                    UppaalBuilder.STR_AND);

            case_counter++;
            y_current_position_squares += y_offset_squares;

        }

        UppaalTransition to_end = new UppaalTransition( source_id, target_id,
                new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_nail ) ),
                        UppaalBuilder.direction_down(UppaalBuilder.squares_length( y_squares_all_false ))),
                UppaalBuilder.Side.bottom_left,
                all_fails_false.toString(),
                null, null, null, null );
        sub_graph.transitions.add( to_end );

        sub_graph.add_sub_graph( plp_uppaal, source_place, "" );
    }

    public void uppaal_file_add_plp(PLP plp) throws VerificationException {
        int plp_id = this.plp_catalog.find_plp_id_by_name(plp.getBaseName());
        StringBuffer add_to_declarations          = new StringBuffer();
        StringBuffer add_to_modules               = new StringBuffer();
        StringBuffer add_to_system_declarations   = new StringBuffer();

        //String plp_id_string                = String.format("%d", plp_id);
        //String plp_uppaal.id_padded         = String.format("%05d", plp_id);
        UppaalPLP plp_uppaal    = new UppaalPLP( plp, plp_id, this.variable_manager );
        plp_uppaal.name         = plp.getBaseName();

        if ( ( null  != plp.getPreConditions()           ) &&
             ( false == plp.getPreConditions().isEmpty() ) )
        {
            plp_uppaal.precondition.append( "// Preconditions: " + UppaalBuilder.comply_string_single_line( plp.getPreConditions().toString() ) + "\n");
            plp_uppaal.precondition.append( this.xml_to_uppaal_converter.convert_xml_conditions_to_uppaal(plp_id, plp.getPreConditions(), UppaalBuilder.STR_AND, null ) );
        }


        if ( AchievePLP.class.isInstance(plp) ) {
            AchievePLP plp_achieve  = (AchievePLP) plp;
            plp_uppaal.insert_plp_template( this.verification_plp_achieve );


            try{

                UppaalSubGraphContainer sub_graph_probabilities = new UppaalSubGraphContainer();

                Point time_distribution_source = new Point (-552, -612 );
                Point time_distribution_target = new Point (-204, -612 );
                Point time_distribution_target_relative_to_source = new Point (
                        time_distribution_target.x - time_distribution_source.x,
                        time_distribution_target.y - time_distribution_source.y );

                List<String> created_locations_ids_by_time_distribution = plp_add_time_distribution( plp_id, plp_uppaal, sub_graph_probabilities, (plp_achieve.getSuccessRuntime()).get(0),
                        "id25", "id31",
                        plp_uppaal.transition_to_main_success_labels, plp_uppaal.main_success_labels,
                        time_distribution_source,
                        time_distribution_target_relative_to_source );

                sub_graph_probabilities.add_sub_graph( plp_uppaal, time_distribution_source, "" );

                plp_add_success_and_failure_probabilities( plp_id, plp, plp_uppaal );

                List<Condition> fail_termination_conditions = null;
                if ( null != plp_achieve.getFailTerminationCond() ) {
                    fail_termination_conditions = new LinkedList<>();
                    fail_termination_conditions.add( plp_achieve.getFailTerminationCond() );
                }

                plp_add_success_and_failure_terminations( plp_id, plp, plp_uppaal,
                        plp_achieve.getSuccessTerminationCond(),  fail_termination_conditions,
                          "id34",
                           "id31",
                               "id25",
                        new Point( -552, -612 ),
                        created_locations_ids_by_time_distribution,
                        9 );

                /*
                ConcurrentCommandCollector concurrent_commands_0 = new ConcurrentCommandCollector("_0");
                String var_0 = plp_uppaal.local_variable_add();
                String var_2 = plp_uppaal.local_variable_add();
                String var_3 = plp_uppaal.local_variable_add();
                String var_4 = plp_uppaal.local_variable_add();
                String req_0 = plp_uppaal.local_concurrent_request_add();
                concurrent_commands_0.add_request( (new ConcurrentCommand()).make_write( 30, var_0 ) );
                concurrent_commands_0.add_request( (new ConcurrentCommand()).make_write( 32, var_2 ) );
                concurrent_commands_0.add_request( (new ConcurrentCommand()).make_write( 35, var_3 ) );
                concurrent_commands_0.add_request( (new ConcurrentCommand()).make_watch_add( 35, var_4, 3, 13, true, req_0 ) );
                concurrent_commands_0.generate_requests( plp_uppaal, "id30", "id34", new Point(-731,-612) );

                ConcurrentCommandCollector concurrent_commands_1 = new ConcurrentCommandCollector("_1");
                String var_1 = plp_uppaal.local_variable_add();
                String var_5 = plp_uppaal.local_variable_add();
                String var_6 = plp_uppaal.local_variable_add();
                concurrent_commands_1.add_request( (new ConcurrentCommand()).make_read( 30, var_1 ) );
                concurrent_commands_1.add_request( (new ConcurrentCommand()).make_watch_remove( var_0, var_5 ) );
                concurrent_commands_1.add_request( (new ConcurrentCommand()).make_write( 30, var_6 ) );
                concurrent_commands_1.generate_requests( plp_uppaal, "id31", "id35", new Point(-187,-612) );
                */


                List<String> results_variable = add_concurrent_conditions( plp_id, plp_uppaal, plp_achieve.getConcurrentModules(),
                        plp_achieve.getConcurrencyConditions(),
                          null,
                          "id30",   "id34", new Point(-731,-612),
                        "id31", "id35", new Point(-187,-612),
                        plp_achieve.getGoal(), null, null,
                        plp.getRequiredResources(), plp.getSideEffects() );

                add_sorting_of_result_from_concurrent_requests( plp_id, plp_uppaal,
                        "id36", new Point(272,-612), "id23", results_variable );

                plp_uppaal.replace_all_in_plp_template( "[<[plp_repeat_variable]>]", UppaalBuilder.uppaal_variable_read( this.variable_manager.global_variable_get_id( get_plp_repeat_variable( plp_id ) ) ) );
                add_to_modules = plp_uppaal.seal_for_generation();

            } catch ( VerificationException exception ) {
                throw new VerificationException( plp_achieve.getName() + ": " + exception.get_message() );
            }

            String process_format = "%1$s_process = %1$s( %2$d );\n";
            add_to_system_declarations.append( String.format(process_format, plp_uppaal.name, plp_uppaal.get_id() ) );

            this.plp_processes.add( plp_uppaal.name + "_process");
        } else if ( DetectPLP.class.isInstance(plp) ) {
            DetectPLP plp_detect    = (DetectPLP) plp;
            plp_uppaal.insert_plp_template( this.verification_plp_detect );

            try{

                plp_add_success_and_failure_probabilities( plp_id, plp, plp_uppaal );

                List<Condition> fail_termination_conditions = null;
                if ( null != plp_detect.getFailTerminationCond() ) {
                    fail_termination_conditions = new LinkedList<>();
                    fail_termination_conditions.add( plp_detect.getFailTerminationCond() );
                }

                plp_add_success_and_failure_terminations( plp_id, plp, plp_uppaal,
                        null, fail_termination_conditions,
                        "id135",
                         "id129",
                              "id130",
                        new Point( 102, -136 ),
                        null,
                        10 );

                List<String> results_variable = add_concurrent_conditions( plp_id, plp_uppaal, plp_detect.getConcurrentModules(),
                        plp_detect.getConcurrencyConditions(),
                          null,
                          "id131",   "id135", new Point(-119,-136),
                        "id132", "id136", new Point( 578,-136),
                        plp_detect.getGoal(), null, null,
                        plp.getRequiredResources(), plp.getSideEffects() );

                add_sorting_of_result_from_concurrent_requests( plp_id, plp_uppaal,
                        "id137", new Point(1105,-136), "id124", results_variable );

                plp_uppaal.main_success_labels.append(
                        (new UppaalLabel(
                                "invariant",
                                new Point( 102, -106 ),
                                UppaalBuilder.binary_expression( "local_time", UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf(this.xml_to_uppaal_converter.plp_maximum_run_time()) )
                                )).toString() );

                add_to_modules = plp_uppaal.seal_for_generation();

            } catch ( VerificationException exception ) {
                throw new VerificationException( plp_detect.getBaseName() + ": " + exception.get_message() );
            }

            String process_format = "%1$s_process = %1$s( %2$d );\n";
            add_to_system_declarations.append( String.format(process_format, plp_uppaal.name, plp_uppaal.get_id() ) );

            this.plp_processes.add( plp_uppaal.name + "_process");

        } else if ( MaintainPLP.class.isInstance(plp) ) {

            MaintainPLP plp_maintain    = (MaintainPLP) plp;
            plp_uppaal.insert_plp_template( this.verification_plp_maintain );

            try {

                UppaalSubGraphContainer sub_graph_run_time_wait_before_true = new UppaalSubGraphContainer();

                StringBuffer transition_to_wait_maintain_true_labels  = new StringBuffer();
                StringBuffer wait_maintain_true_labels                = new StringBuffer();

                if ( true == plp_maintain.isInitiallyTrue() )
                {
                    wait_maintain_true_labels.append( UppaalLabel.urgent() );
                    transition_to_wait_maintain_true_labels.append("\t\t\t<label kind=\"guard\" x=\"459\" y=\"-170\">( local_time == 0 )</label>\n");
                    sub_graph_run_time_wait_before_true.transitions.add(
                            new UppaalTransition("id63","id64",
                                    null , UppaalBuilder.Side.bottom_center, null, null,
                                    null, null, null ) );
                    sub_graph_run_time_wait_before_true.add_sub_graph( plp_uppaal, new Point(0,0), "" );
                }
                else
                {
                    Point time_distribution_source = new Point (459, -170 );
                    Point time_distribution_target = new Point (578, -170 );
                    Point time_distribution_target_relative_to_source = new Point (
                            time_distribution_target.x - time_distribution_source.x,
                            time_distribution_target.y - time_distribution_source.y );

                    plp_add_time_distribution( plp_id, plp_uppaal, sub_graph_run_time_wait_before_true, plp_maintain.getTimeUntilTrue().get(0),
                            "id63", "id64",
                            transition_to_wait_maintain_true_labels, wait_maintain_true_labels,
                            time_distribution_source,
                            time_distribution_target_relative_to_source );

                    sub_graph_run_time_wait_before_true.add_sub_graph( plp_uppaal, time_distribution_source, "" );
                }

                // [<[wait_maintain_true_labels]>]
                // [<[transition_to_wait_maintain_true_labels]>]
                plp_uppaal.replace_all_in_plp_template("[<[wait_maintain_true_labels]>]"                , wait_maintain_true_labels.toString()               );
                plp_uppaal.replace_all_in_plp_template("[<[transition_to_wait_maintain_true_labels]>]"  , transition_to_wait_maintain_true_labels.toString() );


                UppaalSubGraphContainer sub_graph_run_time_main_success = new UppaalSubGraphContainer();

                Point time_distribution_source = new Point ( 782, -170 );
                Point time_distribution_target = new Point (1190, -170 );
                Point time_distribution_target_relative_to_source = new Point (
                        time_distribution_target.x - time_distribution_source.x,
                        time_distribution_target.y - time_distribution_source.y );

                List<String> created_locations_ids_by_time_distribution = plp_add_time_distribution( plp_id, plp_uppaal, sub_graph_run_time_main_success, (plp_maintain.getSuccessRuntime()).get(0),
                        "id57", "id55",
                        plp_uppaal.transition_to_main_success_labels, plp_uppaal.main_success_labels,
                        time_distribution_source,
                        time_distribution_target_relative_to_source );

                sub_graph_run_time_main_success.add_sub_graph( plp_uppaal, time_distribution_source, "" );

                plp_add_success_and_failure_terminations( plp_id, plp, plp_uppaal,
                        plp_maintain.getSuccessTerminationCondition(),  plp_maintain.getFailureTerminationConditions(),
                        "id72",
                         "id55",
                             "id57",
                        new Point( 782, -170 ),
                        created_locations_ids_by_time_distribution,
                        12 );

                plp_add_success_and_failure_probabilities( plp_id, plp, plp_uppaal );

                List<String> results_variable = add_concurrent_conditions( plp_id, plp_uppaal, plp_maintain.getConcurrentModules(),
                        plp_maintain.getConcurrencyConditions(),
                        plp_maintain.getMaintainedCondition(),
                          "id64",   "id72", new Point( 595,-170),
                        "id65", "id73", new Point(1275,-170),
                        null, null, null,
                        plp.getRequiredResources(), plp.getSideEffects() );

                add_sorting_of_result_from_concurrent_requests( plp_id, plp_uppaal,
                        "id75", new Point(1649,-170), "id56", results_variable );


                add_to_modules = plp_uppaal.seal_for_generation();

            } catch ( VerificationException exception ) {
                throw new VerificationException( plp_maintain.getBaseName() + ": " + exception.get_message() );
            }

            String process_format = "%1$s_process = %1$s( %2$d );\n";
            add_to_system_declarations.append( String.format(process_format, plp_uppaal.name, plp_uppaal.get_id() ) );

            this.plp_processes.add( plp_uppaal.name + "_process");
        } else if ( ObservePLP.class.isInstance(plp) ) {

            ObservePLP plp_observe      = (ObservePLP) plp;
            plp_uppaal.insert_plp_template( this.verification_plp_observe );

            try{

                UppaalSubGraphContainer sub_graph_run_time_main_success = new UppaalSubGraphContainer();


                Point time_distribution_source = new Point ( -76, -272 );
                Point time_distribution_target = new Point ( 214, -272 );
                Point time_distribution_target_relative_to_source = new Point (
                        time_distribution_target.x - time_distribution_source.x,
                        time_distribution_target.y - time_distribution_source.y );

                List<String> created_locations_ids_by_time_distribution = plp_add_time_distribution( plp_id, plp_uppaal, sub_graph_run_time_main_success, (plp_observe.getSuccessRuntime()).get(0),
                        "id96", "id94",
                        plp_uppaal.transition_to_main_success_labels, plp_uppaal.main_success_labels,
                        time_distribution_source,
                        time_distribution_target_relative_to_source );

                sub_graph_run_time_main_success.add_sub_graph( plp_uppaal, time_distribution_source, "" );

                plp_add_success_and_failure_probabilities( plp_id, plp, plp_uppaal );

                List<Condition> fail_termination_conditions = null;
                if ( null != plp_observe.getFailTerminationCond() ) {
                    fail_termination_conditions = new LinkedList<>();
                    fail_termination_conditions.add( plp_observe.getFailTerminationCond() );
                }

                plp_add_success_and_failure_terminations( plp_id, plp, plp_uppaal,
                        null,  fail_termination_conditions,
                        "id103",
                         "id104",
                             "id96",
                        new Point( -76, -272 ),
                        created_locations_ids_by_time_distribution,
                        12 );

                // plp_observe_add_observations( plp_id, plp_observe, plp_uppaal );

                List<Integer> return_observation_variable_id = new LinkedList<>();
                List<String> observation_assignments = plp_observation_to_assignments( plp_id, plp_observe, return_observation_variable_id );
                /*
                plp_uppaal.transitions.append("\t\t<transition>\n" +
                    "\t\t\t<source ref=\"id94\"/>\n" +
                    "\t\t\t<target ref=\"id106\"/>\n" +
                    "\t\t\t<label kind=\"assignment\" x=\"151\" y=\"" + String.valueOf(-399 + (i * 20)) + "\">" + UppaalBuilder.binary_expression_enclosed( variable_uppaal, UppaalBuilder.STR_ASSIGNMENT, sample_value.toString() ) + "</label>\n" +
                    "\t\t\t<nail x=\"262\" y=\"" + String.valueOf(-374 + (i * 20)) + "\"/>\n" +
                    "\t\t\t<nail x=\"483\" y=\"" + String.valueOf(-374 + (i * 20)) + "\"/>\n" +
                    "\t\t</transition>\n");*/
                /*
                List<UppaalTransition> transitions = generate_nondeterministic_transitions(
                        "id94" , new Point(214,-272),
                        "id106", new Point(551,-272),
                        "",
                        observation_assignments);

                for ( UppaalTransition transition : transitions )
                {
                    transition.add_transition(plp_uppaal);
                }
                */

                List<String> results_variable = add_concurrent_conditions( plp_id, plp_uppaal, plp_observe.getConcurrentModules(),
                        plp_observe.getConcurrencyConditions(),
                          null,
                          "id102",   "id103", new Point(-212,-272),
                        "id104", "id105", new Point( 704,-272),
                        null, observation_assignments, return_observation_variable_id,
                        plp.getRequiredResources(), plp.getSideEffects() );

                add_sorting_of_result_from_concurrent_requests( plp_id, plp_uppaal,
                        "id107", new Point(1129,-272), "id93", results_variable );

                plp_uppaal.replace_all_in_plp_template( "[<[plp_repeat_variable]>]", UppaalBuilder.uppaal_variable_read( this.variable_manager.global_variable_get_id( get_plp_repeat_variable( plp_id ) ) ) );

                add_to_modules = plp_uppaal.seal_for_generation();

            } catch ( VerificationException exception ) {
                throw new VerificationException( plp_observe.getBaseName() + ": " + exception.get_message() );
            }

            String process_format = "%1$s_process = %1$s( %2$d );\n";
            add_to_system_declarations.append( String.format( process_format, plp_uppaal.name, plp_uppaal.get_id() ) );

            this.plp_processes.add( plp_uppaal.name + "_process");
        } else {
            assert false : "uppaal_file_add_plp - Unknown PLP type.";
        }

        uppaal_file_add_to_declarations(add_to_declarations);
        uppaal_file_add_to_modules( add_to_modules );
        uppaal_file_add_to_system_declarations(add_to_system_declarations);
    }

    public void generate_processes_update() {
        // Processes update
        StringBuffer processes_string = new StringBuffer("system concurrent_process, control_graph_init, ");
        int processes_last_index = this.plp_processes.size() - 1;
        for (int i = 0; i <= processes_last_index; i++) {
            processes_string.append( this.plp_processes.get(i) );
            if (i < processes_last_index) {
                processes_string.append( ", " );
                if ( 0 == (i % 3) )
                {
                    processes_string.append( "\n       " );
                }
            }
        }
        processes_string.append( ";" );
        uppaal_file_add_to_system_declarations(processes_string);
    }

    public void generate_variables_update( int processes_amount ) {

        StringBufferExtra.replace( verification_declarations_start, "[<[processes_amount]>]", String.format("%d", processes_amount) );

        int concurrent_data_amount = this.variable_manager.get_global_variables_amount();
        StringBufferExtra.replace( verification_declarations_start, "[<[concurrent_data_amount]>]", String.format("%d", concurrent_data_amount) );

        int concurrent_requests_amount = this.variable_manager.concurrent_requests_get();
        StringBufferExtra.replace( verification_declarations_start, "[<[concurrent_requests_amount]>]", String.format("%d", concurrent_requests_amount) );

        int global_variables_amount = this.variable_manager.get_global_variables_amount();
        verification_declarations_start.append("\nvoid initialize_variables(){\n");
        for ( int i = 0 ; i < global_variables_amount ; i++ )
        {
            VerificationVariable variable_data = this.variable_manager.global_variable_get_data( i );

            verification_declarations_start.append( String.format( "// %03d: %s\n", i, UppaalBuilder.comply_string_single_line( this.variable_manager.global_variable_get_name(i) ) ) );
            /*
            if ( VerificationVariable.VerificationVariableType.type_parameter == variable_data.variable_type )
            {
                verification_declarations_start.append( UppaalBuilder.comply_string_single_line( String.format( "<parameter plp_name=\"%s\"   name=\"%s\"   value=\"0\" />\n", this.plp_catalog.find_plp_name_by_id(variable_data.plp_id), this.variable_manager.global_variable_get_name(i) ) ) );
            }else if ( VerificationVariable.VerificationVariableType.type_regular == variable_data.variable_type )
            {
                verification_declarations_start.append( UppaalBuilder.comply_string_single_line( String.format( "<variable  name=\"%s\"  value=\"0\" />\n", this.variable_manager.global_variable_get_name(i) ) ) );
            }
            */
            //verification_declarations_start.append( String.format( "// <variable  name=\"%s\n", i, UppaalBuilder.comply_string_single_line( this.variable_manager.global_variable_get_name(i) ) ) );
            if ( true == variable_data.is_set ){
                verification_declarations_start.append( UppaalBuilder.uppaal_variable_write( i, variable_data.value) + ";\n" );
            }

            if ( true == variable_data.is_in_range ){
                verification_declarations_start.append( UppaalBuilder.uppaal_variable_set_range( i, variable_data.min_value, variable_data.max_value ) + ";\n" );
            }

        }
        verification_declarations_start.append("}\n");
    }

    public List<String> plp_add_time_distribution_by_intervals( UppaalPLP plp_uppaal,
                                                                UppaalSubGraphContainer sub_graph,
                                                                String source_location_id,   String target_location_id,
                                                                StringBuffer labels_at_main,
                                                                Point target_place_relative_to_source,
                                                                List<ProbabilityAndUniformTime> intervals )
    {
        List<String> created_locations_ids = new LinkedList<>();

        final int x_place_squares_from_left_branch      = 1;
        final int x_place_squares_from_left_nail        = 2;
        final int x_place_squares_from_left_location    = 5;
        final int x_place_squares_from_left_guard       = 7;
        final int x_place_squares_from_right_nail       = 2;
        final int y_offset_squares                      = 3;
        int y_current_position_squares                  = 0;


        labels_at_main.append( UppaalLabel.urgent() );

        String location_branchpoint_id = plp_uppaal.get_new_location_id();

        sub_graph.branchpoints.add( new UppaalBranchpoint(location_branchpoint_id,
                new Point(
                        UppaalBuilder.direction_right(UppaalBuilder.squares_length(x_place_squares_from_left_branch)),
                        UppaalBuilder.direction_up(UppaalBuilder.squares_length( 0 )) )) );

        UppaalTransition to_branch = new UppaalTransition( source_location_id, location_branchpoint_id,
                new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_from_left_nail ) ),
                        UppaalBuilder.direction_up(UppaalBuilder.squares_length( 0 ))),
                UppaalBuilder.Side.bottom_center,
                null, null,
                UppaalBuilder.binary_expression( "local_time", UppaalBuilder.STR_ASSIGNMENT, "0" ),
                null, null );
        sub_graph.transitions.add( to_branch );

        for ( ProbabilityAndUniformTime interval : intervals )
        {
            String current_location_name = String.format( "interval_%d_%d", interval.run_time_minimum, interval.run_time_maximum );

            String loc_current_location_id = plp_uppaal.get_new_location_id();

            UppaalLocation loc_current_location = new UppaalLocation( current_location_name,
                    UppaalBuilder.Side.top_center,
                    new Point(
                            UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_from_left_location ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares   ) ) ),
                    "", UppaalBuilder.Side.none,
                    UppaalBuilder.binary_expression( "local_time", UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf( interval.run_time_maximum ) ),
                    UppaalBuilder.Side.bottom_center,
                    false);
            loc_current_location.location_id = loc_current_location_id;
            sub_graph.locations.add( loc_current_location  );

            created_locations_ids.add( loc_current_location_id );

            List<Point> left_nails = new LinkedList<>();
            left_nails.add(
                    new Point(
                            UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_from_left_nail ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ) ) ) );

            UppaalTransition to_current_locatin = new UppaalTransition( location_branchpoint_id, loc_current_location_id,
                    new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_from_left_nail ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ))),
                    UppaalBuilder.Side.top_left,
                    null, null, null,
                    String.valueOf( interval.probability ),
                    left_nails );
            sub_graph.transitions.add( to_current_locatin );

            List<Point> right_nails = new LinkedList<>();
            right_nails.add(
                    new Point(
                            UppaalBuilder.direction_right( target_place_relative_to_source.x -  UppaalBuilder.squares_length( x_place_squares_from_right_nail ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ) ) ) );

            StringBuffer transition_guard = new StringBuffer();

            UppaalBuilder.add_to_cumulative_expression_with_operator( transition_guard,
                    UppaalBuilder.binary_expression_enclosed( String.valueOf( interval.run_time_minimum ),UppaalBuilder.STR_LESS_THAN,"local_time"   ),
                    UppaalBuilder.STR_AND);
            UppaalBuilder.add_to_cumulative_expression_with_operator( transition_guard,
                    UppaalBuilder.binary_expression_enclosed("local_time",    UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf( interval.run_time_maximum )),
                    UppaalBuilder.STR_AND);

            UppaalTransition from_current_locatin = new UppaalTransition( loc_current_location_id, target_location_id,
                    new Point( UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_from_left_guard ) ),
                            UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ))),
                    UppaalBuilder.Side.top_left,
                    transition_guard.toString(),
                    null, null, null,
                    right_nails );
            sub_graph.transitions.add( from_current_locatin );

            y_current_position_squares += y_offset_squares;

        }

        return created_locations_ids;
    }



    public List<String> plp_add_time_distribution( int plp_id, UppaalPLP plp_uppaal, UppaalSubGraphContainer sub_graph,
                                           ConditionalDist run_time_distribution,
                                           String source_location_id,   String target_location_id,
                                           StringBuffer transition_labels_before_main, StringBuffer labels_at_main,
                                           Point source_place_absolute,
                                           Point target_place_relative_to_source  ) throws VerificationException
    {

        Distribution distribution = run_time_distribution.getDist();
        List<String> created_locations_ids = new LinkedList<>();

        if ( UniformDistribution.class.isInstance( run_time_distribution.getDist() ) )
        {
            UniformDistribution distribution_uniform = (UniformDistribution)distribution;
            String variable_bound_lower = plp_uppaal.local_variable_add();
            String variable_bound_upper = plp_uppaal.local_variable_add();

            String label_assignment_content = variable_bound_lower + " = " + this.xml_to_uppaal_converter.convert_xml_formula_to_uppaal( plp_id, distribution_uniform.getLowerBound() ) + ",\n" +
                    variable_bound_upper + " = " + this.xml_to_uppaal_converter.convert_xml_formula_to_uppaal( plp_id, distribution_uniform.getUpperBound() );
            Point label_assignment_place = new Point(
                    source_place_absolute.x + UppaalBuilder.label_center_at_x( UppaalBuilder.direction_left( UppaalBuilder.squares_length(3)), label_assignment_content ),
                    source_place_absolute.y + UppaalBuilder.direction_down(UppaalBuilder.squares_length(1.5)) );

            transition_labels_before_main.append( new UppaalLabel("assignment", label_assignment_place, label_assignment_content  ).toString() );

            Point label_invariant_place = new Point(
                    source_place_absolute.x + UppaalBuilder.label_center_at_x( 0, variable_bound_upper ),
                    source_place_absolute.y + UppaalBuilder.direction_down(UppaalBuilder.squares_length(1)) );


            labels_at_main.append(new UppaalLabel(
                    "invariant",
                    label_invariant_place,
                    UppaalBuilder.binary_expression_enclosed("local_time", UppaalBuilder.STR_LESS_THAN_EQUAL, variable_bound_upper ) ).toString());

            Point label_guard_place = new Point( (target_place_relative_to_source.x) / 2,
                    UppaalBuilder.direction_up(UppaalBuilder.squares_length(1)) );

            StringBuffer transition_guard = new StringBuffer();

            UppaalBuilder.add_to_cumulative_expression_with_operator( transition_guard,
                    UppaalBuilder.binary_expression_enclosed(variable_bound_lower,UppaalBuilder.STR_LESS_THAN_EQUAL,"local_time"   ),
                    UppaalBuilder.STR_AND);
            UppaalBuilder.add_to_cumulative_expression_with_operator( transition_guard,
                    UppaalBuilder.binary_expression_enclosed("local_time",    UppaalBuilder.STR_LESS_THAN_EQUAL,variable_bound_upper),
                    UppaalBuilder.STR_AND);

            sub_graph.transitions.add( new UppaalTransition( source_location_id, target_location_id,
                                                new Point( label_guard_place.x, label_guard_place.y ), UppaalBuilder.Side.bottom_center,
                                                transition_guard.toString(),
                                                "", "", "",
                                                null ) );


        }
        else if ( ExpDistribution.class.isInstance( run_time_distribution.getDist() ) )
        {
            ExpDistribution distribution_exp    = (ExpDistribution)distribution;
            String          variable_exp_lambda = plp_uppaal.local_variable_add();

            labels_at_main.append(
                    (new UppaalLabel(
                            "invariant",
                            new Point( source_place_absolute.x, source_place_absolute.y + UppaalBuilder.units_letter_hight ),
                            UppaalBuilder.binary_expression( "local_time", UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf(this.xml_to_uppaal_converter.plp_maximum_run_time()) )
                    )).toString() );


            String label_assignment_content = UppaalBuilder.binary_expression(
                    variable_exp_lambda,
                    UppaalBuilder.STR_ASSIGNMENT,
                    this.xml_to_uppaal_converter.convert_xml_formula_to_uppaal( plp_id, distribution_exp.getLambda() ));

            Point label_assignment_place = new Point(
                    source_place_absolute.x + UppaalBuilder.label_center_at_x( UppaalBuilder.direction_left( UppaalBuilder.squares_length(1)), label_assignment_content ),
                    source_place_absolute.y + UppaalBuilder.direction_down(UppaalBuilder.squares_length(1.5)) );


            transition_labels_before_main.append(new UppaalLabel("assignment", label_assignment_place, label_assignment_content ).toString());


            Point label_exponentialrate_place = new Point(
                    source_place_absolute.x + UppaalBuilder.label_center_at_x( 0, variable_exp_lambda ),
                    source_place_absolute.y + UppaalBuilder.direction_down(UppaalBuilder.squares_length(1)) );

            labels_at_main.append(new UppaalLabel("exponentialrate", label_exponentialrate_place, variable_exp_lambda ).toString());


            sub_graph.transitions.add( new UppaalTransition( source_location_id, target_location_id,
                    new Point( 0, 0 ), UppaalBuilder.Side.bottom_left,
                    "","", "", "", null ) );
        }
        else if ( NormalDistribution.class.isInstance( run_time_distribution.getDist() ) )
        {
            NormalDistribution run_time_normal = (NormalDistribution)run_time_distribution.getDist();



            List<ProbabilityAndUniformTime> intervals = new LinkedList<>();

            int    intervals_amount         = this.settings.get_int( settings.RUN_TIME_AMOUNT_OF_INTERVALS_FOR_DISCRETIZATION );
            double mean                     = Double.valueOf( run_time_normal.getMean() );
            double variance                 = Double.valueOf( run_time_normal.getVariance() );
            double standard_deviation       = Math.sqrt(variance);
            double interval_of_significance =  3 * standard_deviation;
            double range_start              = mean - interval_of_significance;
            double range_end                = mean + interval_of_significance;
            double interval_size            = (range_end - range_start) / intervals_amount;
            double interval_start           = range_start;
            double interval_end             = range_start + interval_size;

            org.apache.commons.math3.distribution.NormalDistribution normal_distribution = new org.apache.commons.math3.distribution.NormalDistribution( mean, standard_deviation );


            for ( int i = 0 ; i < intervals_amount ; i++ )
            {
                double probability = normal_distribution.cumulativeProbability(interval_end) - normal_distribution.cumulativeProbability(interval_start);

                intervals.add(new ProbabilityAndUniformTime(
                        this.xml_to_uppaal_converter.convert_xml_double_to_uppaal_int(probability),
                        this.xml_to_uppaal_converter.convert_xml_double_to_uppaal_int(interval_start),
                        this.xml_to_uppaal_converter.convert_xml_double_to_uppaal_int(interval_end)));

                interval_start   = interval_end;
                interval_end    += interval_size;
            }

            created_locations_ids = plp_add_time_distribution_by_intervals(
                    plp_uppaal,
                    sub_graph,
                    source_location_id, target_location_id,
                    labels_at_main,
                    target_place_relative_to_source,
                    intervals );

        }
        else if ( GammaDistribution.class.isInstance( run_time_distribution.getDist() ) )
        {
            GammaDistribution run_time_gamma = (GammaDistribution) run_time_distribution.getDist();

            int intervals_amount = this.settings.get_int( settings.RUN_TIME_AMOUNT_OF_INTERVALS_FOR_DISCRETIZATION );

            org.apache.commons.math3.distribution.GammaDistribution gamma_distribution = new org.apache.commons.math3.distribution.GammaDistribution(
                    Double.valueOf( run_time_gamma.getK_shape() ), Double.valueOf( run_time_gamma.getAlpha_shape() ) );


            List<ProbabilityAndUniformTime> intervals = new LinkedList<>();

            double mean                     = gamma_distribution.getNumericalMean();
            double variance                 = gamma_distribution.getNumericalVariance() * gamma_distribution.getScale();
            double standard_deviation       = Math.sqrt(variance);
            double interval_of_significance =  3 * standard_deviation;
            double range_start              = mean - interval_of_significance;
            if ( range_start < 0 )
            {
                range_start = 0;
            }
            double range_end                = mean + interval_of_significance;
            double interval_size            = (range_end - range_start) / intervals_amount;
            double interval_start           = range_start;
            double interval_end             = range_start + interval_size;

            for ( int i = 0 ; i < intervals_amount ; i++ )
            {
                double probability = gamma_distribution.cumulativeProbability(interval_end) - gamma_distribution.cumulativeProbability(interval_start);

                intervals.add(new ProbabilityAndUniformTime(
                        this.xml_to_uppaal_converter.convert_xml_double_to_uppaal_int(probability),
                        this.xml_to_uppaal_converter.convert_xml_double_to_uppaal_int(interval_start),
                        this.xml_to_uppaal_converter.convert_xml_double_to_uppaal_int(interval_end)));

                interval_start   = interval_end;
                interval_end    += interval_size;
            }

            created_locations_ids = plp_add_time_distribution_by_intervals(
                    plp_uppaal,
                    sub_graph,
                    source_location_id, target_location_id,
                    labels_at_main,
                    target_place_relative_to_source,
                    intervals );
        }

        return created_locations_ids;
    }
/*

    public void plp_add_success_termination(int plp_id, PLP plp, UppaalPLP plp_uppaal, Condition success_termination_condition, boolean is_detect ) throws VerificationException
    {
        Set<Integer> success_termination_variables = new HashSet<>();

        if ( false == is_detect ) {

            xml_to_uppaal_converter.convert_xml_condition_to_uppaal(plp_id, plp_uppaal.termination_success_condition, success_termination_condition, success_termination_variables);

            int base_y          = -119;
            int success_case    = 0;
            for (Integer success_termination_variable : success_termination_variables)
            {
                int value_y = base_y + (20 * success_case);

                plp_uppaal.termination_success_notify_transitions.append( "\t\t<transition>\n" +
                        "\t\t\t<source ref=\"id57\"/>\n" +
                        "\t\t\t<target ref=\"id55\"/>\n" +
                        "\t\t\t<label kind=\"guard\" x=\"833\" y=\"" + String.valueOf(value_y + 0) + "\">");
                plp_uppaal.termination_success_notify_transitions.append( plp_uppaal.termination_success_condition );
                plp_uppaal.termination_success_notify_transitions.append( "</label>\n" +
                        "\t\t\t<label kind=\"synchronisation\" x=\"833\" y=\"" + String.valueOf(value_y + 17) + "\">concurrent_notify[" + success_termination_variable.toString() + "]?</label>\n" +
                        "\t\t\t<nail x=\"824\" y=\"" + String.valueOf(value_y + 34) + "\"/>\n" +
                        "\t\t\t<nail x=\"1351\" y=\"" + String.valueOf(value_y + 34) + "\"/>\n" +
                        "\t\t\t<nail x=\"1351\" y=\"" + String.valueOf(value_y + 0) + "\"/>\n" +
                        "\t\t</transition>\n");

                success_case++;
            }
        }
        else
        {
            plp_uppaal.termination_success_condition.append( UppaalBuilder.STR_FALSE );
        }
    }

    public void plp_add_failure_termination(int plp_id, PLP plp, UppaalPLP plp_uppaal, Condition failure_termination_condition, boolean is_detect  ) throws VerificationException
    {
        List<Condition> list = new LinkedList<>();
        list.add(failure_termination_condition);
        plp_add_failure_terminations( plp_id, plp, plp_uppaal, list, is_detect );
    }

    public void plp_add_failure_terminations(int plp_id, PLP plp, UppaalPLP plp_uppaal, List<Condition> failure_termination_conditions, boolean is_detect ) throws VerificationException
    {

        if ( false == is_detect ) {
            if (null != failure_termination_conditions && false == failure_termination_conditions.isEmpty()) {
                Set<Integer> failure_termination_variables = new HashSet<>();

                plp_uppaal.termination_failure_condition = xml_to_uppaal_converter.convert_xml_conditions_to_uppaal( plp_id, failure_termination_conditions, UppaalBuilder.STR_OR, failure_termination_variables);

                for (Integer failure_termination_variable : failure_termination_variables) {
                    plp_uppaal.termination_failure_notify_transitions.append( "\t\t<transition>\n" +
                            "\t\t\t<source ref=\"id57\"/>\n" +
                            "\t\t\t<target ref=\"id58\"/>\n" +
                            "\t\t\t<label kind=\"guard\" x=\"816\" y=\"-17\">");
                    plp_uppaal.termination_failure_notify_transitions.append( plp_uppaal.termination_failure_condition );
                    plp_uppaal.termination_failure_notify_transitions.append( "</label>\n" +
                            "\t\t\t<label kind=\"synchronisation\" x=\"816\" y=\"9\">concurrent_notify[" + failure_termination_variable.toString() + "]?</label>\n" +
                            "\t\t\t<nail x=\"816\" y=\"34\"/>\n" +
                            "\t\t</transition>\n" );
                }
            }
            else
            {
                plp_uppaal.termination_failure_condition.append( UppaalBuilder.STR_FALSE );
            }
        }

    }

    public void plp_add_success_and_failure_terminations(int plp_id, PLP plp, UppaalPLP plp_uppaal, Condition success_termination_condition, List<Condition> failure_termination_conditions ) throws VerificationException
    {

        plp_add_success_termination( plp_id, plp, plp_uppaal, success_termination_condition, false );
        plp_add_failure_terminations( plp_id, plp, plp_uppaal, failure_termination_conditions, false );
    }
*/
    public void plp_add_success_and_failure_terminations( int plp_id, PLP plp, UppaalPLP plp_uppaal,
                                                          Condition success_termination_condition,
                                                          List<Condition> failure_termination_conditions,
                                                          String location_before_main_id,
                                                          String location_main_done_id,
                                                          String location_main_id,
                                                          Point main_place,
                                                          List<String> location_ids_for_time_pass,
                                                          int width_in_squares ) throws VerificationException
    {
        UppaalSubGraphContainer sub_graph = new UppaalSubGraphContainer();

        final int x_distance_to_check_termination       = 4;
        final int x_distance_from_left_to_left_nail     = 2;
        final int x_distance_from_right_to_right_nail   = 2;
        final int y_offset_squares                      = 2;
        int y_current_position_squares                  = y_offset_squares;

        List<String> locations_ids_we_can_terminate_from = new LinkedList<>();
        if ( null != location_ids_for_time_pass ) {
            locations_ids_we_can_terminate_from.addAll(location_ids_for_time_pass);
        }
        locations_ids_we_can_terminate_from.add( location_main_id );

        UppaalLocation loc_check_termination = new UppaalLocation( "check_termination",
                UppaalBuilder.Side.top_center,
                new Point(
                        UppaalBuilder.direction_left( UppaalBuilder.squares_length( x_distance_to_check_termination ) ),
                        0 ),
                "", UppaalBuilder.Side.none,
                "", UppaalBuilder.Side.none,
                true );
        sub_graph.locations.add( loc_check_termination  );

        UppaalTransition to_check_termination = new UppaalTransition( location_before_main_id, "check_termination",
                new Point( UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_distance_to_check_termination ) ),
                        UppaalBuilder.direction_down(UppaalBuilder.squares_length( 0 ))),
                UppaalBuilder.Side.bottom_center,
                null,
                null, null, null,
                null );
        sub_graph.transitions.add( to_check_termination );

        String failure_termination_id = plp_uppaal.get_new_location_id();


        UppaalTransition to_main = new UppaalTransition( "check_termination", location_main_id,
                new Point( UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_distance_to_check_termination / 2 ) ),
                        UppaalBuilder.direction_down(UppaalBuilder.squares_length( 0 ))),
                UppaalBuilder.Side.bottom_center,
                null,
                null, null, null,
                null,
                "[<[transition_to_main_success_labels]>]");
        sub_graph.transitions.add( to_main );


        if ( null != success_termination_condition ) {
            Set<Integer> success_termination_variables = new HashSet<>();

            StringBuffer termination_success_condition = new StringBuffer();


            xml_to_uppaal_converter.convert_xml_condition_to_uppaal(plp_id, termination_success_condition, success_termination_condition, success_termination_variables);

            termination_success_condition.insert( 0, "// " + UppaalBuilder.comply_string_single_line( success_termination_condition.toString()  ) + "\n" );


            List<Point> nails_from_check_termination_with_success_termination = new LinkedList<>();
            nails_from_check_termination_with_success_termination.add(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(x_distance_from_left_to_left_nail)),
                            UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))));
            nails_from_check_termination_with_success_termination.add(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(width_in_squares - x_distance_from_right_to_right_nail)),
                            UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))));


            UppaalTransition from_check_termination_with_success_termination = new UppaalTransition(
                    "check_termination", location_main_done_id,
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(width_in_squares / 2)),
                            UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))),
                    UppaalBuilder.Side.bottom_center,
                    termination_success_condition.toString(),
                    null, null, null,
                    nails_from_check_termination_with_success_termination);
            sub_graph.transitions.add(from_check_termination_with_success_termination);

            y_current_position_squares += y_offset_squares;

            for (Integer success_termination_variable : success_termination_variables) {
            /*
            plp_uppaal.termination_failure_notify_transitions.append( "\t\t<transition>\n" +
                    "\t\t\t<source ref=\"id57\"/>\n" +
                    "\t\t\t<target ref=\"id58\"/>\n" +
                    "\t\t\t<label kind=\"guard\" x=\"816\" y=\"-17\">");
            plp_uppaal.termination_failure_notify_transitions.append( termination_failure_condition );
            plp_uppaal.termination_failure_notify_transitions.append( "</label>\n" +
                    "\t\t\t<label kind=\"synchronisation\" x=\"816\" y=\"9\">concurrent_notify[" + failure_termination_variable.toString() + "]?</label>\n" +
                    "\t\t\t<nail x=\"816\" y=\"34\"/>\n" +
                    "\t\t</transition>\n" );
            */

                for ( String locations_id_we_can_terminate_from : locations_ids_we_can_terminate_from ) {
                    List<Point> nails_current_termination_variable = new LinkedList<>();
                    nails_current_termination_variable.add(
                            new Point( UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_distance_from_left_to_left_nail ) ),
                                    UppaalBuilder.direction_down(UppaalBuilder.squares_length( y_current_position_squares ))) );
                    nails_current_termination_variable.add(
                            new Point( UppaalBuilder.direction_right( UppaalBuilder.squares_length(  width_in_squares - x_distance_from_right_to_right_nail ) ),
                                    UppaalBuilder.direction_down(UppaalBuilder.squares_length( y_current_position_squares ))) );


                    UppaalTransition from_main_with_success_termination_for_current_termination_variable = new UppaalTransition(
                            locations_id_we_can_terminate_from, location_main_done_id,
                            new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(width_in_squares / 2)),
                                    UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))),
                            UppaalBuilder.Side.bottom_center,
                            termination_success_condition.toString(),
                            "concurrent_notify[" + success_termination_variable.toString() + "]?",
                            null, null,
                            nails_current_termination_variable);
                    sub_graph.transitions.add(from_main_with_success_termination_for_current_termination_variable);
                }

                y_current_position_squares += y_offset_squares;
            }

        }






        if ( null != failure_termination_conditions ) {


            Set<Integer> failure_termination_variables = new HashSet<>();

            StringBuffer termination_failure_condition = xml_to_uppaal_converter.convert_xml_conditions_to_uppaal(plp_id, failure_termination_conditions, UppaalBuilder.STR_OR, failure_termination_variables);

            termination_failure_condition.insert( 0, "// " + UppaalBuilder.comply_string_single_line( failure_termination_conditions.toString()  ) + "\n" );


            for (Integer failure_termination_variable : failure_termination_variables) {
            /*
            plp_uppaal.termination_failure_notify_transitions.append( "\t\t<transition>\n" +
                    "\t\t\t<source ref=\"id57\"/>\n" +
                    "\t\t\t<target ref=\"id58\"/>\n" +
                    "\t\t\t<label kind=\"guard\" x=\"816\" y=\"-17\">");
            plp_uppaal.termination_failure_notify_transitions.append( termination_failure_condition );
            plp_uppaal.termination_failure_notify_transitions.append( "</label>\n" +
                    "\t\t\t<label kind=\"synchronisation\" x=\"816\" y=\"9\">concurrent_notify[" + failure_termination_variable.toString() + "]?</label>\n" +
                    "\t\t\t<nail x=\"816\" y=\"34\"/>\n" +
                    "\t\t</transition>\n" );
            */

                for (String locations_id_we_can_terminate_from : locations_ids_we_can_terminate_from) {
                    List<Point> nails_current_termination_variable = new LinkedList<>();
                    nails_current_termination_variable.add(
                            new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(x_distance_from_left_to_left_nail)),
                                    UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))));
                    nails_current_termination_variable.add(
                            new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(width_in_squares - x_distance_from_right_to_right_nail)),
                                    UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))));


                    UppaalTransition from_main_with_failure_termination_for_curent_termination_variable = new UppaalTransition(
                            locations_id_we_can_terminate_from, failure_termination_id,
                            new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(width_in_squares / 2)),
                                    UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))),
                            UppaalBuilder.Side.bottom_center,
                            termination_failure_condition.toString(),
                            "concurrent_notify[" + failure_termination_variable.toString() + "]?",
                            null, null,
                            nails_current_termination_variable);
                    sub_graph.transitions.add(from_main_with_failure_termination_for_curent_termination_variable);
                }
                y_current_position_squares += y_offset_squares;
            }


            UppaalLocation loc_failure_termination = new UppaalLocation("failure_termination",
                    UppaalBuilder.Side.bottom_center,
                    new Point(
                            UppaalBuilder.direction_right(UppaalBuilder.squares_length(width_in_squares)),
                            UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))),
                    "", UppaalBuilder.Side.none,
                    "", UppaalBuilder.Side.none,
                    false);
            loc_failure_termination.location_id = failure_termination_id;
            sub_graph.locations.add(loc_failure_termination);


            List<Point> nails_from_check_termination_with_failure_termination = new LinkedList<>();
            nails_from_check_termination_with_failure_termination.add(
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(x_distance_from_left_to_left_nail)),
                            UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))));

            UppaalTransition from_check_termination_with_failure_termination = new UppaalTransition(
                    "check_termination", failure_termination_id,
                    new Point(UppaalBuilder.direction_right(UppaalBuilder.squares_length(width_in_squares / 2)),
                            UppaalBuilder.direction_down(UppaalBuilder.squares_length(y_current_position_squares))),
                    UppaalBuilder.Side.bottom_center,
                    termination_failure_condition.toString(),
                    null, null, null,
                    nails_from_check_termination_with_failure_termination);
            sub_graph.transitions.add(from_check_termination_with_failure_termination);
        }

        sub_graph.add_sub_graph( plp_uppaal, main_place, "" );
        /*

            for ( ProbabilityAndUniformTime interval : intervals )
            {
                String current_location_name = String.format( "interval_%d_%d", interval.run_time_minimum, interval.run_time_maximum );


                UppaalLocation loc_current_location = new UppaalLocation( current_location_name,
                        UppaalBuilder.Side.top_center,
                        new Point(
                                UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_from_left_location ) ),
                                UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares   ) ) ),
                        "", UppaalBuilder.Side.none,
                        UppaalBuilder.binary_expression( "local_time", UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf( interval.run_time_maximum ) ),
                        UppaalBuilder.Side.bottom_center,
                        false);
                sub_graph.locations.add( loc_current_location  );


                List<Point> left_nails = new LinkedList<>();
                left_nails.add(
                        new Point(
                                UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_from_left_nail ) ),
                                UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ) ) ) );

                UppaalTransition to_current_locatin = new UppaalTransition( location_branchpoint_id, current_location_name,
                        new Point(UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_from_left_nail ) ),
                                UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ))),
                        UppaalBuilder.Side.top_left,
                        null, null, null,
                        String.valueOf( interval.probability ),
                        left_nails );
                sub_graph.transitions.add( to_current_locatin );

                List<Point> right_nails = new LinkedList<>();
                right_nails.add(
                        new Point(
                                UppaalBuilder.direction_right( target_place_relative_to_source.x -  UppaalBuilder.squares_length( x_place_squares_from_right_nail ) ),
                                UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ) ) ) );

                StringBuffer transition_guard = new StringBuffer();

                UppaalBuilder.add_to_cumulative_expression_with_operator( transition_guard,
                        UppaalBuilder.binary_expression_enclosed( String.valueOf( interval.run_time_minimum ),UppaalBuilder.STR_LESS_THAN,"local_time"   ),
                        UppaalBuilder.STR_AND);
                UppaalBuilder.add_to_cumulative_expression_with_operator( transition_guard,
                        UppaalBuilder.binary_expression_enclosed("local_time",    UppaalBuilder.STR_LESS_THAN_EQUAL, String.valueOf( interval.run_time_maximum )),
                        UppaalBuilder.STR_AND);

                UppaalTransition from_current_locatin = new UppaalTransition( current_location_name, target_location_id,
                        new Point( UppaalBuilder.direction_right( UppaalBuilder.squares_length( x_place_squares_from_left_guard ) ),
                                UppaalBuilder.direction_up(UppaalBuilder.squares_length( y_current_position_squares ))),
                        UppaalBuilder.Side.top_left,
                        transition_guard.toString(),
                        null, null, null,
                        right_nails );
                sub_graph.transitions.add( from_current_locatin );

                case_counter++;
                y_current_position_squares += y_offset_squares;

            }
            */
    }


    public void add_control_graph( ControlGraph control_graph )throws VerificationException
    {
        for ( ControlNodeInterface control_node : control_graph.control_nodes ){
            UppaalControlNode uppaal_control_node = new UppaalControlNode( control_node, control_graph, this.plp_catalog,
                                                                           this.xml_to_uppaal_converter, this.variable_manager );
            UppaalPTA uppaal_pta_for_control_node = uppaal_control_node.generate();
            uppaal_file_add_to_modules( uppaal_pta_for_control_node.seal_pta() );

            String process_format = "%1$s_process";
            String process_name = String.format( process_format, control_node.get_node_name() );
            String process_init_format = "%1$s = %2$s( %3$d, %4$d );\n";
            uppaal_file_add_to_system_declarations( String.format( process_init_format, process_name, control_node.get_node_name(), control_node.get_node_id(), control_node.get_concurrent_process_id() ) );

            this.plp_processes.add( process_name );
        }

        StringBufferExtra.replace_all( this.verification_declarations_start, "[<[CONTROL_NODES_AMOUNT]>]",         String.valueOf( control_graph.contron_nodes_amount() ) );
        int transitions_amount = control_graph.contron_transitions_amount();
        if ( 0 == transitions_amount )
        {
            transitions_amount = 1;
        }
        StringBufferExtra.replace_all( this.verification_declarations_start, "[<[CONTROL_NODES_CAN_RUN_AMOUNT]>]", String.valueOf( transitions_amount ) );
        StringBufferExtra.replace_all( this.verification_modules_end, "[<[CONTROL_NODE_ROOT_CHANNEL]>]", UppaalSystem.uppaal_sync_signal_send( UppaalControlNode.uppaal_channel_notify_by_id( control_graph.get_root_id() ) ) );
    }

    private class ProbabilityAndUniformTime {
        public int probability;
        public int run_time_minimum;
        public int run_time_maximum;

        public ProbabilityAndUniformTime(){}
        public ProbabilityAndUniformTime( int probability, int run_time_minimum, int run_time_maximum )
        {
            this.probability        = probability;
            this.run_time_minimum   = run_time_minimum;
            this.run_time_maximum   = run_time_maximum;
        }
    }
}
