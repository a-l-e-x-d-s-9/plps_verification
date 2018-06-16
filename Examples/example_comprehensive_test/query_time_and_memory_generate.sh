#!/usr/bin/env bash


MIN_DEPTH=2
MAX_DEPTH=6
ITERATIONS=10


for (( DEPTH=${MIN_DEPTH}; DEPTH <=${MAX_DEPTH}; DEPTH++))
do
    VAR_INDEX=`grep ": direction" generated_system_depth_${DEPTH}.xml | sed -r 's/\/\/ 0*([1-9][0-9]*): direction/\1/'`
    # // 044: direction
    echo "E<> concurrent_info.concurrent_data[ ${VAR_INDEX} ].value == 10" > query_exist_${DEPTH}
    echo "Pr[<=1000000000] ( <> concurrent_info.concurrent_data[ ${VAR_INDEX} ].value == 10 )" > query_probability_${DEPTH}

    echo "DEPTH: ${DEPTH}"

	for (( I=1; I <=${ITERATIONS}; I++))
	do
        echo "I: ${I}"
        echo "exist"
    	/usr/bin/time -v ../../../uppaal64-4.1.19/bin-Linux/verifyta -s -o 2 -u generated_system_depth_${DEPTH}.xml query_exist_${DEPTH}       &> time_query_exist_depth_${DEPTH}_iteration_${I}
	    echo "probability"
        /usr/bin/time -v ../../../uppaal64-4.1.19/bin-Linux/verifyta -s -o 2 -u generated_system_depth_${DEPTH}.xml query_probability_${DEPTH} &> time_query_probability_depth_${DEPTH}_iteration_${I}
    done
done
	
	
