package cs.bgu.maorash.plps.effects;

import cs.bgu.maorash.plps.conditions.Condition;
import cs.bgu.maorash.plps.etc.ParamHolder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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


    @Override
    public boolean sharesParams(ParamHolder ph) {
        return effect.sharesParams(ph);
        // TODO: decide if to include the condition
    }

    @Override
    public boolean containsParam(String paramName) {
       return effect.containsParam(paramName);
        // TODO: decide if to include the condition
    }

    @Override
    public String toString() {
        return "[" + effect.toString() + "|" + condition.toString() + "]";
    }
}
