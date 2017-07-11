package verification;

/**
 * Created by alexds9 on 02/06/17.
 */
public class StringBufferExtra {
    static void replace(StringBuffer origin_buffer, String find_string, String replace_string) {
        int found_index = origin_buffer.indexOf(find_string);

        if (-1 < found_index) {
            origin_buffer.delete(found_index, found_index + find_string.length());
            origin_buffer.insert(found_index, replace_string);
        }
    }

    static void replace_all(StringBuffer origin_buffer, String find_string, String replace_string) {
        while (-1 < origin_buffer.indexOf(find_string)) {
            replace(origin_buffer, find_string, replace_string);
        }
    }
}
