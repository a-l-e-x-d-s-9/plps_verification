package verification;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by alexds9 on 12/06/17.
 */
public class ControlNodeCondition extends ControlNodeInterface {

    private String          node_name;
    private StartPolicyType start_policy;
    private ControlNodeType node_kind;
    private int             node_id;
    private List<String>    predecessors_names;
    private int             concurrent_process_id;

    public  List<ControlConditionOfSuccessor> condition_of_successor_nodes;
    public  int             wait_time;

    public ControlNodeCondition(){

        this.node_kind                      = ControlNodeType.node_condition;
        this.condition_of_successor_nodes   = new LinkedList<>();
        this.wait_time                      = 0;
        this.predecessors_names             = new LinkedList<>();
    }

    public ControlNodeType get_node_kind(){
        return this.node_kind;
    }

    public String get_node_name(){
        return this.node_name;
    }

    public void set_node_name(String node_name){
        this.node_name = node_name;
    }

    public StartPolicyType get_start_policy() {
        return this.start_policy;
    }

    public void set_start_policy(StartPolicyType start_policy){
        this.start_policy = start_policy;
    }

    public int get_node_id()
    {
        return this.node_id;
    }

    public void set_node_id( int node_id )
    {
        this.node_id = node_id;
    }

    public List<String> get_predecessors()
    {
        return this.predecessors_names;
    }

    public void add_predecessor( String predecessor_name )
    {
        if ( false == this.predecessors_names.contains( predecessor_name ) )
        {
            this.predecessors_names.add( predecessor_name );
        }
    }

    public int  get_concurrent_process_id()
    {
        return this.concurrent_process_id;
    }
    public void set_concurrent_process_id( int concurrent_process_id )
    {
        this.concurrent_process_id = concurrent_process_id;
    }

}
