package effects;


import conditions.Condition;
import plpEtc.ParamHolder;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class ConditionalEffect implements Effect {

    private Condition condition;
    private Effect effect;

    public ConditionalEffect(Condition condition, Effect effect) {
        this.condition = condition;
        this.effect = effect;
    }

    public Condition getCondition() {
        return condition;
    }

    public Effect getEffect() {
        return effect;
    }

    public boolean isConditional() {
        return condition != null;
    }

    public boolean sharesParams(ParamHolder ph) {
        return effect.sharesParams(ph);
        // TODO: decide if to include the condition
    }

    public boolean containsParam(String paramName) {
       return effect.containsParam(paramName);
        // TODO: decide if to include the condition
    }

    @Override
    public String toString() {
        return "[when " + effect.toString() + " -> " + condition.toString() + "]";
    }

    @Override
    public String simpleString() {
        return condition.simpleString()+"_"+effect.simpleString();
    }
}
