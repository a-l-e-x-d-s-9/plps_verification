package verification;

import conditions.*;
import distributions.ConditionalDist;
import plpEtc.Predicate;
import plpEtc.Range;
import plpFields.ConditionalProb;
import plpFields.FailureMode;
import plpFields.PLPParameter;

import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by alexds9 on 09/06/17.
 */
public class XMLtoUppaalConverter {

    public static final String STR_XML_TRUE                = "TRUE";
    public static final String STR_XML_FALSE               = "FALSE";
    public static final String STR_XML_EQUAL               = "=";
    public static final String STR_XML_NOT_EQUAL           = "!=";
    public static final String STR_XML_LESS_THAN           = "<";
    public static final String STR_XML_LESS_THAN_EQUAL     = "<=";
    public static final String STR_XML_GREATER_THAN        = ">";
    public static final String STR_XML_GREATER_THAN_EQUAL  = ">=";
    public static final HashMap<String, String> operators_xml_to_uppaal = new HashMap() {
        {
            put( STR_XML_EQUAL              ,   UppaalBuilder.STR_EQUAL               );
            put( STR_XML_NOT_EQUAL          ,   UppaalBuilder.STR_NOT_EQUAL           );
            put( STR_XML_LESS_THAN          ,   UppaalBuilder.STR_LESS_THAN           );
            put( STR_XML_LESS_THAN_EQUAL    ,   UppaalBuilder.STR_LESS_THAN_EQUAL     );
            put( STR_XML_GREATER_THAN       ,   UppaalBuilder.STR_GREATER_THAN        );
            put( STR_XML_GREATER_THAN_EQUAL ,   UppaalBuilder.STR_GREATER_THAN_EQUAL  );
        }
    };
    public static final HashMap<String, String> operators_xml_mirror = new HashMap() {
        {
            put( STR_XML_EQUAL              ,   STR_XML_EQUAL               );
            put( STR_XML_NOT_EQUAL          ,   STR_XML_NOT_EQUAL           );
            put( STR_XML_LESS_THAN          ,   STR_XML_GREATER_THAN        );
            put( STR_XML_LESS_THAN_EQUAL    ,   STR_XML_GREATER_THAN_EQUAL  );
            put( STR_XML_GREATER_THAN       ,   STR_XML_LESS_THAN           );
            put( STR_XML_GREATER_THAN_EQUAL ,   STR_XML_LESS_THAN_EQUAL     );
        }
    };

    VerificationVariableManager variable_manager;
    VerificationSettings        settings;
    VerificationReports         reports;

    public XMLtoUppaalConverter( VerificationVariableManager variable_manager, VerificationSettings settings,
                                 VerificationReports reports )
    {
        this.variable_manager   = variable_manager;
        this.settings           = settings;
        this.reports            = reports;
    }

    public static boolean xml_is_numeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public static boolean xml_is_double(String str) {
        return str.matches("-?\\d+\\.\\d+");
    }

    public static boolean xml_is_integer(String str) {
        return str.matches("-?\\d+");
    }

    public static boolean xml_is_boolean(String str) {
        return str.matches("(" + STR_XML_TRUE + "|" + STR_XML_FALSE + ")");
    }

    public static boolean xml_is_value_true(String str) {
        return str.matches("(" + STR_XML_TRUE + ")");
    }

    public static boolean xml_is_mathematical_expression(String str) {

        return str.matches( ".+(-|\\+|/|\\*).+" );

    }

    public static boolean xml_is_numeric_or_boolean( String str ) {
        return xml_is_numeric(str) || xml_is_boolean(str);
    }

    // DO NOT USE!!!
    // Use instead use:
    //                  UppaalBuilder.uppaal_variable_read()
    //                  UppaalBuilder.uppaal_variable_write()
    //static public String variable_id_to_uppaal_string(int variable_id )
    //{
    //    return "concurrent_info.concurrent_data[ " + variable_id + " ].value";
    //}

    public int convert_xml_double_to_uppaal_int( double number )
    {
        return (int)( number * this.settings.get_int( this.settings.SETTING_PRECISION_MULTIPLIER_FOR_NUMBERS_AND_TIME ) );
    }

    public int plp_maximum_run_time()
    {
        return convert_xml_double_to_uppaal_int( this.settings.get_int( settings.MAXIMUM_RUN_TIME_OF_PLP ) );
    }

