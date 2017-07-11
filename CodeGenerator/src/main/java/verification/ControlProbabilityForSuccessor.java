package verification;

import conditions.Condition;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by alexds9 on 12/06/17.
 */
public class ControlProbabilityForSuccessor {
    public String           node_name;
    public int              probability;
    public List<Condition>  updates;


    public ControlProbabilityForSuccessor()
    {
        this.node_name      = "";
        this.probability    = 0;
        this.updates        = new LinkedList<>();
    }

    public ControlProbabilityForSuccessor( String node_name )
    {
        this.node_name      = node_name;
        this.probability    = 0;
        this.updates        = new LinkedList<>();
    }
}
