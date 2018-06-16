#!/usr/bin/env python
# -*- coding: UTF-8 -*-
import sys
import fileinput
import os
import re
from pathlib import Path

def get_args():
    if len( sys.argv ) != 5:
        raise IOError("Must get: 1. Control graph; 2. Configuration file; 3. Source PLPs folder; 4. Tree depth.")

    #</node_sequential>
    file_control_graph_input_path = sys.argv[1]
    print( "argument: file_control_graph_input_path  : " + file_control_graph_input_path )


    file_configuration_input_path = sys.argv[2]
    print( "argument: file_configuration_input_path  : " + file_configuration_input_path )

    plps_path   = sys.argv[3]
    print( "argument: file_source_plp_input_path : " + plps_path )

    depth_number = int(sys.argv[4])

    if os.path.exists(file_control_graph_input_path):
        print( "found: file_control_graph_input_path     : " + file_control_graph_input_path )
        file_control_graph_input = open( file_control_graph_input_path, 'r+' )
    else:
        raise ImportError( "file_control_graph_input_path file does not exists: " + file_control_graph_input_path )

    if os.path.exists(file_configuration_input_path):
        print( "found: file_configuration_input_path     : " + file_configuration_input_path )
        file_configuration_input = open( file_configuration_input_path, 'r+' )
    else:
        raise ImportError( "file_configuration_input_path file does not exists: " + file_configuration_input_path )

    if os.path.exists(plps_path):
        print( "found: plps_path     : " + plps_path )
        #file_source_plp_input = open( file_source_plps_input_path, 'rt' )
    else:
        raise ImportError( "plps_path folder does not exists: " + plps_path )

    ''' 
    plp_name = re.search("(.*/)([^/]*)\.xml",
                          file_source_plps_input_path, re.DOTALL)
                          
    if ( None != plp_name ):
        plps_path = plp_name[1]
        plp_name = plp_name[2]
        print("plps_path: " + plps_path)
        print("plp_name:  " + plp_name)
    '''

    return file_control_graph_input, file_configuration_input, plps_path, depth_number

def open_plp( plps_directory, plp_name):
    path_to_plp = os.path.join( plps_directory, plp_name )
    if os.path.exists(path_to_plp):
        print( "open plp     : " + path_to_plp )
        plp_file = open( path_to_plp, 'rt' )
    else:
        raise ImportError( "can not openplp: " + path_to_plp )

    return plp_file

def create_probability_tree( depth, root_index, free_index, list_leafs_indexes, text ):
    if depth < 1:
        return [ free_index, text ]

    elif depth == 1:
        reserved_couple_indexes = free_index
        text += ('<node_probability node_name="node_probability_{index_father:08d}" start_policy="any_predecessor_done" wait_time="0">\n'
        '\t<probability_for_successor_node probability="0.5" node_name="node_concurrent_{index_son_left:08d}"/>\n'
        '\t<probability_for_successor_node probability="0.5" node_name="node_concurrent_{index_son_right:08d}"/>\n'
        '</node_probability>\n').format(index_father=root_index,index_son_left=reserved_couple_indexes,index_son_right=reserved_couple_indexes+1)
        list_leafs_indexes.append(reserved_couple_indexes)
        list_leafs_indexes.append(reserved_couple_indexes+1)
        return [ free_index+2, text ]

    elif depth >= 1:
        reserved_couple_indexes = free_index
        text += ('<node_probability node_name="node_probability_{index_father:08d}" start_policy="any_predecessor_done" wait_time="0">\n'
        '\t<probability_for_successor_node probability="0.5" node_name="node_probability_{index_son_left:08d}"/>\n'
        '\t<probability_for_successor_node probability="0.5" node_name="node_probability_{index_son_right:08d}"/>\n'
        '</node_probability>\n').format(index_father=root_index,index_son_left=reserved_couple_indexes,index_son_right=reserved_couple_indexes+1)

        free_index, text = create_probability_tree(depth - 1, reserved_couple_indexes,       free_index + 2, list_leafs_indexes, text)
        free_index, text = create_probability_tree(depth - 1, reserved_couple_indexes + 1,   free_index,     list_leafs_indexes, text)

        return [ free_index, text ]


