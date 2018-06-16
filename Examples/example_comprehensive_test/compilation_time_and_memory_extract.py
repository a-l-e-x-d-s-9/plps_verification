#!/usr/bin/env python
# -*- coding: UTF-8 -*-
import os
import re

def main(): 
    kb_in_gb = 1024 ** 2
    results_times = "Times:\n"
    results_sizes = "Sizes:\n"
    
    for depth in [2,3,4,5,6,7,8,9,10,11,12,13]:
        for iteration in [1,2,3,4,5,6,7,8,9,10]:
            time_data_file_name = "time_compilation_depth_{depth}_iteration_{iteration}".format(depth=depth,iteration=iteration)
            if os.path.exists(time_data_file_name):
                time_data_file = open( time_data_file_name, 'r' )
                time_data_text = time_data_file.read()
                
                time_elapsed = re.search('Elapsed \(wall clock\) time \(h:mm:ss or m:ss\): ([0-9]+):([0-9\.]+)\n', time_data_text, re.MULTILINE | re.DOTALL)
                
                delimiter = "\t"
                if iteration == 10:
                    delimiter = "\n"
                
                if ( None != time_elapsed ):
                    time_elapsed_minutes    = int(time_elapsed[1])
                    time_elapsed_seconds    = float(time_elapsed[2])
                    #print("time_elapsed_minutes: " + str(time_elapsed_minutes) )
                    #print("time_elapsed_seconds: " + str(time_elapsed_seconds) )
                    time_elapsed_seconds += 60 * time_elapsed_minutes
                    #"{0:.2f}".format(round(a,2))
                    results_times += "{0:.2f}".format(round(time_elapsed_seconds,2)) + delimiter
                else:
                    raise ImportError( "can not find time_elapsed in: " + time_data_file_name )

                
                size_in_kb = re.search('Maximum resident set size \(kbytes\): ([0-9]+)\n', time_data_text, re.MULTILINE | re.DOTALL)
                          
                if ( None != size_in_kb ):
                    size_in_gb    = int(size_in_kb[1]) / kb_in_gb
                    #print("size_in_kb[1]: " + str(size_in_kb[1]) )
                    #print("size_in_gb: " + str(size_in_gb) )
                    
                    results_sizes += "{0:.3f}".format(round(size_in_gb,3)) + delimiter
                    
                    #"{0:.2f}".format(round(a,2))
                else:
                    raise ImportError( "can not find time_elapsed in: " + time_data_file_name )
                
                time_data_file.close()
                
            else:
                raise ImportError( "does not exist file: " + time_data_file_name )

    
    print( results_times )
    print( "---" )
    print( results_sizes )
    print( "---" )
main()
