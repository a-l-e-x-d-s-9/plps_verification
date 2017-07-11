package verification;

/**
 * Created by alexds9 on 15/06/17.
 */
public class UppaalPTA {
    protected StringBuffer  name;
    protected StringBuffer  parameters;
    protected StringBuffer  declarations;
    protected StringBuffer  locations;
    protected StringBuffer  init_location_id;
    protected StringBuffer  branchpoints;
    protected StringBuffer  transitions;

    protected int           location_id_free;
    protected int           variable_id_free;
    protected int           concurrent_request_id_free;
    protected VerificationVariableManager variable_manager;

    public UppaalPTA( VerificationVariableManager variable_manager ){

        this.variable_manager   = variable_manager;
        this.name               = new StringBuffer();
        this.parameters         = new StringBuffer();
        this.declarations       = new StringBuffer();
        this.locations          = new StringBuffer();
        this.init_location_id   = new StringBuffer();

        this.branchpoints       = new StringBuffer();
        this.transitions        = new StringBuffer();

        this.location_id_free   = 0;
        this.variable_id_free   = 0;
    }

    public StringBuffer get_name() {
        return this.name;
    }

    public void set_name( CharSequence name) {
        this.name = new StringBuffer( name );
    }

    public StringBuffer get_parameters() {
        return this.parameters;
    }

    public void append_parameters( StringBuffer parameters ) {
        this.parameters.append( parameters );
    }

    public StringBuffer get_declarations() {
        return this.declarations;
    }

    public void append_declarations( CharSequence declarations ) {
        this.declarations.append( declarations );
    }

    public StringBuffer get_locations() {
        return this.locations;
    }

    public void append_locations( CharSequence locations ) {
        this.locations.append( locations );
    }

    public StringBuffer get_branchpoints() {
        return this.branchpoints;
    }

    public void append_branchpoints(CharSequence branchpoints) {
        this.branchpoints.append( branchpoints );
    }

    public StringBuffer get_transitions() {
        return this.transitions;
    }

    public void append_transitions(CharSequence transitions) {
        this.transitions.append( transitions );
    }

    public StringBuffer get_init_location_id() {
        return this.init_location_id;
    }

    public void set_init_location_id( CharSequence init_location_id ) {
        this.init_location_id = new StringBuffer( init_location_id );
    }

    public String local_variable_name( int variable_id )
    {
        return String.format( "_variable_%04d", variable_id);
    }

    public String get_new_location_id()
    {
        return "id" + String.valueOf(this.location_id_free++);
    }

    public String local_concurrent_request_name( int concurrent_request_id )
    {
        return String.format( "_request_%04d", concurrent_request_id);
    }

    public String local_variable_add()
    {
        String new_local_variable = local_variable_name(this.variable_id_free++);
        this.declarations.append( String.format( "int %s;\n", new_local_variable ) );
        return new_local_variable;
    }

    public String local_concurrent_request_add()
    {
        this.variable_manager.concurrent_requests_add_one();

        String new_concurrent_request = local_concurrent_request_name(this.concurrent_request_id_free++);
        this.declarations.append( String.format( "concurrent_request_type %s;\n", new_concurrent_request ) );
        return new_concurrent_request;
    }

    public StringBuffer seal_pta()
    {
        StringBuffer uppaal_pta = new StringBuffer();

        uppaal_pta.append( "\t<template>\n" );
        uppaal_pta.append( "\t\t<name>" );
        uppaal_pta.append( this.name );
        uppaal_pta.append( "</name>\n" );
        uppaal_pta.append( "\t\t<parameter>" );
        uppaal_pta.append( this.parameters );
        uppaal_pta.append( "</parameter>\n" );
        uppaal_pta.append( "\t\t<declaration>\n" );
        uppaal_pta.append( this.declarations );
        uppaal_pta.append( "\t\t</declaration>\n" );
        uppaal_pta.append( this.locations );
        uppaal_pta.append( this.branchpoints );
        uppaal_pta.append( "\t\t<init ref=\"" );
        uppaal_pta.append( this.init_location_id );
        uppaal_pta.append( "\"/>\n" );
        uppaal_pta.append( this.transitions );
        uppaal_pta.append( "\t</template>\n" );

        return uppaal_pta;
    }
}
