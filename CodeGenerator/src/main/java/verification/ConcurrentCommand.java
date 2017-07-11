package verification;

/**
 * Created by alexds9 on 03/06/17.
 */
public class ConcurrentCommand
{
    public enum CommandType {
        type_read,
        type_write,
        type_watch_add,
        type_watch_remove
    }

    public CommandType  type;
    public int          global_variable_id_to_watch;
    public String       general_io_variable;
    public String       request_bound_lower;
    public String       request_bound_upper;
    public boolean      request_is_single_range;
    public String       variable_for_request_id;
    public String       variable_with_request_data;
    public String       message;

    ConcurrentCommand()
    {
        this.message = null;
    }

    public ConcurrentCommand make_read( int global_variable_id_to_read, String variable_read_to )
    {
        this.type                           = CommandType.type_read;
        this.global_variable_id_to_watch    = global_variable_id_to_read;
        this.general_io_variable            = variable_read_to;

        return this;
    }

    public ConcurrentCommand make_read( int global_variable_id_to_read, String variable_read_to,
                                        String message )
    {
        make_read( global_variable_id_to_read, variable_read_to );
        this.message = UppaalBuilder.comply_string_single_line( message );
        return this;
    }

    public ConcurrentCommand make_write( int global_variable_id_to_write, String variable_write_from )
    {
        this.type                           = CommandType.type_write;
        this.global_variable_id_to_watch    = global_variable_id_to_write;
        this.general_io_variable            = variable_write_from;

        return this;
    }

    public ConcurrentCommand make_write( int global_variable_id_to_write, String variable_write_from,
                                         String message )
    {
        make_write( global_variable_id_to_write, variable_write_from );
        this.message = UppaalBuilder.comply_string_multi_line( message );
        return this;
    }


    public ConcurrentCommand make_watch_add( int global_variable_id_to_watch, String variable_for_request_id, String request_bound_lower,
                                             String request_bound_upper, boolean request_is_single_range, String variable_with_request_data )
    {
        this.type                           = CommandType.type_watch_add;
        this.global_variable_id_to_watch    = global_variable_id_to_watch;
        this.variable_for_request_id        = variable_for_request_id;
        this.request_bound_lower            = request_bound_lower;
        this.request_bound_upper            = request_bound_upper;
        this.request_is_single_range        = request_is_single_range;
        this.variable_with_request_data     = variable_with_request_data;

        return this;
    }

    public ConcurrentCommand make_watch_add( int global_variable_id_to_watch, String variable_for_request_id,
                                             int request_bound_lower, int request_bound_upper,
                                             boolean request_is_single_range, String variable_with_request_data )
    {
        return make_watch_add( global_variable_id_to_watch, variable_for_request_id, String.valueOf( request_bound_lower ),
                String.valueOf(  request_bound_upper ), request_is_single_range, variable_with_request_data );
    }

    public ConcurrentCommand make_watch_add( int global_variable_id_to_watch, String variable_for_request_id,
                                             int request_bound_lower, int request_bound_upper,
                                             boolean request_is_single_range, String variable_with_request_data,
                                             String message )
    {
        make_watch_add( global_variable_id_to_watch, variable_for_request_id, String.valueOf( request_bound_lower ),
                String.valueOf(  request_bound_upper ), request_is_single_range, variable_with_request_data );
        this.message = UppaalBuilder.comply_string_multi_line( message );
        return this;
    }

    public ConcurrentCommand make_watch_add( int global_variable_id_to_watch, String variable_for_request_id,
                                             String request_bound_lower, String request_bound_upper,
                                             boolean request_is_single_range, String variable_with_request_data,
                                             String message )
    {
        make_watch_add( global_variable_id_to_watch, variable_for_request_id, request_bound_lower,
                request_bound_upper, request_is_single_range, variable_with_request_data );
        this.message = UppaalBuilder.comply_string_multi_line( message );
        return this;
    }

    public ConcurrentCommand make_watch_remove(  String request_id_variable, String variable_to_return_is_satisfied )
    {
        this.type                       = CommandType.type_watch_remove;
        this.general_io_variable        = variable_to_return_is_satisfied;
        this.variable_for_request_id    = request_id_variable;

        return this;
    }

    public ConcurrentCommand make_watch_remove(  String request_id_variable, String variable_to_return_is_satisfied, String message )
    {
        make_watch_remove( request_id_variable, variable_to_return_is_satisfied );
        this.message = UppaalBuilder.comply_string_multi_line( message );
        return this;
    }


}