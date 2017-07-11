package verification;

import java.util.List;

/**
 * Created by alexds9 on 12/06/17.
 */
public abstract class ControlNodeInterface {
    public enum StartPolicyType{
        all_predecessor_done,
        any_predecessor_done
    }
    public enum ControlNodeType{
        node_probability,
        node_concurrent,
        node_sequential,
        node_condition
    }

    static public StartPolicyType start_policy_enum_from_string( String value )
    {
        if ( true == value.equals( "all_predecessor_done" ) )
        {
            return StartPolicyType.all_predecessor_done;
        }
        else
        {
            return StartPolicyType.any_predecessor_done;
        }
    }

    abstract public ControlNodeType get_node_kind();
    abstract public String          get_node_name();
    abstract public void            set_node_name( String node_name );
    abstract public StartPolicyType get_start_policy();
    abstract public void            set_start_policy(StartPolicyType start_policy);
    abstract public int             get_node_id();
    abstract public void            set_node_id( int node_id );
    abstract public List<String>    get_predecessors();
    abstract public void            add_predecessor( String predecessor_name );
    abstract public int             get_concurrent_process_id();
    abstract public void            set_concurrent_process_id( int node_id );
}
