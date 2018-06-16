#!/usr/bin/env bash


MIN_DEPTH=2
MAX_DEPTH=13
ITERATIONS=10


for (( DEPTH=${MIN_DEPTH}; DEPTH <=${MAX_DEPTH}; DEPTH++))
do
	./create_tree.py control_graph.xml configurations.xml plps/ ${DEPTH}
	for (( I=1; I <=${ITERATIONS}; I++))
	do
		 /usr/bin/time -v java -Xms560m -Xmx12048m -jar ../../CodeGenerator/out/artifacts/CodeGenerator_jar/CodeGenerator.jar -verify plps control_graph.xml generated_system_depth_${DEPTH}.xml configurations.xml 2> time_compilation_depth_${DEPTH}_iteration_${I}
	done
done
	
	
