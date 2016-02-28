package cs.bgu.maorash.plps.conditions;

import cs.bgu.maorash.plps.effects.Effect;
import cs.bgu.maorash.plps.etc.ParamHolder;
import cs.bgu.maorash.plps.plpFields.ObservationGoal;


public interface Condition extends ObservationGoal, ParamHolder {
    Effect createProperEffect();
}