    public int convert_xml_value_to_uppaal_int( String number_string ) throws VerificationException
    {
        if ( xml_is_boolean( number_string ) )
        {
            //throw new VerificationException("\"" + number_string + "\" should be of boolean type.");
            if ( xml_is_value_true( number_string ) )
            {
                return 1;
            }
            else
            {
                return 0;
            }
        }
        else if ( xml_is_numeric( number_string ) )
        {
            return convert_xml_double_to_uppaal_int( Double.parseDouble( number_string ) );
        }
        else
        {
            throw new VerificationException("\"" + number_string + "\" should be of number or boolean type.");
        }
    }
/*
    public String convert_xml_expression_to_uppaal_expression( int plp_id, String xml_expression ) throws VerificationException
    {

        if ( xml_is_mathematical_expression( xml_expression ) )
        {
            return convert_xml_formula_to_uppaal( plp_id, xml_expression );
        }
        else
        {
            return String.valueOf( convert_xml_value_to_uppaal_int( xml_expression ) );
        }
    }
*/
    public String convert_xml_value_to_uppaal_string(String number_string ) throws VerificationException
    {
        return Integer.toString(convert_xml_value_to_uppaal_int(number_string));
    }

    public StringBuffer convert_xml_conditions_to_uppaal(int plp_id, List<Condition> current_conditions, String operator, Set<Integer> uppaal_variables_indexes ) throws VerificationException
    {
        StringBuffer results_string_buffer = new StringBuffer();

        if (null != current_conditions) {

            int conditions_last_amount = current_conditions.size();

            for (int i = 0; i < conditions_last_amount; i++) {

                StringBuffer current_condition_buffer = new StringBuffer();
                convert_xml_condition_to_uppaal(plp_id, current_condition_buffer, current_conditions.get(i), uppaal_variables_indexes);

                UppaalBuilder.add_to_cumulative_expression_with_operator( results_string_buffer, current_condition_buffer, operator );
            }

        }

        if ( 0 != results_string_buffer.length() )
        {
            results_string_buffer.insert( 0, "( " );
            results_string_buffer.append( " )" );
        }

        return results_string_buffer;
    }

