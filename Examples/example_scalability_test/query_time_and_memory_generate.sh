#!/usr/bin/env bash


declare -a PLPS_SIZES=(50 100 200 400 700) 
# 50 100 200 400 700 1400 2800 5600 11200 22500 45000 90000
PLPS_SIZES_AMOUNT=${#PLPS_SIZES[@]}
ITERATIONS=10


#PLPS=100000
#./multiply_ptas.py control_graph.xml plps/move_forward_xml $PLPS
#java -Xms560m -Xmx12048m -jar ../../CodeGenerator/out/artifacts/CodeGenerator_jar/CodeGenerator.jar -verify plps control_graph.xml generated_system_plps_${PLPS}.xml configurations.xml

for (( PLP_INDEX=0; PLP_INDEX < ${PLPS_SIZES_AMOUNT}; PLP_INDEX++))
do
    PLPS=${PLPS_SIZES[PLP_INDEX]}
    
    echo "PLPS: ${PLPS}"
    
    for (( I=1; I <=${ITERATIONS}; I++))
    do
        
        echo "I: ${I}"
        echo "exist"
        /usr/bin/time -v ../../../uppaal64-4.1.19/bin-Linux/verifyta -s -o 2 -u generated_system_plps_${PLPS}.xml query_exist       &> time_query_exist_plps_${PLPS}_iteration_${I}
        echo "probability"
        /usr/bin/time -v ../../../uppaal64-4.1.19/bin-Linux/verifyta -s -o 2 -u generated_system_plps_${PLPS}.xml query_probability &> time_query_probability_plps_${PLPS}_iteration_${I}
    done
done
