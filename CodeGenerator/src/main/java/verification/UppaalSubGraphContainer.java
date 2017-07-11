package verification;

import java.awt.Point;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by alexds9 on 05/06/17.
 */
public class UppaalSubGraphContainer {

    public List<UppaalLocation>     locations;
    public List<UppaalBranchpoint>  branchpoints;
    public List<UppaalTransition>   transitions;
    public String                   init_location;

    public UppaalSubGraphContainer(){
        this.locations      = new LinkedList<>();
        this.branchpoints   = new LinkedList<>();
        this.transitions    = new LinkedList<>();
        this.init_location  = "";
    }

    public UppaalSubGraphContainer(List<UppaalLocation> locations, List<UppaalTransition> transitions ){
        this.locations      = locations;
        this.branchpoints   = branchpoints;
        this.transitions    = transitions;
    }

    private String location_id_use_allocated( HashMap<String,String> locations_names_to_ids, String location_to_check )
    {
        if ( true == locations_names_to_ids.containsKey(location_to_check) )
        {
            return locations_names_to_ids.get( location_to_check );
        }
        else
        {
            return location_to_check;
        }
    }

    public void replace_extra_locations( String replace_string, String replace_with )
    {
        for ( UppaalLocation location : this.locations )
        {
            if ( -1 < location.extra.indexOf(replace_string) )
            {
                location.extra = location.extra.replace( replace_string, replace_with );
            }
        }
    }

    public void replace_extra_transitions( String replace_string, String replace_with )
    {
        for ( UppaalTransition transition : this.transitions )
        {
            if ( -1 < transition.extra.indexOf(replace_string) )
            {
                transition.extra = transition.extra.replace( replace_string, replace_with );
            }
        }
    }

    public void add_sub_graph( UppaalPTA uppaal_pta, Point offset, String locations_suffix )
    {
        HashMap<String,String> locations_names_to_ids = new HashMap<>();

        if ( null != this.locations )
        {
            for ( UppaalLocation location : this.locations )
            {
                String original_name    = location.location_name;
                location.location_name += locations_suffix;
                locations_names_to_ids.put( original_name, location.add_location( uppaal_pta, offset ) );
            }
        }
        if ( null != this.branchpoints )
        {
            for ( UppaalBranchpoint branchpoint : this.branchpoints )
            {
                branchpoint.append( uppaal_pta, offset );
            }
        }

        if ( null != this.transitions )
        {
            for ( UppaalTransition transition : this.transitions )
            {
                transition.source_id = location_id_use_allocated( locations_names_to_ids, transition.source_id );
                transition.target_id = location_id_use_allocated( locations_names_to_ids, transition.target_id );
                transition.add_transition( uppaal_pta, offset );
            }
        }

        if ( false == this.init_location.isEmpty() )
        {
            uppaal_pta.set_init_location_id( location_id_use_allocated( locations_names_to_ids, this.init_location ) );
        }


    }
}