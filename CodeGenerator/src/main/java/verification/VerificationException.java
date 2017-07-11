package verification;

/**
 * Created by alexds9 on 23/03/17.
 */
public class VerificationException extends Exception {
    private String error_message;

    public VerificationException( String error_message )
    {
        this.error_message = error_message;
    }

    public String get_message()
    {
        return this.error_message;
    }
}