    public void convert_xml_condition_to_uppaal( int plp_id, StringBuffer cumulative_results_string, Condition current_condition, Set<Integer> uppaal_variables_indexes ) throws VerificationException
    {

        if ( null == current_condition ){
            return;
        }
        else if ( Formula.class.isInstance(current_condition) )
        {
            Formula formula = (Formula)current_condition;
            String left_side = "";

            if ( formula.getRange() != null ) // Right part containing range
            {
                String variable_name = formula.getLeftExpr();

                if ( this.variable_manager.variable_or_parameter_is_exist( plp_id, variable_name) )
                {
                    int variable_id         = this.variable_manager.variable_or_parameter_get_variable_id( plp_id, variable_name );
                    String variable_uppaal  = UppaalBuilder.uppaal_variable_read(variable_id);
                    Range range             = formula.getRange();
                    String operator_min     = "";
                    String operator_max     = "";
                    VerificationVariable  variable_data = this.variable_manager.global_variable_get_data(variable_id);
                    if (null != uppaal_variables_indexes){
                        uppaal_variables_indexes.add(variable_id);
                    }

                    if ( range.isMinInclusive() )
                    {
                        operator_min = UppaalBuilder.STR_LESS_THAN_EQUAL;
                    }
                    else
                    {
                        operator_min = UppaalBuilder.STR_LESS_THAN;
                    }

                    if ( range.isMaxInclusive() )
                    {
                        operator_max = UppaalBuilder.STR_GREATER_THAN_EQUAL;
                    }
                    else
                    {
                        operator_max = UppaalBuilder.STR_GREATER_THAN;
                    }

                    String min;
                    String max;

                    try {
                        min = convert_xml_expression_to_uppaal_expression( plp_id,  range.getMinValue() );
                        max = convert_xml_expression_to_uppaal_expression( plp_id,  range.getMinValue() );
                    } catch ( VerificationException exception ) {
                        throw new VerificationException( "problem in condition \"" + current_condition.toString() + "\".\n" + exception.get_message() );
                    }

                    UppaalBuilder.add_to_cumulative_condition( cumulative_results_string,
                            UppaalBuilder.binary_expression_enclosed(
                                    UppaalBuilder.binary_expression_enclosed( min, operator_min, variable_uppaal ),
                                    UppaalBuilder.STR_AND,
                                    UppaalBuilder.binary_expression_enclosed( variable_uppaal , operator_max, max ) ) );

                }
                else
                {
                    throw new VerificationException("Formula, variable \"" + variable_name + "\" does not exist.");
                }


            }
            else
            {
                String operand_left_xml     = formula.getLeftExpr();
                String operand_right_xml    = formula.getRightExpr();
                String operand_left_uppaal  = "";
                String operand_right_uppaal = "";
                String operator_uppaal      = "";


                if ( xml_is_numeric_or_boolean( operand_left_xml )   ||
                     xml_is_mathematical_expression( operand_left_xml ) )
                {
                    try {
                        operand_left_uppaal = convert_xml_expression_to_uppaal_expression( plp_id,  operand_left_xml );
                    } catch ( VerificationException exception ) {
                        throw new VerificationException( "problem in condition \"" + current_condition.toString() + "\", can not read left value.\n" + exception.get_message() );
                    }

                }
                else
                {
                    if ( this.variable_manager.variable_or_parameter_is_exist( plp_id, operand_left_xml ) )
                    {
                        int variable_id = this.variable_manager.variable_or_parameter_get_variable_id( plp_id, operand_left_xml);
                        if (null != uppaal_variables_indexes){
                            uppaal_variables_indexes.add(variable_id);
                        }
                        operand_left_uppaal  = UppaalBuilder.uppaal_variable_read( variable_id );
                    }
                    else
                    {
                        throw new VerificationException("Formula, variable \"" + operand_left_xml + "\" does not exist.");
                    }
                }


                if ( xml_is_numeric_or_boolean( operand_right_xml )  ||
                     xml_is_mathematical_expression( operand_left_xml ) )
                {
                    try {
                        operand_right_uppaal = convert_xml_expression_to_uppaal_expression( plp_id, operand_right_xml );
                    } catch ( VerificationException exception ) {
                        throw new VerificationException( "problem in condition \"" + current_condition.toString() + "\", can not read right value.\n" + exception.get_message() );
                    }
                }
                else
                {
                    if ( this.variable_manager.variable_or_parameter_is_exist( plp_id, operand_right_xml) )
                    {
                        int variable_id = this.variable_manager.variable_or_parameter_get_variable_id( plp_id, operand_right_xml);
                        if (null != uppaal_variables_indexes){
                            uppaal_variables_indexes.add(variable_id);
                        }
                        operand_right_uppaal = UppaalBuilder.uppaal_variable_read( variable_id );
                    }
                    else
                    {
                        throw new VerificationException("Formula, variable \"" + operand_right_xml + "\" does not exist.");
                    }
                }

                operator_uppaal = operators_xml_to_uppaal.get( formula.getOperator() );


                UppaalBuilder.add_to_cumulative_condition( cumulative_results_string,
                        UppaalBuilder.binary_expression_enclosed( operand_left_uppaal, operator_uppaal, operand_right_uppaal ) );

            }

        }
        else if ( Predicate.class.isInstance( current_condition ) )
        {
            Predicate predicate                 = (Predicate)current_condition;
            String predicate_string             = predicate_to_string( predicate );
            int variable_id                     = this.variable_manager.variable_or_parameter_get_variable_id( plp_id, predicate_string );
            if (null != uppaal_variables_indexes){
                uppaal_variables_indexes.add(variable_id);
            }

            UppaalBuilder.add_to_cumulative_condition( cumulative_results_string, UppaalBuilder.make_uppaal_condition_is_true_variable( UppaalBuilder.uppaal_variable_read( variable_id ) ) );
        }
        else if ( BitwiseOperation.class.isInstance( current_condition ) )
        {
            BitwiseOperation bitwise_operation  = (BitwiseOperation)current_condition;
            String operation                    = "";

            if ( BitwiseOperation.Operation.AND == bitwise_operation.getOperation() )
            {
                operation = UppaalBuilder.STR_AND;
            }
            else if ( BitwiseOperation.Operation.OR == bitwise_operation.getOperation() )
            {
                operation = UppaalBuilder.STR_OR;
            }

            StringBuffer results_string_buffer = convert_xml_conditions_to_uppaal( plp_id, bitwise_operation.getConditions(), operation, uppaal_variables_indexes );

            UppaalBuilder.add_to_cumulative_expression_with_operator( cumulative_results_string, results_string_buffer, UppaalBuilder.STR_AND );
        }
        else if ( NotCondition.class.isInstance( current_condition ) )
        {
            NotCondition not_condition  = (NotCondition)current_condition;
            StringBuffer internal_condition_buffer = new StringBuffer();
            convert_xml_condition_to_uppaal( plp_id, internal_condition_buffer , not_condition.getCondition(), uppaal_variables_indexes );
            UppaalBuilder.add_to_cumulative_condition( cumulative_results_string, UppaalBuilder.STR_NOT + " " + internal_condition_buffer.toString() );
        }
        else if ( QuantifiedCondition.class.isInstance( current_condition ) ) {
            QuantifiedCondition quantified_condition = (QuantifiedCondition) current_condition;

            if (QuantifiedCondition.Quantifier.FORALL == quantified_condition.getQuantifier()) {
                reports.add_warning("FOR ALL quantifier unsupported \"" + quantified_condition.toString() + "\", using as regular condition");
            }

            convert_xml_condition_to_uppaal( plp_id, cumulative_results_string, quantified_condition.getCondition(), uppaal_variables_indexes );
        }
    }

