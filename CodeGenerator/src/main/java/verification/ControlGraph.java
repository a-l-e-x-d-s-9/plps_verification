package verification;

import conditions.Condition;
import plpEtc.FieldType;
import plpEtc.Range;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by alexds9 on 12/06/17.
 */
public class ControlGraph {
    public  List<ControlNodeInterface>  control_nodes;
    public  String                      root_name;
    private int                         root_id;
    private Map<String,Integer>         map_node_name_to_id;
    private Map<String,Integer>         map_nodes_names_to_transition_id;
    private int                         nodes_id_counter;
    private int                         nodes_transitions_id_counter;
    private XMLtoUppaalConverter        xml_to_uppaal_converter;
    private PLPCatalog                  plp_catalog;


    public ControlGraph( XMLtoUppaalConverter xml_to_uppaal_converter,
                         PLPCatalog plp_catalog )
    {
        this.control_nodes                      = new LinkedList<>();
        this.root_name                          = "";
        this.root_id                            = -1;
        this.map_node_name_to_id                = new HashMap<>();
        this.map_nodes_names_to_transition_id   = new HashMap<>();
        this.nodes_id_counter                   = 0;
        this.nodes_transitions_id_counter       = 0;
        this.xml_to_uppaal_converter            = xml_to_uppaal_converter;
        this.plp_catalog                        = plp_catalog;
    }

    private String combine_predecessor_and_successor( int predecessor, int successor )
    {
        return String.format( "%d_%d", predecessor, successor );
    }

    public boolean is_node_exist( String node_name )
    {
        return this.map_node_name_to_id.containsKey( node_name );
    }

    public int node_name_get_id( String node_name )
    {
        if ( true == this.map_node_name_to_id.containsKey( node_name ) )
        {
            return this.map_node_name_to_id.get( node_name );
        }
        else
        {
            return -1;
        }
    }

    private int node_name_add( ControlNodeInterface node )
    {
        int node_id;

        if ( false == this.map_node_name_to_id.containsKey( node.get_node_name() ) )
        {
            node_id = this.nodes_id_counter++;
            this.map_node_name_to_id.put( node.get_node_name(), node_id );
            node.set_node_id( node_id );
        }
        else
        {
            node_id = this.map_node_name_to_id.get( node.get_node_name() );
        }
        return node_id;
    }

    public int contron_nodes_amount()
    {
        return this.nodes_id_counter;
    }

    public int contron_transitions_amount()
    {
        return this.nodes_transitions_id_counter;
    }


    private int add_transition( String predecessor_name, String successor_name )
    {
        int transition_id;
        int predecessor_id  = node_name_get_id( predecessor_name );
        int successor_id    = node_name_get_id( successor_name );
        String combined_ids = combine_predecessor_and_successor(predecessor_id,successor_id);

        transition_id = this.nodes_transitions_id_counter++;
        this.map_nodes_names_to_transition_id.put( combined_ids, transition_id );

        return transition_id;
    }

    public int get_transition_id( String predecessor_name, String successor_name )
    {
        int predecessor_id  = node_name_get_id( predecessor_name );
        int successor_id    = node_name_get_id( successor_name );
        String combined_ids = combine_predecessor_and_successor(predecessor_id,successor_id);

        return this.map_nodes_names_to_transition_id.get( combined_ids );
    }

