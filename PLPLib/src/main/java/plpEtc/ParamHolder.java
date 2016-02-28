package plpEtc;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public interface ParamHolder {
    boolean sharesParams(ParamHolder ph);
    boolean containsParam(String paramName);
}