    public void convert_xml_condition_for_assignments_to_list_of_variable_and_values(int plp_id, List<Condition> assignments, List<Integer> variables_id, List<String> values ) throws VerificationException
    {
        for ( Condition assignment : assignments )
        {
            convert_xml_condition_for_assignments_to_list_of_variable_and_values( plp_id, assignment, variables_id, values );
        }
    }


    public void convert_xml_condition_for_assignments_to_list_of_variable_and_values(int plp_id, Condition assignment, List<Integer> variables_id, List<String> values ) throws VerificationException
    {

        if (null == assignment) {
            return;
        } else if (Formula.class.isInstance(assignment)) {
            Formula formula = (Formula) assignment;

            if (formula.getRange() != null) // Right part containing range
            {
                String variable_name = formula.getLeftExpr();
                boolean variable_is_exist;

                if (-1 == plp_id) {
                    variable_is_exist = this.variable_manager.global_variable_is_exist(variable_name);
                } else {
                    variable_is_exist = this.variable_manager.variable_or_parameter_is_exist(plp_id, variable_name);
                }

                if (variable_is_exist) {
                    int variable_id;

                    if (-1 == plp_id) {
                        variable_id = this.variable_manager.global_variable_get_id(variable_name);
                    } else {
                        variable_id = this.variable_manager.variable_or_parameter_get_variable_id(plp_id, variable_name);
                    }


                    Range range = formula.getRange();

                    VerificationVariable variable_data = this.variable_manager.global_variable_get_data(variable_id);

                    String min;
                    String max;

                    try {

                        min = convert_xml_expression_to_uppaal_expression( plp_id, range.getMinValue() );
                        max = convert_xml_expression_to_uppaal_expression( plp_id, range.getMinValue() );
                    } catch (VerificationException exception) {
                        throw new VerificationException("problem in assignment \"" + assignment.toString() + "\"; " + exception.get_message());
                    }

                    variables_id.add(variable_id);
                    values.add( String.format( "((%s) + (%s)) / 2", min, max ) );
                } else {
                    throw new VerificationException("Formula, variable \"" + variable_name + "\" does not exist.");
                }


            } else {
                String operand_left_xml                             = formula.getLeftExpr();
                String operand_right_xml                            = formula.getRightExpr();
                String operand_left_uppaal                          = "";
                String operand_right_uppaal                         = "";
                boolean operand_left_is_constant                    = false;
                boolean operand_right_is_constant                   = false;
                VerificationVariable operand_left_variable_data     = null;
                VerificationVariable operand_right_variable_data    = null;
                String assignment_left                              = "";
                String assignment_right                             = "";
                String xml_operator_fixed_direction                 = "";
                Integer operand_left_variable_id                    = 0;
                Integer operand_right_variable_id                   = 0;

                if ( xml_is_numeric_or_boolean( operand_left_xml      ) ||
                     xml_is_mathematical_expression( operand_left_xml ) ) {
                    try {
                        operand_left_uppaal         = convert_xml_expression_to_uppaal_expression( plp_id, operand_left_xml );
                        operand_left_is_constant    = true;
                    } catch (VerificationException exception) {
                        throw new VerificationException("problem in assignment \"" + assignment.toString() + "\", can read left value.\n" + exception.get_message());
                    }
                } else {

                    boolean variable_is_exist;

                    if (-1 == plp_id) {
                        variable_is_exist = this.variable_manager.global_variable_is_exist(operand_left_xml);
                    } else {
                        variable_is_exist = this.variable_manager.variable_or_parameter_is_exist(plp_id, operand_left_xml);
                    }

                    if (variable_is_exist) {
                        int left_variable_id;

                        if (-1 == plp_id) {
                            left_variable_id = this.variable_manager.global_variable_get_id(operand_left_xml);
                        } else {
                            left_variable_id = this.variable_manager.variable_or_parameter_get_variable_id(plp_id, operand_left_xml);
                        }

                        operand_left_variable_id = left_variable_id;

                        operand_left_uppaal = UppaalBuilder.uppaal_variable_read(left_variable_id);
                        operand_left_variable_data = this.variable_manager.global_variable_get_data(left_variable_id);
                        operand_left_is_constant = VerificationVariable.VerificationVariableType.type_constant == operand_left_variable_data.variable_type;
                    } else {
                        throw new VerificationException("Formula, variable \"" + operand_left_xml + "\" does not exist.");
                    }
                }


                if ( xml_is_numeric_or_boolean( operand_right_xml )   ||
                     xml_is_mathematical_expression( operand_left_xml ) ) {
                    try {
                        operand_right_uppaal = convert_xml_expression_to_uppaal_expression( plp_id, operand_right_xml);
                        operand_right_is_constant = true;
                    } catch (VerificationException exception) {
                        throw new VerificationException("problem in assignment \"" + assignment.toString() + "\", can read right value.\n" + exception.get_message());
                    }
                } else {
                    boolean variable_is_exist;

                    if (-1 == plp_id) {
                        variable_is_exist = this.variable_manager.global_variable_is_exist(operand_right_xml);
                    } else {
                        variable_is_exist = this.variable_manager.variable_or_parameter_is_exist(plp_id, operand_right_xml);
                    }

                    if (variable_is_exist) {
                        int right_variable_id;

                        if (-1 == plp_id) {
                            right_variable_id = this.variable_manager.global_variable_get_id(operand_right_xml);
                        } else {
                            right_variable_id = this.variable_manager.variable_or_parameter_get_variable_id(plp_id, operand_right_xml);
                        }

                        operand_right_variable_id = right_variable_id;

                        operand_right_uppaal = UppaalBuilder.uppaal_variable_read(right_variable_id);
                        operand_right_variable_data = this.variable_manager.global_variable_get_data(right_variable_id);
                        operand_right_is_constant = VerificationVariable.VerificationVariableType.type_constant == operand_right_variable_data.variable_type;
                    } else {
                        throw new VerificationException("Formula, variable \"" + operand_right_xml + "\" does not exist.");
                    }
                }

                if (true == operand_left_is_constant) {
                    if (true == operand_right_is_constant) {
                        throw new VerificationException("Both sides of assignment are constants, left: \"" + operand_left_xml + "\", right: \"" + operand_right_xml + "\".");
                    } else // variable on the right side
                    {
                        xml_operator_fixed_direction = this.operators_xml_mirror.get(formula.getOperator());

                        //assignment_left     = operand_right_uppaal;
                        assignment_right            = operand_left_uppaal;
                        operand_left_variable_id    = operand_right_variable_id;
                    }
                } else {
                    xml_operator_fixed_direction = formula.getOperator();

                    if (false == operand_right_is_constant) {
                        reports.add_warning("Both sides of assignment are variables, make sure that intended assign right side value to variable on left: \"" + formula.toString() + "\"");
                    }

                    assignment_left     = operand_left_uppaal;
                    assignment_right    = operand_right_uppaal;
                }

                if (xml_operator_fixed_direction.equals(STR_XML_NOT_EQUAL)) {
                    assignment_right = assignment_right + " + 1";
                } else if (xml_operator_fixed_direction.equals(STR_XML_LESS_THAN)) {
                    assignment_right = assignment_right + " - 1";
                } else if (xml_operator_fixed_direction.equals(STR_XML_LESS_THAN_EQUAL)) {
                    assignment_right = assignment_right;
                } else if (xml_operator_fixed_direction.equals(STR_XML_GREATER_THAN)) {
                    assignment_right = assignment_right + " + 1";
                } else if (xml_operator_fixed_direction.equals(STR_XML_GREATER_THAN_EQUAL)) {
                    assignment_right = assignment_right;
                }


                variables_id.add(operand_left_variable_id);
                values.add(assignment_right);
            }

        }else if ( Predicate.class.isInstance( assignment ) )
        {
            Predicate predicate                 = (Predicate)assignment;
            String predicate_string             = predicate_to_string( predicate );

            int predicate_variable_id;

            if ( -1 == plp_id )
            {
                predicate_variable_id = this.variable_manager.global_variable_get_id( predicate_string );
            }
            else
            {
                predicate_variable_id = this.variable_manager.variable_or_parameter_get_variable_id( plp_id, predicate_string );
            }

            variables_id.add( predicate_variable_id );
            values.add("1");
        }
        else if ( BitwiseOperation.class.isInstance( assignment ) )
        {
            BitwiseOperation bitwise_operation  = (BitwiseOperation)assignment;

            convert_xml_condition_for_assignments_to_list_of_variable_and_values( plp_id, bitwise_operation.getConditions(), variables_id, values );
        }
        else if ( NotCondition.class.isInstance( assignment ) )
        {
            NotCondition not_condition  = (NotCondition)assignment;

            throw new VerificationException("Assignment containing NOT operator unsupported: \"" + not_condition.toString() +  "\".");
        }
        else if ( QuantifiedCondition.class.isInstance( assignment ) ) {
            QuantifiedCondition quantified_condition = (QuantifiedCondition) assignment;

            reports.add_warning("FOR ALL and EXIST quantifiers ignored in assignment: \"" + quantified_condition.toString() + "\"" );

            convert_xml_condition_for_assignments_to_list_of_variable_and_values( plp_id, quantified_condition.getCondition(), variables_id, values );
        }
    }