def main( single_node = True ):
    if sys.version_info[0] < 3: # Python 2 needs utf-8
        reload(sys)
        sys.setdefaultencoding('utf-8')

    file_control_graph_input, file_configuration_input, plps_path, depth_number = get_args()

    plps_to_delete = ["achieve_false_0*.xml", "achieve_true_0*.xml", "maintain_0*.xml", "observe_0*.xml"]
    for plp_to_delete in plps_to_delete:
        for file_path in Path(plps_path).glob(plp_to_delete):
            print("remove: " + str(file_path))
            os.remove(file_path)

    plp_name_achieve_false  = "achieve_false_xml"
    plp_name_achieve_true   = "achieve_true_xml"
    plp_name_maintain       = "maintain_xml"
    plp_name_observe        = "observe_xml"
    plp_file_achieve_false  = open_plp(plps_path, plp_name_achieve_false)
    plp_file_achieve_true   = open_plp(plps_path, plp_name_achieve_true)
    plp_file_maintain       = open_plp(plps_path, plp_name_maintain)
    plp_file_observe        = open_plp(plps_path, plp_name_observe)
    plp_text_achieve_false  = plp_file_achieve_false.read()
    plp_text_achieve_true   = plp_file_achieve_true.read()
    plp_text_maintain       = plp_file_maintain.read()
    plp_text_observe        = plp_file_observe.read()

    control_graph_input_text = file_control_graph_input.read()

    sub_flags = re.MULTILINE | re.DOTALL

    control_graph_input_text = re.sub(r"<node_sequential.*node_sequential>[ \n\r]*",   "", control_graph_input_text, flags=sub_flags)
    control_graph_input_text = re.sub(r"<node_probability.*node_probability>[ \n\r]*", "", control_graph_input_text, flags=sub_flags)
    control_graph_input_text = re.sub(r"<node_concurrent.*node_concurrent>[ \n\r]*",   "", control_graph_input_text, flags=sub_flags)
    control_graph_input_text = re.sub(r"<node_condition.*node_condition>[ \n\r]*",     "", control_graph_input_text, flags=sub_flags)

    control_graph_input_text = re.sub(r'<root root_name="[^>]*"/>', '<root root_name="node_probability_00000000"/>', control_graph_input_text )

    text_for_configuration_variables = ""
    text_for_configuration_parameters = ""

    control_graph_text = ""
    nodes_counter = 0
    list_leafs_indexes = []
    nodes_counter, control_graph_text = create_probability_tree( depth_number, nodes_counter, nodes_counter + 1, list_leafs_indexes, control_graph_text)


    for root_index in list_leafs_indexes:
        node_maintain_index                 = nodes_counter
        plp_maintain_index                  = nodes_counter + 1
        node_sequential_observe             = nodes_counter + 2
        plp_observe_index                   = nodes_counter + 3
        node_condition_index                = nodes_counter + 4
        node_sequential_achieve_true_index  = nodes_counter + 5
        node_sequential_achieve_false_index = nodes_counter + 6
        plp_achieve_true_index              = nodes_counter + 7
        plp_achieve_false_index             = nodes_counter + 8

        control_graph_text += ('<node_concurrent   node_name="node_concurrent_{node_concurrent_index:08d}" start_policy="any_predecessor_done">\n'
        '\t<run_node node_name="node_sequential_maintain_{node_maintain_index:08d}"/>\n'
        '\t<run_node node_name="node_sequential_observe_{node_sequential_observe:08d}"/>\n'
        '</node_concurrent>\n'
        '<node_sequential   node_name="node_sequential_maintain_{node_maintain_index:08d}" start_policy="any_predecessor_done" next_node_name="">\n'
        '\t<run_plp plp_name="maintain_{plp_maintain_index:08d}" wait_time="0"></run_plp>\n'
        '</node_sequential>\n'
        '<node_sequential   node_name="node_sequential_observe_{node_sequential_observe:08d}" start_policy="any_predecessor_done" next_node_name="node_condition_{node_condition_index:08d}">\n'
        '\t<run_plp plp_name="observe_{plp_observe_index:08d}" wait_time="0"></run_plp>\n'
        '</node_sequential>\n'
        '<node_condition   node_name="node_condition_{node_condition_index:08d}" start_policy="any_predecessor_done">\n'
        '\t<run_node node_name="node_sequential_achieve_true_{node_sequential_achieve_true_index:08d}">\n'
        '\t\t<preconditions>\n'
        '\t\t\t<formula_condition key_description="true == observe_goal_{plp_observe_index:08d}">\n'
        '\t\t\t\t<expression value="_observe_{plp_observe_index:08d}_observe_goal_{plp_observe_index:08d}" />\n'
        '\t\t\t\t<operator type="="/>\n'
        '\t\t\t\t<expression value="TRUE" />\n'
        '\t\t\t</formula_condition>\n'
        '\t\t</preconditions>\n'
        '\t</run_node>\n'
        '\t<run_node node_name="node_sequential_achieve_false_{node_sequential_achieve_false_index:08d}">\n'
        '\t\t<preconditions>\n'
        '\t\t\t<formula_condition key_description="false == observe_goal_{plp_observe_index:08d}">\n'
        '\t\t\t\t<expression value="_observe_{plp_observe_index:08d}_observe_goal_{plp_observe_index:08d}" />\n'
        '\t\t\t\t<operator type="="/>\n'
        '\t\t\t\t<expression value="FALSE" />\n'
        '\t\t\t</formula_condition>\n'
        '\t\t</preconditions>\n'
        '\t</run_node>\n'
        '</node_condition>\n'
        '<node_sequential   node_name="node_sequential_achieve_true_{node_sequential_achieve_true_index:08d}" start_policy="any_predecessor_done" next_node_name="">\n'
        '\t<run_plp plp_name="achieve_true_{plp_achieve_true_index:08d}" wait_time="0"></run_plp>\n'
        '</node_sequential>\n'
        '<node_sequential   node_name="node_sequential_achieve_false_{node_sequential_achieve_false_index:08d}" start_policy="any_predecessor_done" next_node_name="">\n'
        '\t<run_plp plp_name="achieve_false_{plp_achieve_false_index:08d}" wait_time="0"></run_plp>\n'
        '</node_sequential>').format(node_concurrent_index=root_index,
                                     node_maintain_index = node_maintain_index,
                                     plp_maintain_index = plp_maintain_index,
                                     node_sequential_observe = node_sequential_observe,
                                     plp_observe_index = plp_observe_index,
                                     node_condition_index = node_condition_index,
                                     node_sequential_achieve_true_index = node_sequential_achieve_true_index,
                                     node_sequential_achieve_false_index = node_sequential_achieve_false_index,
                                     plp_achieve_true_index = plp_achieve_true_index,
                                     plp_achieve_false_index = plp_achieve_false_index)

        path_plp_observe = os.path.join( plps_path, "observe_{plp_observe_index:08d}.xml".format(plp_observe_index = plp_observe_index) )
        file_plp_observe = open(path_plp_observe, 'wt')
        current_plp_text_observe = plp_text_observe
        current_plp_text_observe = re.sub(r'"observe_"',      '"observe_{plp_observe_index:08d}"'.format(plp_observe_index = plp_observe_index), current_plp_text_observe)
        current_plp_text_observe = re.sub(r'"observe_goal_"', '"observe_goal_{plp_observe_index:08d}"'.format(plp_observe_index = plp_observe_index), current_plp_text_observe)

        file_plp_observe.write( current_plp_text_observe )
        file_plp_observe.close()

        text_for_configuration_parameters += '<parameter plp_name="observe_{plp_observe_index:08d}" name="observe_goal_{plp_observe_index:08d}" min_value="FALSE"   max_value="TRUE"  />\n'.format(plp_observe_index = plp_observe_index)


        path_plp_maintain = os.path.join(plps_path, "maintain_{plp_maintain_index:08d}.xml".format(plp_maintain_index=plp_maintain_index))
        file_plp_maintain = open(path_plp_maintain, 'wt')
        current_plp_text_maintain = plp_text_maintain
        current_plp_text_maintain = re.sub(r'"maintain_"',                      '"maintain_{plp_maintain_index:08d}"'.format(plp_maintain_index=plp_maintain_index), current_plp_text_maintain)
        current_plp_text_maintain = re.sub(r'"maintaining_"',                   '"maintaining_{plp_maintain_index:08d}"'.format(plp_maintain_index=plp_maintain_index), current_plp_text_maintain)
        current_plp_text_maintain = re.sub(r'"maintain_termination_success_"',  '"maintain_termination_success_{plp_maintain_index:08d}"'.format(plp_maintain_index=plp_maintain_index), current_plp_text_maintain)
        current_plp_text_maintain = re.sub(r'"maintain_termination_failure_"',  '"maintain_termination_failure_{plp_maintain_index:08d}"'.format(plp_maintain_index=plp_maintain_index), current_plp_text_maintain)

        text_for_configuration_variables += ('<variable name="maintain_termination_success_{plp_maintain_index:08d}"    min_value="FALSE"   max_value="TRUE"  value="FALSE"   />\n'
                                   '<variable name="maintain_termination_failure_{plp_maintain_index:08d}"    min_value="FALSE"   max_value="TRUE"  value="FALSE"   />\n'
                                   '<variable name="maintaining_{plp_maintain_index:08d}"                     min_value="FALSE"   max_value="TRUE"  value="FALSE"   />\n').format(plp_maintain_index=plp_maintain_index)

        file_plp_maintain.write(current_plp_text_maintain)
        file_plp_maintain.close()

        path_plp_achieve_true = os.path.join(plps_path, "achieve_true_{plp_achieve_true_index:08d}.xml".format(
            plp_achieve_true_index=plp_achieve_true_index))
        file_plp_achieve_true = open(path_plp_achieve_true, 'wt')
        current_plp_text_achieve_true = plp_text_achieve_true
        current_plp_text_achieve_true = re.sub(r'"achieve_true_"',
                                               '"achieve_true_{plp_achieve_true_index:08d}"'.format(
                                                    plp_achieve_true_index=plp_achieve_true_index),
                                                    current_plp_text_achieve_true)

        current_plp_text_achieve_true = re.sub(r'"maintain_"',
                                               '"maintain_{plp_maintain_index:08d}"'.format(
                                                    plp_maintain_index=plp_maintain_index),
                                                    current_plp_text_achieve_true)


        file_plp_achieve_true.write(current_plp_text_achieve_true)
        file_plp_achieve_true.close()


        path_plp_achieve_false = os.path.join(plps_path, "achieve_false_{plp_achieve_false_index:08d}.xml".format(
            plp_achieve_false_index=plp_achieve_false_index))
        file_plp_achieve_false = open(path_plp_achieve_false, 'wt')
        current_plp_text_achieve_false = plp_text_achieve_false
        current_plp_text_achieve_false = re.sub(r'"achieve_false_"',
                                               '"achieve_false_{plp_achieve_false_index:08d}"'.format(
                                                   plp_achieve_false_index=plp_achieve_false_index),
                                               current_plp_text_achieve_false)

        current_plp_text_achieve_false = re.sub(r'"maintain_"',
                                               '"maintain_{plp_maintain_index:08d}"'.format(
                                                   plp_maintain_index=plp_maintain_index),
                                               current_plp_text_achieve_false)

        file_plp_achieve_false.write(current_plp_text_achieve_false)
        file_plp_achieve_false.close()

        nodes_counter += 9

    configuration_input_text = file_configuration_input.read()
    configuration_input_text = re.sub(r"<variable[^\n\r>]*>[ \n\r]*",  "", configuration_input_text, flags=sub_flags)
    configuration_input_text = re.sub(r"<parameter[^\n\r>]*>[ \n\r]*", "", configuration_input_text, flags=sub_flags)


    text_for_configuration_variables += ('<variable name="direction"        value="0"   />\n'
        '<variable name="collision_alert" min_value="FALSE"   max_value="TRUE"  value="FALSE"   />\n' )
    #    '<variable name="maintain_termination_success_"    min_value="FALSE"   max_value="TRUE"  value="FALSE"   />\n'
    #    '<variable name="maintain_termination_failure_"    min_value="FALSE"   max_value="TRUE"  value="FALSE"   />\n'
    #    '<variable name="maintaining_"                     min_value="FALSE"   max_value="TRUE"  value="FALSE"   />\n')

    #text_for_configuration_parameters += ('<parameter plp_name="observe_" name="observe_goal_" min_value="FALSE"   max_value="TRUE"  />\n')

    configuration_input_text = configuration_input_text.replace( "</configurations>", text_for_configuration_variables + text_for_configuration_parameters + "</configurations>" )

    file_configuration_input.truncate(0) # .*</node_sequential>
    file_configuration_input.seek(0)
    file_configuration_input.write(configuration_input_text)


    control_graph_input_text = control_graph_input_text.replace( "</control_graph>", control_graph_text + "</control_graph>" )

    file_control_graph_input.truncate(0) # .*</node_sequential>
    file_control_graph_input.seek(0)
    file_control_graph_input.write(control_graph_input_text)


    plp_file_achieve_false.close()
    plp_file_achieve_true.close()
    plp_file_maintain.close()
    plp_file_observe.close()
    file_configuration_input.close()
    file_control_graph_input.close()
    print("Done")
    
    
main(True)

# Run example:
# ./create_tree.py control_graph.xml configurations.xml plps/ 10
