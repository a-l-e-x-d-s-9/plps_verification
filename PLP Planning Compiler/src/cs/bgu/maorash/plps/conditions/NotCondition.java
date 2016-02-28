package cs.bgu.maorash.plps.conditions;

import cs.bgu.maorash.plps.effects.Effect;
import cs.bgu.maorash.plps.effects.NotEffect;
import cs.bgu.maorash.plps.etc.ParamHolder;
import cs.bgu.maorash.plps.etc.Predicate;

import java.util.LinkedList;
import java.util.List;

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
        return "[Not " + condition.toString() + "]";
    }

    @Override
    public boolean containsParam(String paramName) {
        return condition.containsParam(paramName);
    }

    @Override
    public boolean sharesParams(ParamHolder c) {
        return condition.sharesParams(c);
    }

    @Override
    public Effect createProperEffect() {
        if (!condition.getClass().isAssignableFrom(Predicate.class)) {
            throw new UnsupportedOperationException("Can't treat condition "+toString()+" as an action effect, " +
                    "the inner condition needs to be a Predicate");
        }
        return new NotEffect((Predicate) condition);

    }
}