    public void convert_xml_conditions_to_uppaal_assignment( int plp_id, StringBuffer cumulative_results_string, List<Condition> current_assignments ) throws VerificationException
    {
        if (null != current_assignments) {

            int assignments_amount = current_assignments.size();

            for (int i = 0; i < assignments_amount; i++) {

                StringBuffer current_assignment_buffer = new StringBuffer();
                convert_xml_condition_to_uppaal_assignment(plp_id, current_assignment_buffer, current_assignments.get(i) );

                UppaalBuilder.add_to_cumulative_expression_with_operator( cumulative_results_string, current_assignment_buffer, UppaalBuilder.STR_COMMA );
            }

        }
    }

    public void convert_xml_condition_to_uppaal_assignment( int plp_id, StringBuffer cumulative_string, Condition current_assignment ) throws VerificationException
    {
        List<Integer>   variables_id   = new LinkedList<>();
        List<String>    values      = new LinkedList<>();

        convert_xml_condition_for_assignments_to_list_of_variable_and_values( plp_id, current_assignment, variables_id, values );

        int last_index  = variables_id.size() - 1;

        for ( int i = 0 ; i <= last_index ; i++ )
        {
            UppaalBuilder.add_to_cumulative_assignment( cumulative_string,
                     UppaalBuilder.uppaal_variable_write( variables_id.get(i).intValue(), values.get(i) ) );
        }

    }


