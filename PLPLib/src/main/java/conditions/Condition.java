package conditions;


import effects.Effect;
import plpEtc.ParamHolder;
import plpFields.ObservationGoal;

public interface Condition extends ObservationGoal, ParamHolder {
    Effect createProperEffect();
    String simpleString();
}