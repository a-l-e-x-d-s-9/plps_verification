package cs.bgu.maorash.plps.plpFields;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public interface ObservationGoal {
    boolean containsParam(PLPParameter param);

/*    private Condition cond;
    private String parameter;

    public ObservationGoal(Condition c){
        this.cond = c;
    }

    public ObservationGoal(String parameter) {
        this.parameter = parameter;
    }

    public boolean isParameter(){
        return parameter!=null;
    }

    public Condition getCond() {
        return cond;
    }

    public String getParameter() {
        return parameter;
    }

    public String toString() {
        if (isParameter())
            return parameter;
        return cond.toString();
    }

    public boolean containsParam(String param) {
        if (isParameter()) return param.equals(parameter);
        else return cond.containsParam(param);
    }*/
}
