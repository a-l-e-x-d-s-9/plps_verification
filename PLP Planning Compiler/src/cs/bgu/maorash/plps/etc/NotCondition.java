package cs.bgu.maorash.plps.etc;

import cs.bgu.maorash.compiler.PDDLCompiler;
import cs.bgu.maorash.plps.plpFields.PLPParameter;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class NotCondition implements Condition {
    Condition condition;

    public NotCondition (Condition c) {
        this.condition = c;
    }

    public Condition getCondition() {
        return condition;
    }

    public String toString() {
        return "[Not " + condition.toPDDL() + "]";
    }

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
