package conditions;

import effects.Effect;
import effects.NotEffect;
import plpEtc.ParamHolder;
import plpEtc.Predicate;

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

    public boolean containsParam(String paramName) {
        return condition.containsParam(paramName);
    }

    public boolean sharesParams(ParamHolder c) {
        return condition.sharesParams(c);
    }

    public Effect createProperEffect() {
        if (!condition.getClass().isAssignableFrom(Predicate.class)) {
            throw new UnsupportedOperationException("Can't treat condition "+toString()+" as an action effect, " +
                    "the inner condition needs to be a Predicate");
        }
        return new NotEffect((Predicate) condition);

    }

    @Override
    public String simpleString() {
        return "not_".concat(condition.simpleString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this.getClass().isInstance(obj)) {
            return condition.equals(((NotCondition) obj).condition);
        }
        return false;
    }
}
