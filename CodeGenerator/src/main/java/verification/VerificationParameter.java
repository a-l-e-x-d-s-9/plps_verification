package verification;

/**
 * Created by alexds9 on 12/03/17.
 */
public class VerificationParameter {

    public enum VerificationParameterType {
        type_execution,
        type_input,
        type_output,
        type_unobservable
    }

    public enum VerificationValueType {
        value_integer,
        value_real,
        value_boolean
    }

    public VerificationParameterType    parameter_type;
    public int                          variable_id;



}
