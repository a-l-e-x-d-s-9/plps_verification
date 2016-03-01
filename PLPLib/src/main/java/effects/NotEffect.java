package effects;

import plpEtc.ParamHolder;
import plpEtc.Predicate;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public class NotEffect implements Effect {

    private Predicate effect;

    public NotEffect(Predicate effect) {
        this.effect = effect;
    }

    public Predicate getEffect() {
        return effect;
    }

    public boolean sharesParams(ParamHolder ph) {
        return effect.sharesParams(ph);
    }

    public boolean containsParam(String paramName) {
        return effect.containsParam(paramName);
    }

    public String toString() {
        return "[Not " + effect.toString() + "]";
    }

    @Override
    public String simpleString() {
        return "not_".concat(effect.simpleString());
    }
}