    public String predicate_to_string( Predicate predicate )
    {
        String predicate_string     = "";
        String terms_string         = "";
        List<String> terms          = predicate.getValues();
        int terms_amount_minus_one  = terms.size() - 1;
        for ( int i = 0; i < terms_amount_minus_one; i++ )
        {
            terms_string = terms_string + terms.get(i) + ",";
        }
        if ( terms_amount_minus_one > -1 )
        {
            terms_string = terms_string + terms.get( terms_amount_minus_one );
        }

        predicate_string = predicate.getName() + "(" + terms_string + ")";


        return predicate_string;
    }



    public List<Condition> probs_to_conditions( List<ConditionalProb> conditional_probs )
    {
        List<Condition> conditions = new LinkedList<>();

        if (null != conditional_probs) {
            for (ConditionalProb conditional_prob : conditional_probs) {
                conditions.add( conditional_prob.getCondition() );
            }
        }

        return conditions;
    }

    public List<Condition> dists_to_conditions( List<ConditionalDist> conditional_dists )
    {
        List<Condition> conditions = new LinkedList<>();

        if (null != conditional_dists) {
            for ( ConditionalDist conditional_dist : conditional_dists) {
                conditions.add( conditional_dist.getCondition() );
            }
        }

        return conditions;
    }

