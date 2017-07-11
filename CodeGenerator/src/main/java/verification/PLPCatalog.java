package verification;

import java.util.HashMap;

/**
 * Created by alexds9 on 18/06/17.
 */
public class PLPCatalog {
    private HashMap<String, Integer>    map_plp_name_to_plp_id;
    private HashMap<Integer, String>    map_plp_id_to_plp_name;
    private int                         plp_id_counter;

    public PLPCatalog()
    {
        this.plp_id_counter                     = 0;

        this.map_plp_name_to_plp_id             = new HashMap<>();
        this.map_plp_id_to_plp_name             = new HashMap<>();
    }

    public int plp_map_add(String plp_name) {
        int current_id = this.plp_id_counter;
        this.plp_id_counter++;

        this.map_plp_name_to_plp_id.put(plp_name, current_id);
        this.map_plp_id_to_plp_name.put(current_id,plp_name);

        return current_id;
    }

    public boolean plp_id_is_valid( int plp_id ) {
        return ( ( 0 <= plp_id ) && ( plp_id < this.plp_id_counter ) );
    }

    public int get_plps_amount()
    {
        return this.plp_id_counter;
    }

    public int find_plp_id_by_name(String plp_name) {
        return this.map_plp_name_to_plp_id.get(plp_name);
    }
    public String find_plp_name_by_id(int plp_id) {
        return this.map_plp_id_to_plp_name.get( plp_id );
    }

    public boolean plp_name_is_exist(String plp_name ) {
        return this.map_plp_name_to_plp_id.containsKey(plp_name);
    }

}
