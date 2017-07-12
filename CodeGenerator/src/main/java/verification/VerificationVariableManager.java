package verification;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * Created by alexds9 on 09/06/17.
 */
public class VerificationVariableManager {

    public static final double RESOURCE_UNDEFINED = -1;
    private HashMap<String, Integer>    map_global_variables_to_id;
    private Vector<String>              map_variable_id_to_name;

    private PLPCatalog                  plp_catalog;

    private int id_counter_global_variable;
    private Vector<VerificationVariable>        variables_data;
    private Vector<VerificationPLPParameters>   plp_parameters;

    private int concurrent_modules_reserved_index_start;
    private int concurrent_modules_reserved_index_end;
    private int concurrent_modules_reserved_indexes_amount;

    private int concurrent_requests_amount = 0;

    public VerificationVariableManager( PLPCatalog plp_catalog )
    {
        this.plp_catalog                = plp_catalog;

        this.id_counter_global_variable = 0;
        this.variables_data             = new Vector<>();
        this.plp_parameters             = new Vector<>();

        this.map_global_variables_to_id = new HashMap<>();
        this.map_variable_id_to_name    = new Vector<>();


    }

    public int global_variable_add( String variable_name, VerificationVariable variable_data ) {
        int current_id = this.id_counter_global_variable;
        this.id_counter_global_variable++;

        this.map_global_variables_to_id.put(variable_name, current_id);
        this.map_variable_id_to_name.add(current_id,variable_name);
        this.variables_data.add(current_id, variable_data);

        return current_id;
    }

    public int global_variable_get_id(String variable_name) {
        return this.map_global_variables_to_id.get(variable_name);
    }

    public String global_variable_get_name( int variable_id ) {
        return this.map_variable_id_to_name.get(variable_id);
    }

    public VerificationVariable  global_variable_get_data(int variable_index) {
        return this.variables_data.elementAt(variable_index);
    }

    public VerificationVariable global_variable_get_data(String variable_name) {
        return global_variable_get_data(global_variable_get_id(variable_name));
    }

    public boolean global_variable_is_exist(String variable_name) {
        return this.map_global_variables_to_id.containsKey(variable_name);
    }

    public void global_variable_concurrent_module_add( int variable_id, String variable_name, VerificationVariable variable_data ) {
        if ( ( this.concurrent_modules_reserved_index_start <= variable_id ) &&
             ( variable_id <= this.concurrent_modules_reserved_index_end   ) ) {
            this.map_global_variables_to_id.put(variable_name, variable_id);
            this.map_variable_id_to_name.add(variable_id, variable_name);
            this.variables_data.add(variable_id, variable_data);
        }
    }

    public boolean variable_or_parameter_is_exist( int plp_id, String variable_name ) {
        if ( true == this.map_global_variables_to_id.containsKey(variable_name) )
        {
            return true;
        }
        else
        {
            return local_parameters_is_exist( plp_id, variable_name );
        }
    }

    public int variable_or_parameter_get_variable_id( int plp_id, String variable_name ) {
        if ( true == this.map_global_variables_to_id.containsKey(variable_name) )
        {
            return global_variable_get_id( variable_name );
        }
        else
        {
            return local_parameters_get_data( plp_id, variable_name ).variable_id;
        }
    }

    public void local_parameters_init( int plp_id )
    {
        this.plp_parameters.add( plp_id, new VerificationPLPParameters() );
    }

    public int local_parameters_add( int plp_id, String parameter_name, VerificationParameter parameter_data ) {
        int parameter_id;

        if ( plp_id >= this.plp_parameters.size() )
        {
            parameter_id = 0;
        }
        else
        {
            parameter_id = this.plp_parameters.get(plp_id).name_to_id.size();
        }
        VerificationPLPParameters current_plp_parameters = this.plp_parameters.get(plp_id);

        String variable_name = local_parameter_to_variable_name( plp_id, parameter_name );
        VerificationVariable variable_data = new VerificationVariable();
        variable_data.plp_id = plp_id;
        variable_data.is_set = false;
        variable_data.variable_type = VerificationVariable.VerificationVariableType.type_parameter;

        parameter_data.variable_id = global_variable_add( variable_name, variable_data );

        current_plp_parameters.name_to_id.put( parameter_name, parameter_id );
        current_plp_parameters.data.add( parameter_id, parameter_data );



        return parameter_id;
    }

