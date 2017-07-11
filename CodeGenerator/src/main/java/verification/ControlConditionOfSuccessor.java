package verification;

import conditions.Condition;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by alexds9 on 12/06/17.
 */
public class ControlConditionOfSuccessor {
    public String           node_name;
    public List<Condition>  condition;
    public List<Condition>  updates;


    public ControlConditionOfSuccessor()
    {
        this.node_name  = "";
        this.updates    = new LinkedList<>();
    }

    public ControlConditionOfSuccessor(String id )
    {
        this.node_name  = id;
        this.updates    = new LinkedList<>();
    }
}
