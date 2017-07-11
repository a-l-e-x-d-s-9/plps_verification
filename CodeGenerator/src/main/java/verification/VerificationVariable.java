package verification;

import java.util.HashMap;

/**
 * Created by alexds9 on 12/03/17.
 */
public class VerificationVariable {

    public enum VerificationVariableType {
        type_constant,
        type_regular,
        type_resource,
        type_predicate,
        type_parameter,
        type_concurrent_module,
        type_control
    }

    public enum VerificationValueType {
        value_integer,
        value_real,
        value_boolean
    }

    private static HashMap<VerificationVariableType, String> variable_type_names =  new HashMap() {
        {
            put( VerificationVariableType.type_constant         ,   "type_constant"     );
            put( VerificationVariableType.type_regular          ,   "type_regular"      );
            put( VerificationVariableType.type_resource         ,   "type_resource"     );
            put( VerificationVariableType.type_predicate        ,   "type_predicate"    );
            put( VerificationVariableType.type_parameter        ,   "type_parameter"    );
            put( VerificationVariableType.type_concurrent_module,   "type_module"       );
            put( VerificationVariableType.type_control          ,   "type_control"       );
            put( null                                           ,   "type_undefined"    );
        }
    };

    private static HashMap<VerificationValueType, String> value_type_names =  new HashMap() {
        {
            put( VerificationValueType.value_integer    ,   "value_integer"         );
            put( VerificationValueType.value_real       ,   "value_real"            );
            put( VerificationValueType.value_boolean    ,   "value_boolean"         );
            put( null                                   ,   "value_undefined"       );
        }
    };

    public VerificationVariableType variable_type;
    public VerificationValueType    value_type;
    public int                      value;
    public boolean                  is_set;
    public boolean                  is_in_range;
    public int                      min_value;
    public int                      max_value;
    public boolean                  is_exclusive_access;
    public int                      plp_id;

    public VerificationVariable()
    {
        this.variable_type  = VerificationVariableType.type_parameter;
        this.value_type     = VerificationValueType.value_integer;
        this.value          = 0;
        this.is_set         = false;
        this.is_in_range    = false;
        this.min_value      = 0;
        this.max_value      = 0;
    }

    public VerificationVariable( VerificationVariableType variable_type, VerificationValueType value_type,
                                 int value, boolean is_set, boolean is_in_range, int min_value, int max_value )
    {
        this.variable_type          = variable_type;
        this.value_type             = value_type;
        this.value                  = value;
        this.is_set                 = is_set;
        this.is_in_range            = is_in_range;
        this.min_value              = min_value;
        this.max_value              = max_value;
        this.is_exclusive_access    = true;
    }

    public VerificationVariable( VerificationVariableType variable_type, VerificationValueType value_type,
                                 int value, boolean is_set, boolean is_in_range, int min_value, int max_value,
                                 boolean is_exclusive_access )
    {
        this( variable_type, value_type, value, is_set, is_in_range, min_value, max_value );
        this.is_exclusive_access    = is_exclusive_access;
    }

    public String variable_type_to_name()
    {
        return this.variable_type_names.get(this.variable_type);
    }

    public String value_type_to_name()
    {
        return this.value_type_names.get(this.value_type);
    }


}
