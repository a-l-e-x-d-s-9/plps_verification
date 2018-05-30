#!/usr/bin/env python
# -*- coding: UTF-8 -*-
import sys
import fileinput
import os
import re
from pathlib import Path

def get_args():
    if len( sys.argv ) != 4:
        raise IOError("Must get: 1. Control graph; 2. Source PLP; 3. Multiply number.")

    #</node_sequential>
    file_control_graph_input_path    = sys.argv[1]
    print( "argument: file_control_graph_input_path  : " + file_control_graph_input_path )

    file_source_plp_input_path   = sys.argv[2]
    print( "argument: file_source_plp_input_path : " + file_source_plp_input_path )

    multiply_number = int(sys.argv[3])

    if os.path.exists(file_control_graph_input_path):
        print( "found: file_control_graph_input_path     : " + file_control_graph_input_path )
        file_control_graph_input = open( file_control_graph_input_path, 'r+' )
    else:
        raise ImportError( "file_control_graph_input_path file does not exists: " + file_control_graph_input_path )

    if os.path.exists(file_source_plp_input_path):
        print( "found: file_source_plp_input_path     : " + file_source_plp_input_path )
        file_source_plp_input = open( file_source_plp_input_path, 'rt' )
    else:
        raise ImportError( "file_source_plp_input_path file does not exists: " + file_source_plp_input_path )

    plp_name = re.search("(.*/)([^/]*)\.xml",
                          file_source_plp_input_path, re.DOTALL)
                          
    if ( None != plp_name ):
        plps_path = plp_name[1]
        plp_name = plp_name[2]
        print("plps_path: " + plps_path)
        print("plp_name:  " + plp_name)


    return file_control_graph_input, file_source_plp_input, plp_name, plps_path, multiply_number


def main( single_node = True ):
    if sys.version_info[0] < 3: # Python 2 needs utf-8
        reload(sys)
        sys.setdefaultencoding('utf-8')

    file_control_graph_input, file_source_plp_input, plp_name, plps_path, multiply_number = get_args()
    source_plp_input_text = file_source_plp_input.read()
    control_graph_list = "\r"

    if True == single_node:
        control_graph_list += "<node_sequential   node_name=\"node_sequential_00001\" start_policy=\"any_predecessor_done\" next_node_name=\"\">\n"

    # Path("plps_path").glob('{name}_\\d\\d\\d\\d\\d.xml'.format(name=plp_name)):
    for file_path in Path(plps_path).glob('{name}_*.xml'.format(name=plp_name)):
        print("remove: " + str(file_path))
        os.remove(file_path)

    for i in range(1, multiply_number + 1):
        current_plp_file_name = '{name}_{num:05d}'.format(name=plp_name,num=i)
        current_plp_file_path = '{path}{name}.xml'.format(path=plps_path,name=current_plp_file_name)

        print( current_plp_file_path )
        current_plp_file = open(current_plp_file_path, 'wt')
        current_plp_file_text = source_plp_input_text
        current_plp_file_text = current_plp_file_text.replace("name=\"" + plp_name + "\"",
                                                              "name=\"" + current_plp_file_name + "\"")
        current_plp_file.write(current_plp_file_text)
        current_plp_file.close()

        if False == single_node:
            next_name = ""
            if i < multiply_number:
                next_name = "node_sequential_{next:05d}".format(next=i+1)

            control_graph_list += "<node_sequential   node_name=\"node_sequential_{num:05d}\" start_policy=\"any_predecessor_done\" next_node_name=\"{next}\">\n".format(num=i,next=next_name)

        control_graph_list += "    <run_plp plp_name=\""  + current_plp_file_name + "\" wait_time=\"0\"></run_plp>\n"

        if False == single_node:
            control_graph_list += "</node_sequential>\n"

    if True == single_node:
        control_graph_list += "</node_sequential>\n"

    control_graph_input_text = file_control_graph_input.read()

    file_control_graph_input.truncate(0) # .*</node_sequential>
    file_control_graph_input.seek(0)

    control_graph_input_text = re.sub(r"<node_sequential   node_name=\"[^\"]*\" start_policy=\"any_predecessor_done\"[ ]+next_node_name=\"[^\"]*\">[ \r\n]*", "",
                                      control_graph_input_text)
    control_graph_input_text = re.sub( r"</node_sequential>[ \r\n]*",
        "",
        control_graph_input_text)
    control_graph_input_text = re.sub(r"[ \r\n]*<run_plp plp_name=\"[^\"]+\"[ ]+wait_time=\"0\"></run_plp>[ \r\n]*", "",
                                      control_graph_input_text)


    control_graph_input_text = control_graph_input_text.replace("</control_graph>",
                                        control_graph_list + "</control_graph>" )


    file_control_graph_input.write(control_graph_input_text)


    file_control_graph_input.close()
    file_source_plp_input.close()
    print("Done")
    
    
main()

# Run example:
# ./multiply_ptas.py control_graph.xml plps/move_forward.xml 10
