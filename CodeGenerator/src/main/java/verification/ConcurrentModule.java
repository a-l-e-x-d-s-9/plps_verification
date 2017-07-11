package verification;

/**
 * Created by alexds9 on 22/06/17.
 */
public class ConcurrentModule {
    private int concurrent_processes_amount = 0;

    public int allocate_concurrent_process_id()
    {
        return allocate_concurrent_process_id(1);
    }

    public int allocate_concurrent_process_id( int processes_amount )
    {
        int process_id = this.concurrent_processes_amount;

        this.concurrent_processes_amount += processes_amount;

        return process_id;
    }

    public void generate_concurrent_module_update( StringBuffer verification_modules_end ) {
        StringBuffer concurrent_module_update                           = new StringBuffer();
        StringBuffer concurrent_module_guard_no_one_wants_to_access     = new StringBuffer();
        StringBuffer concurrent_module_guard_any_one_wants_to_access    = new StringBuffer();

        int last_plp_index = concurrent_processes_amount - 1;
        for (int i = 0; i <= last_plp_index; i++) {
            concurrent_module_update.append( "\t\t\n" +
                    "\t\t<transition>\n" +
                    "\t\t\t<source ref=\"id71\"/>\n" +
                    "\t\t\t<target ref=\"id69\"/>\n" +
                    "\t\t\t<label kind=\"guard\" x=\"-825\" y=\"" + (-320 + (i * 30)) + "\">concurrent_info.is_process_would_like_to_access[" + String.format("%d", i) + "]</label>\n" +
                    "\t\t\t<label kind=\"assignment\" x=\"-755\" y=\"" + (-300 + (i * 30)) + "\">process_access_granted_id = " + String.format("%d", i) + "</label>\n" +
                    "\t\t\t<nail x=\"-860\" y=\"" + (-300 + (i * 30)) + "\"/>\n" +
                    "\t\t\t<nail x=\"-420\" y=\"" + (-300 + (i * 30)) + "\"/>\n" +
                    "\t\t</transition>\n" );

            concurrent_module_guard_no_one_wants_to_access.append(  "(concurrent_info.is_process_would_like_to_access[" + String.format("%d", i) + "] == false)" );
            concurrent_module_guard_any_one_wants_to_access.append( "(concurrent_info.is_process_would_like_to_access[" + String.format("%d", i) + "] == true)" );
            if ( i != last_plp_index )
            {
                concurrent_module_guard_no_one_wants_to_access.append(  " " + UppaalBuilder.STR_AND + "\n" );
                concurrent_module_guard_any_one_wants_to_access.append( " " + UppaalBuilder.STR_OR  + "\n" );
            }
        }

        StringBufferExtra.replace( verification_modules_end, "[<[PROCESSES_TRANSITIONS_TO_CRITICAL]>]", concurrent_module_update.toString());
        StringBufferExtra.replace( verification_modules_end, "[<[NO_ONE_WANTS_TO_ACCESS]>]",            concurrent_module_guard_no_one_wants_to_access.toString());
        StringBufferExtra.replace( verification_modules_end, "[<[ANY_ONE_WANTS_TO_ACCESS]>]",           concurrent_module_guard_any_one_wants_to_access.toString());
    }

    int get_concurrent_processes_amount()
    {
        return this.concurrent_processes_amount;
    }
}