    public int local_parameters_get_id( int plp_id, String parameter_name ) {
        return this.plp_parameters.get(plp_id).name_to_id.get(parameter_name);
    }

    public VerificationParameter local_parameters_get_data( int plp_id, int parameter_index ) {
        return this.plp_parameters.get(plp_id).data.get( parameter_index );
    }

    public VerificationParameter local_parameters_get_data( int plp_id, String parameter_name ) {
        return local_parameters_get_data( plp_id, local_parameters_get_id( plp_id, parameter_name ) );
    }

    public boolean local_parameters_is_exist( int plp_id, String parameter_name ) {
        if ( plp_id < this.plp_parameters.size() )
        {
            VerificationPLPParameters parameters = this.plp_parameters.get(plp_id);
            if ( null != parameters )
            {
                return parameters.name_to_id.containsKey(parameter_name);
            }
            else
            {
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public String local_parameter_to_variable_name( int plp_id, String parameter_name )
    {
        return  "_" + this.plp_catalog.find_plp_name_by_id( plp_id ) + "_" + parameter_name;
    }

    public String concurrent_module_variable_name( String module_name )
    {
        return "_module_" + module_name;
    }

    public int concurrent_modules_reserve_variables(int modules_amount )
    {
        if ( 0 < modules_amount )
        {
            this.concurrent_modules_reserved_indexes_amount = modules_amount;
            this.concurrent_modules_reserved_index_start    = this.id_counter_global_variable;
            this.concurrent_modules_reserved_index_end      = this.concurrent_modules_reserved_index_start + modules_amount - 1;

            this.id_counter_global_variable                 = this.concurrent_modules_reserved_index_end + 1;
        }
        else
        {
            this.concurrent_modules_reserved_indexes_amount = 0;
            this.concurrent_modules_reserved_index_start    = -1;
            this.concurrent_modules_reserved_index_end      = -1;
        }

        return this.concurrent_modules_reserved_index_start;
    }

    public int concurrent_module_index_to_variable_id( int module_index_zero_based )
    {
        if ( ( 0 <= module_index_zero_based ) &&
             ( module_index_zero_based < this.concurrent_modules_reserved_indexes_amount ) )
        {
            return this.concurrent_modules_reserved_index_start + module_index_zero_based;
        }
        else
        {
            return -1;
        }
    }

    public void concurrent_requests_add_one()
    {
        this.concurrent_requests_amount++;
    }

    public int concurrent_requests_get()
    {
        return this.concurrent_requests_amount;
    }

    public int get_global_variables_amount()
    {
        return this.id_counter_global_variable;
    }



    public void print_variables()
    {
        System.out.println("******************************************* All Variables ******************************************");
        System.out.println(" ID| Variable_Name                   | Variable_Type | Value_Type     |  Value [  Min  ,  Max  ]");
        System.out.println("----------------------------------------------------------------------------------------------------");
        for ( int i = 0; i < this.id_counter_global_variable; i++ )
        {
            VerificationVariable variable_data = global_variable_get_data(i);
            String variable_name = this.map_variable_id_to_name.get(i);
            if ( variable_name.length() > 33 )
            {
                variable_name = variable_name.substring(0,10) + "..." + variable_name.substring(variable_name.length()-20,variable_name.length());
            }

            //" ID|Variable_Name                 | Variable_Type | Value_Type     |  Value [   Min  ,  Max   ]"
            System.out.format( "%3d|%-33.33s|%-15s|%-16s", i, variable_name, variable_data.variable_type_to_name(), variable_data.value_type_to_name() );

            if ( true == variable_data.is_set )
            {
                System.out.format( "|%7d ", variable_data.value );
            }
            else
            {
                System.out.print( "| not_set" );
            }

            if ( true == variable_data.is_in_range )
            {
                System.out.format( "[ %6d, %6d]", variable_data.min_value, variable_data.max_value );
            }

            System.out.println( "" );
        }

        System.out.println("****************************************************************************************************");
    }
}
