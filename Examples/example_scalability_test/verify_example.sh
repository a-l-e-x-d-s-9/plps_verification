#!/usr/bin/env bash
set -e

xmllint --xinclude --schema ../../Verification-schemas/configurations.xsd   configurations.xml
xmllint --xinclude --schema ../../Verification-schemas/control_graph.xsd    control_graph.xml

xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd         plps/move_forward.xml


java -jar ../../CodeGenerator/out/artifacts/CodeGenerator_jar/CodeGenerator.jar -verify plps control_graph.xml generated_system.xml configurations.xml

# Pr[<=10000] ( <> node_sequential_1_process.done  )
# E<> node_sequential_1_process.done

echo "DONE"
