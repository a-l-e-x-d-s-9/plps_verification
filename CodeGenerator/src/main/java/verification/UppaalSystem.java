package verification;

/**
 * Created by alexds9 on 09/06/17.
 */
public class UppaalSystem {

    static public String uppaal_sync_signal_send( String channel )
    {
        return String.format( "%s!", channel );
    }

    static public String uppaal_sync_signal_receive( String channel )
    {
        return String.format( "%s?", channel );
    }
}
