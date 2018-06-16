#!/usr/bin/env python
# -*- coding: UTF-8 -*-
import os
import re

def query_extract_data(query_result,query_type):
    kb_in_gb = 1024 ** 2
    
    result_duration         = ""
    result_resident_memory  = 0.0
    result_virtual_memory   = 0.0
    result_states           = ""
    result_runs             = ""


    search_satisfied = re.search('-- Formula is satisfied\.\n', query_result, re.MULTILINE | re.DOTALL)

    if ( None == search_satisfied ):
        raise ImportError( "can not find satisfied" )
        

    search_duration = re.search('Elapsed \(wall clock\) time \(h:mm:ss or m:ss\): ([0-9]+):([0-9\.]+)\n', query_result, re.MULTILINE | re.DOTALL)
                
    if ( None != search_duration ):
        
        duration_minutes    = int(search_duration[1])
        duration_seconds    = float(search_duration[2])
        
        duration_seconds    += 60 * duration_minutes
        
        result_duration     = "{0:.2f}".format(round(duration_seconds,2))
        
    else:
        raise ImportError( "can not find duration" )



    search_resident_memory = re.search('-- Resident memory used : ([0-9]+) KiB\n', query_result, re.MULTILINE | re.DOTALL)
          
    if ( None != search_resident_memory ):
        size_in_gb    = int(search_resident_memory[1]) / kb_in_gb
        
        result_resident_memory = "{0:.3f}".format(round(size_in_gb,3))

    else:
        raise ImportError( "can not find resident_memory" )
        
        
    
    search_virtual_memory = re.search('-- Virtual memory used : ([0-9]+) KiB\n', query_result, re.MULTILINE | re.DOTALL)
          
    if ( None != search_virtual_memory ):
        size_in_gb    = int(search_virtual_memory[1]) / kb_in_gb
        
        result_virtual_memory = "{0:.3f}".format(round(size_in_gb,3))

    else:
        raise ImportError( "can not find virtual_memory" )
        
        
    
    search_states = re.search('-- States explored : ([0-9]+) states\n', query_result, re.MULTILINE | re.DOTALL)
          
    if ( None != search_states ):
        result_states = int(search_states[1])

    else:
        raise ImportError( "can not find states" )
        
        
    if "probability" == query_type:
        search_runs = re.search('\(([0-9]+) runs\)', query_result, re.MULTILINE | re.DOTALL)
              
        if ( None != search_runs ):
            result_runs = int(search_runs[1])

        else:
            raise ImportError( "can not find runs" )
        
                  
    return result_duration, result_resident_memory, result_virtual_memory, result_states, result_runs
                    
def main(): 
    
    for query_type in ["probability", "exist"]:
        print("----- " + query_type + " -----")
        results_duration        = query_type + " Durations:\n"
        results_resident_memory = query_type + " Resident memory:\n"
        results_virtual_memory  = query_type + " Virtual memory:\n"
        results_states          = query_type + " States:\n"
        results_runs            = query_type + " Runs:\n"
        
        for depth in [2,3,4,5,6]:
            for iteration in [1,2,3,4,5,6,7,8,9,10]:
                query_result_file_name = "time_query_{query_type}_depth_{depth}_iteration_{iteration}".format(query_type=query_type,depth=depth,iteration=iteration)
                
                if os.path.exists(query_result_file_name):
                    query_result_file = open( query_result_file_name, 'r' )
                    query_result_text = query_result_file.read()
                    query_result_file.close()
                    
                    delimiter = "\t"
                    if iteration == 10:
                        delimiter = "\n"
                        
                    result_duration, result_resident_memory, result_virtual_memory, result_states, result_runs = query_extract_data(query_result_text,query_type)

                    results_duration        += str(result_duration)         + delimiter
                    results_resident_memory += str(result_resident_memory)  + delimiter
                    results_virtual_memory  += str(result_virtual_memory)   + delimiter
                    results_states          += str(result_states)           + delimiter
                    results_runs            += str(result_runs)             + delimiter

                    
                else:
                    raise ImportError( "does not exist file: " + query_result_file_name )

        
        print( results_duration )
        print( "---" )
        print( results_resident_memory )
        print( "---" )
        print( results_virtual_memory )
        print( "---" )
        print( results_states )
        print( "---" )
        print( results_runs )
        print( "---" )
main()