    public List<Condition> failures_to_conditions( List<FailureMode> failure_modes )
    {
        List<Condition> conditions = new LinkedList<>();

        if (null != failure_modes) {
            for ( FailureMode failure_mode : failure_modes) {
                conditions.add( failure_mode.getCondition() );
            }
        }

        return conditions;
    }

    private Set<String> extract_all_possible_variables_in_formula( String xml_formula )
    {
        Pattern         variable_pattern    = Pattern.compile("[a-zA-Z][a-zA-Z0-9_]*");
        Matcher         matcher             = variable_pattern.matcher( xml_formula );
        Set <String>    variables           = new HashSet<>();

        while(matcher.find()) {
            variables.add(xml_formula.substring(matcher.start(), matcher.end()));
        }

        return variables;
    }

    private List<StringsStartAndEnd> extract_all_numbers_in_formula( String xml_formula )
    {
        Pattern                     number_pattern  = Pattern.compile("\\d+(\\.\\d+)?");
        Matcher                     matcher         = number_pattern.matcher( xml_formula );
        List<StringsStartAndEnd>    number_places   = new LinkedList<>();

        while(matcher.find()) {
            number_places.add( new StringsStartAndEnd( matcher.start(), matcher.end() ));
        }

        return number_places;
    }

    private class StringsStartAndEnd{
        int start;
        int end;

        public StringsStartAndEnd( int start, int end ){
            this.start  = start;
            this.end    = end;
        }
    }

    public String convert_xml_expression_to_uppaal_expression( int plp_id, String xml_formula ) throws VerificationException
    {
        if ( xml_formula.isEmpty() )
        {
            throw new VerificationException("Can't convert empty formula.");
        }

        StringBuffer    result_formula  = new StringBuffer(xml_formula);

        int replace_offset = 0;
        List<StringsStartAndEnd> number_places = extract_all_numbers_in_formula( xml_formula );
        for ( StringsStartAndEnd number_place : number_places )
        {
            if ( ( 0 == number_place.start ) ||
                 ( xml_formula.substring( number_place.start - 1, number_place.start ).matches("[^a-zA-Z_]") ) ){
                int found_length = number_place.end - number_place.start;
                String converted_number = String.valueOf(convert_xml_double_to_uppaal_int(Double.valueOf(xml_formula.substring(number_place.start, number_place.end))));
                int converted_length = converted_number.length();

                StringBufferExtra.replace(result_formula, replace_offset + number_place.start, found_length, converted_number);

                replace_offset += converted_length - found_length;
            }
        }

        Set <String>    variables       = extract_all_possible_variables_in_formula(result_formula.toString());
        //StringBuffer    result_formula  = new StringBuffer(xml_formula);

        for ( String variable : variables )
        {
            if ( variable.equals( XMLtoUppaalConverter.STR_XML_TRUE ) )
            {
                StringBufferExtra.replace_all_uniqueque_variables( result_formula, variable, UppaalBuilder.STR_NUMERIC_TRUE );
            }
            else if ( variable.equals( XMLtoUppaalConverter.STR_XML_FALSE ) )
            {
                StringBufferExtra.replace_all_uniqueque_variables( result_formula, variable, UppaalBuilder.STR_NUMERIC_FALSE );
            }
            else  if ( this.variable_manager.variable_or_parameter_is_exist( plp_id, variable ) )
            {
                int variable_id = this.variable_manager.variable_or_parameter_get_variable_id( plp_id, variable );
                //System.out.println("  Origin Formula: \"" + xml_formula + "\"");
                //System.out.println("Replace variable: \"" + variable + "\"");
                //System.out.println("            With: \"" + variable_id_to_uppaal_string( variable_id ) + "\"");
                StringBufferExtra.replace_all_uniqueque_variables( result_formula, variable, UppaalBuilder.uppaal_variable_read( variable_id ) );
                //System.out.println("  Result Formula: \"" + result_formula.toString() + "\"");
            }
            else
            {
                throw new VerificationException("Unknown variable \"" + variable + "\", in formula: \"" + xml_formula + "\"." );
            }


        }

        return UppaalBuilder.comply_string_single_line( result_formula.toString() );
    }

