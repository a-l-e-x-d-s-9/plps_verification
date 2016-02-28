package cs.bgu.maorash.plps.etc;

/**
 * Created by maorash
 * maorash@cs.bgu.ac.il
 */
public interface ParamHolder {
    boolean sharesParams(ParamHolder ph);
    boolean containsParam(String paramName);
}
