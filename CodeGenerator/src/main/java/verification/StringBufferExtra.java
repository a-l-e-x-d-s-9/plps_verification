package verification;

/**
 * Created by alexds9 on 02/06/17.
 */
public class StringBufferExtra {
    static void replace(StringBuffer origin_buffer, int at_index, int delete_amount, String replace_string ) {
        origin_buffer.delete( at_index, at_index + delete_amount );
        origin_buffer.insert( at_index, replace_string );
    }

    static void replace(StringBuffer origin_buffer, String find_string, String replace_string) {
        int found_index = origin_buffer.indexOf(find_string);

        if (-1 < found_index) {
            // origin_buffer.delete(found_index, found_index + find_string.length());
            // origin_buffer.insert(found_index, replace_string);
            replace( origin_buffer, found_index, find_string.length(), replace_string );
        }
    }

    static void replace_all(StringBuffer origin_buffer, String find_string, String replace_string) {
        while (-1 < origin_buffer.indexOf(find_string)) {
            replace(origin_buffer, find_string, replace_string);
        }
    }

    static void replace_all_uniqueque_variables(StringBuffer origin_buffer, String find_string, String replace_string) {
        int offset = 0;
        int found_index = origin_buffer.indexOf( find_string, offset );

        while ( ( offset < origin_buffer.length()  ) &&
                ( -1 < found_index                 )  ) {
            boolean is_before_ok = ( 0 == found_index ) || origin_buffer.substring(found_index - 1,found_index ).matches("[^a-zA-Z0-9_]");
            boolean is_after_ok  = ( found_index + find_string.length() == origin_buffer.length() ) || origin_buffer.substring(found_index+find_string.length(),found_index+find_string.length()+1).matches("[^a-zA-Z0-9_]");

            offset = found_index;

            if ( is_before_ok && is_after_ok ) {
                replace(origin_buffer, find_string, replace_string);
                offset += replace_string.length();
            }
            else
            {
                offset += find_string.length();
            }

            found_index = origin_buffer.indexOf( find_string, offset );
        }
    }
}
