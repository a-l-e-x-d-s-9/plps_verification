package cs.bgu.maorash.plps.effects;

import cs.bgu.maorash.plps.etc.ParamHolder;
import cs.bgu.maorash.plps.etc.Predicate;

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

    @Override
    public boolean sharesParams(ParamHolder ph) {
        return effect.sharesParams(ph);
    }

    @Override
    public boolean containsParam(String paramName) {
        return effect.containsParam(paramName);
    }

    public String toString() {
        return "[Not " + effect.toString() + "]";
    }

}
