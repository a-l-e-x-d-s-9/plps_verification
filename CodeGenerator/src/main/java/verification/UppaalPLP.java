package verification;

import modules.*;

/**
 * Created by alexds9 on 02/06/17.
 */
public class UppaalPLP extends UppaalPTA
{

    private static final int    plp_achieve_available_location_id_first     = 37;
    private static final int    plp_maintain_available_location_id_first    = 76;
    private static final int    plp_observe_available_location_id_first     = 108;
    private static final int    plp_detect_available_location_id_first      = 138;

    private StringBuffer plp_template = null;

    public int          id;
    //public String       id_padded;
    public String       name;
    public StringBuffer precondition;
    // public StringBuffer goal;
    public StringBuffer termination_failure_condition;
    public StringBuffer termination_failure_notify_transitions;
    public StringBuffer termination_success_condition;
    public StringBuffer termination_success_notify_transitions;
    public StringBuffer transition_to_main_success_labels;
    public StringBuffer main_success_labels;


    UppaalPLP( PLP plp, int id, VerificationVariableManager variable_manager )
    {
        super(variable_manager);

        this.id                                     = id;
        this.plp_template                           = new StringBuffer();
        this.precondition                           = new StringBuffer();
        this.termination_failure_condition          = new StringBuffer();
        this.termination_failure_notify_transitions = new StringBuffer();
        this.termination_success_condition          = new StringBuffer();
        this.termination_success_notify_transitions = new StringBuffer();
        this.transition_to_main_success_labels      = new StringBuffer();
        this.main_success_labels                    = new StringBuffer();

        if ( AchievePLP.class.isInstance(plp) )
        {
            this.location_id_free = plp_achieve_available_location_id_first;
        }
        else if ( MaintainPLP.class.isInstance(plp) )
        {
            this.location_id_free = plp_maintain_available_location_id_first;
        }
        else if ( ObservePLP.class.isInstance(plp) )
        {
            this.location_id_free = plp_observe_available_location_id_first;
        }
        else if ( DetectPLP.class.isInstance(plp) )
        {
            this.location_id_free = plp_detect_available_location_id_first;
        }
    }

    public void insert_plp_template( String plp_template )
    {
        this.plp_template = new StringBuffer( plp_template );
    }

    public String local_variable_name( int variable_id )
    {
        return String.format( "_variable_%04d", variable_id);
    }



    public String get_new_location_id()
    {
        return "id" + String.valueOf(this.location_id_free++);
    }

    public int get_id()
    {
        return this.id;
    }

    static public String get_plp_start_channel()
    {
        return "control_plp_start[id]";
    }

    static public String get_plp_start_channel( int plp_id )
    {
        return String.format( "control_plp_start[%d]", plp_id );
    }

    static public String get_plp_done_channel()
    {
        return "control_plp_done[id]";
    }

    static public String get_plp_done_channel( int plp_id )
    {
        return String.format( "control_plp_done[%d]", plp_id );
    }

    public void replace_all_in_plp_template( String find_string, String replace_string )
    {
        StringBufferExtra.replace_all( this.plp_template, find_string, replace_string );

    }

    public StringBuffer seal_for_generation()
    {
        StringBufferExtra.replace_all( this.plp_template, "[<[plp_name]>]",                                 this.name );
        StringBufferExtra.replace_all( this.plp_template, "[<[locations]>]",                                this.locations.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[branchpoints]>]",                             this.branchpoints.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[transitions]>]",                              this.transitions.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[declarations]>]",                             this.declarations.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[precondition]>]",                             this.precondition.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[termination_failure_condition]>]",            this.termination_failure_condition.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[termination_failure_notify_transitions]>]",   this.termination_failure_notify_transitions.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[termination_success_condition]>]",            this.termination_success_condition.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[termination_success_notify_transitions]>]",   this.termination_success_notify_transitions.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[transition_to_main_success_labels]>]",        this.transition_to_main_success_labels.toString() );
        StringBufferExtra.replace_all( this.plp_template, "[<[main_success_labels]>]",                      this.main_success_labels.toString() );

        return this.plp_template;
    }
}
