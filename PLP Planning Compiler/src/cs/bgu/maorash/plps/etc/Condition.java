package cs.bgu.maorash.plps.etc;

import cs.bgu.maorash.plps.plpFields.ObservationGoal;
import cs.bgu.maorash.plps.plpFields.PLPField;
import cs.bgu.maorash.plps.plpFields.PLPParameter;

public interface Condition extends ObservationGoal, PLPField {
    boolean containsParam(PLPParameter param);
    boolean sharesParams(Condition c);
}