    public void finalize_when_all_data_exist() throws VerificationException
    {
        // 1. Check that all successor nodes names are defined as valid node.
        // 2. Assign unique node_id for each node.
        // 3. Fill out the predecessors lists for each node.
        // 4. Assign unique transition_id for every transition between each 2 nodes.
        // 5. Set root_id.
        // 6. Check all used PLPs name corresponding to existing PLPs.

        Map<String, ControlNodeInterface> map_node_names_to_node_data = new HashMap<>();

        for ( ControlNodeInterface control_node : this.control_nodes ){
            node_name_add( control_node );
            map_node_names_to_node_data.put( control_node.get_node_name(), control_node );
        }


        if ( is_node_exist( this.root_name ) ) {

            this.root_id = node_name_get_id( this.root_name );

            for ( ControlNodeInterface control_node : this.control_nodes ) {
                boolean is_exist    = true;
                String  node_name   = "";

                if (ControlNodeInterface.ControlNodeType.node_probability == control_node.get_node_kind()) {
                    ControlNodeProbability node_probability = (ControlNodeProbability) control_node;

                    for ( ControlProbabilityForSuccessor probability_for_successor_node : node_probability.probability_for_successor_nodes) {
                        String successor_node_name = probability_for_successor_node.node_name;

                        if ( false == successor_node_name.isEmpty() ) {
                            if (false == is_node_exist(successor_node_name)) {
                                is_exist = false;
                                node_name = successor_node_name;
                                break;
                            } else {
                                ControlNodeInterface successor_node = map_node_names_to_node_data.get(successor_node_name);
                                successor_node.add_predecessor(control_node.get_node_name());
                                add_transition(control_node.get_node_name(), successor_node_name);
                            }
                        }
                    }
                } else if (ControlNodeInterface.ControlNodeType.node_concurrent == control_node.get_node_kind()) {
                    ControlNodeConcurrent node_concurrent = (ControlNodeConcurrent) control_node;

                    for (String run_node : node_concurrent.run_nodes) {

                        String successor_node_name = run_node;

                        if ( false == successor_node_name.isEmpty() ) {
                            if (false == is_node_exist(successor_node_name)) {
                                is_exist = false;
                                node_name = successor_node_name;
                                break;
                            } else {
                                ControlNodeInterface successor_node = map_node_names_to_node_data.get(successor_node_name);
                                successor_node.add_predecessor(control_node.get_node_name());
                                add_transition(control_node.get_node_name(), successor_node_name);
                            }
                        }
                    }
                } else if (ControlNodeInterface.ControlNodeType.node_sequential == control_node.get_node_kind()) {
                    ControlNodeSequential node_sequential = (ControlNodeSequential) control_node;

                    String successor_node_name = node_sequential.next_node_name;

                    if ( false == successor_node_name.isEmpty() ) {
                        if (false == is_node_exist(successor_node_name)) {
                            is_exist = false;
                            node_name = successor_node_name;
                        } else {
                            ControlNodeInterface successor_node = map_node_names_to_node_data.get(successor_node_name);
                            successor_node.add_predecessor(control_node.get_node_name());
                            add_transition(control_node.get_node_name(), successor_node_name);
                        }
                    }

                    for (ControlUpdateForPLP update_for_plp : node_sequential.update_for_plp) {
                        if (false == this.plp_catalog.plp_name_is_exist(update_for_plp.plp_name)) {
                            throw new VerificationException("Control node \"" + control_node.get_node_name() + "\", using non-existing PLP \"" + update_for_plp.plp_name + "\".");
                        }
                    }

                } else if (ControlNodeInterface.ControlNodeType.node_condition == control_node.get_node_kind()) {
                    ControlNodeCondition node_condition = (ControlNodeCondition) control_node;

                    for (ControlConditionOfSuccessor condition_of_successor : node_condition.condition_of_successor_nodes) {

                        String successor_node_name = condition_of_successor.node_name;

                        if ( false == successor_node_name.isEmpty() ) {
                            if (false == is_node_exist(successor_node_name)) {
                                is_exist = false;
                                node_name = successor_node_name;
                                break;
                            } else {
                                ControlNodeInterface successor_node = map_node_names_to_node_data.get(successor_node_name);
                                successor_node.add_predecessor(control_node.get_node_name());
                                add_transition(control_node.get_node_name(), successor_node_name);
                            }
                        }
                    }
                }

                if ( false == is_exist )
                {
                    throw new VerificationException( "Control node \"" + node_name + "\" does not exist, but referred by existing control node." );
                }
            }
        }
        else
        {
            throw new VerificationException( "Control node \"" + this.root_name + "\" does not exist, but referred by existing control node." );
        }

    }

    public int get_root_id()
    {
        return this.root_id;
    }

    /*
    public String control_node_to_variable_name( String control_node_name )
    {
        return "_control_" + control_node_name;
    }

    public int create_variable_for_control_node( String control_node_name )
    {
        String variable_name = control_node_to_variable_name( control_node_name );

        VerificationVariable variable_data = new VerificationVariable();

        variable_data.variable_type = VerificationVariable.VerificationVariableType.type_control_node;
        variable_data.value_type    = VerificationVariable.VerificationValueType.value_boolean;
        variable_data.value         = 0;
        variable_data.is_set        = true;
        variable_data.is_in_range   = true;
        variable_data.min_value     = 0;
        variable_data.max_value     = 1;

        int variable_id = global_variable_add( variable_name, variable_data );

        this.map_control_node_name_to_variable_id.put( control_node_name, variable_id );

        return variable_id;
    }

    public int control_node_to_variable_id( String control_node_name )
    {
        if ( this.map_control_node_name_to_variable_id.containsKey( control_node_name ) )
        {
            return this.map_control_node_name_to_variable_id.get( control_node_name ).intValue();
        }
        else
        {
            return -1;
        }
    }
     */
}
