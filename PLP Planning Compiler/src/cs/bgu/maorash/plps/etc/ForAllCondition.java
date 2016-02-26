package cs.bgu.maorash.plps.etc;

import cs.bgu.maorash.compiler.PDDLCompiler;
import cs.bgu.maorash.plps.plpFields.PLPParameter;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ForAllCondition implements Condition {

    private List<String> params;
    private Condition condition;

    public ForAllCondition(Condition c) {
        params = new LinkedList<>();
        this.condition = c;
    }

    public Condition getCondition() {
        return condition;
    }

    public void addParam(String param){
        params.add(param);
    }

    public String toString() {
        return "[For All " + Arrays.toString(params.toArray()) +
                " " + condition + "]";
    }

    public List<String> getForAllParams() {return params;}

    @Override
    public boolean containsParam(PLPParameter param) {
        return condition.containsParam(param);
    }

    @Override
    public boolean sharesParams(Condition c) {
        return condition.sharesParams(c);
    }

    @Override
    public String toPDDL() {
        return PDDLCompiler.compile(this);
    }
}
