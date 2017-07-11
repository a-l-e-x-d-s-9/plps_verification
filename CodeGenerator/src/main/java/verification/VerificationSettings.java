package verification;

import java.util.HashMap;

/**
 * Created by alexds9 on 09/06/17.
 */
public class VerificationSettings {

    public final String SETTING_PRECISION_MULTIPLIER_FOR_NUMBERS_AND_TIME  = "precision_multiplier_for_numbers_and_time";
    public final String SETTING_OBSERVE_VARIABLE_SAMPLES                   = "observe_variable_samples";
    public final String RUN_TIME_AMOUNT_OF_INTERVALS_FOR_DISCRETIZATION    = "run_time_amount_of_intervals_for_discretization";
    public final String MAXIMUM_RUN_TIME_OF_PLP                            = "maximum_run_time_of_plp";


    private HashMap<String, String> settings = new HashMap() {
        {
            put( SETTING_PRECISION_MULTIPLIER_FOR_NUMBERS_AND_TIME      , "100"     );
            put( SETTING_OBSERVE_VARIABLE_SAMPLES                       , "5"       );
            put( RUN_TIME_AMOUNT_OF_INTERVALS_FOR_DISCRETIZATION        , "5"       );
            put( MAXIMUM_RUN_TIME_OF_PLP                                , "100"     );
        }
    };

    public void set( String setting_name, String setting_value)
    {
        this.settings.replace(setting_name, setting_value);
    }
    public boolean is_exist_setting( String setting_name)
    {
        return this.settings.containsKey(setting_name);
    }

    public int get_int( String setting_name )
    {
        return Integer.parseInt( this.settings.get(setting_name) );
    }

    public String get_string( String setting_name )
    {
        return this.settings.get(setting_name);
    }

}
