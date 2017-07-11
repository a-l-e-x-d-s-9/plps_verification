package verification;

import conditions.Condition;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by alexds9 on 12/06/17.
 */
public class ControlUpdateForPLP {
    public String           plp_name;
    public List<Condition>  updates;
    public int              wait_time;


    public ControlUpdateForPLP()
    {
        this.plp_name       = "";
        this.updates        = new LinkedList<>();
        this.wait_time      = 0;
    }

    public ControlUpdateForPLP( String plp_name )
    {
        this.plp_name       = plp_name;
        this.updates        = new LinkedList<>();
        this.wait_time      = 0;
    }
}