    static public String parameter_get_first_field( PLPParameter parameter )
    {
        if ( true == parameter.getParamFieldValues().isEmpty() )
        {
            return parameter.getName();
        }
        else
        {
            return parameter_and_field_to_name( parameter.getName(), parameter.getParamFieldValues().get(0) );
        }
    }

    static public String parameter_and_field_to_name( String parameter_name, String field_name )
    {
        return parameter_name + "_" + field_name;
    }

    public void parameters_add(int plp_id, List<PLPParameter> parameters, VerificationParameter.VerificationParameterType parameters_type ) throws VerificationException
    {
        if ( null != parameters ) {
            for ( PLPParameter current_parameter : parameters ) {

                if ( false == this.variable_manager.local_parameters_is_exist( plp_id, current_parameter.getName() ) ) {

                    if ( true == current_parameter.getParamFieldValues().isEmpty() )
                    {
                        VerificationParameter parameter_data    = new VerificationParameter();
                        parameter_data.parameter_type           = parameters_type;

                        this.variable_manager.local_parameters_add( plp_id, current_parameter.getName(), parameter_data );
                    }
                    else
                    {
                        for ( String field_name : current_parameter.getParamFieldValues() )
                        {
                            VerificationParameter parameter_data    = new VerificationParameter();
                            parameter_data.parameter_type           = parameters_type;

                            this.variable_manager.local_parameters_add( plp_id, parameter_and_field_to_name( current_parameter.getName(), field_name ), parameter_data );
                        }
                    }


                }
                else
                {
                    throw new VerificationException("PLP id: " + String.valueOf(plp_id) + ", parameter already exist: \"" + current_parameter.getName() + "\", type: " +  String.valueOf(parameters_type) + "." );
                }

            }
        }
    }

    public void recursive_add_all_predicates( Condition condition ) throws VerificationException
    {
        if ( null == condition ){
            return;
        }
        else if ( Predicate.class.isInstance( condition ) )
        {
            Predicate predicate                 = (Predicate)condition;
            String predicate_string             = predicate_to_string( predicate );

            if ( false == this.variable_manager.global_variable_is_exist( predicate_string ) ) {
                VerificationVariable variable_data = new VerificationVariable();
                variable_data.variable_type = VerificationVariable.VerificationVariableType.type_predicate;
                variable_data.is_set        = false;
                variable_data.is_in_range   = true;
                variable_data.min_value     = 0;
                variable_data.max_value     = 1;

                this.variable_manager.global_variable_add( predicate_string, variable_data );
            }
        }
        else if ( BitwiseOperation.class.isInstance( condition ) )
        {
            BitwiseOperation bitwise_operation  = (BitwiseOperation)condition;

            if (null != bitwise_operation.getConditions()) {
                for (Condition sub_condition : bitwise_operation.getConditions()) {
                    recursive_add_all_predicates(sub_condition);
                }
            }
        }
        else if ( NotCondition.class.isInstance( condition ) )
        {
            NotCondition not_condition  = (NotCondition)condition;

            recursive_add_all_predicates( not_condition.getCondition() );
        }
        else if ( QuantifiedCondition.class.isInstance( condition ) )
        {
            QuantifiedCondition quantified_condition  = (QuantifiedCondition)condition;

            recursive_add_all_predicates( quantified_condition.getCondition() );
        }
    }

    public void recursive_add_all_predicates( List<Condition> conditions ) throws VerificationException
    {
        if (null != conditions) {
            for (Condition condition : conditions) {
                recursive_add_all_predicates(condition);
            }
        }
    }

}
