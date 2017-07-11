package verification;

/**
 * Created by alexds9 on 09/06/17.
 */
public class VerificationReports {
    private StringBuffer warnings_buffer    = new StringBuffer();
    private int          warnings_counter   = 0;
    private StringBuffer info_buffer        = new StringBuffer();
    private int          info_counter       = 0;

    private void message_add( StringBuffer messages_queque, int messages_counter, String new_message )
    {
        int warning_length  = new_message.length();
        int offset;

        messages_queque.append( String.format("%4d|%-95.95s\n", messages_counter, new_message ) );
        offset = 95;
        while ( offset < warning_length ){
            messages_queque.append( String.format("    |%-95.95s\n", new_message.substring(offset) ) );
            offset += 95;
        }
    }

    public void add_warning( String new_warning )
    {
        message_add( this.warnings_buffer, this.warnings_counter, new_warning );

        this.warnings_counter++;
    }

    public void add_info( String new_info )
    {
        message_add( this.info_buffer, this.info_counter, new_info );

        this.info_counter++;
    }

    public void print_warnings(){
        if ( this.warnings_counter > 0 )
        {
            System.out.println("********************************************* Warnings *********************************************");
            System.out.println(" ID | Warnings");
            System.out.println("----------------------------------------------------------------------------------------------------");
            System.out.print( this.warnings_buffer );
        }
        else
        {
            System.out.println("******************************************* No Warnings ********************************************");
        }
        System.out.println("****************************************************************************************************");
    }

    public void print_info(){
        if ( this.info_counter > 0 )
        {
            System.out.println("*********************************************** Info ***********************************************");
            System.out.println(" ID | Info");
            System.out.println("----------------------------------------------------------------------------------------------------");
            System.out.print( this.info_buffer );
        }
        else
        {
            System.out.println("********************************************* No Info **********************************************");
        }
        System.out.println("****************************************************************************************************");
    }

}
