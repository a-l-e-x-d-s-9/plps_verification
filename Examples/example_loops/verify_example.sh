#!/usr/bin/env bash
set -e

xmllint --xinclude --schema ../../Verification-schemas/configurations.xsd   concurrent_configurations.xml
xmllint --xinclude --schema ../../Verification-schemas/control_graph.xsd    concurrent_control_graph.xml
xmllint --xinclude --schema ../../Verification-schemas/configurations.xsd   condition_configurations.xml
xmllint --xinclude --schema ../../Verification-schemas/control_graph.xsd    condition_control_graph.xml
xmllint --xinclude --schema ../../Verification-schemas/configurations.xsd   probability_configurations.xml
xmllint --xinclude --schema ../../Verification-schemas/control_graph.xsd    probability_control_graph.xml
xmllint --xinclude --schema ../../Verification-schemas/configurations.xsd   sequential_configurations.xml
xmllint --xinclude --schema ../../Verification-schemas/control_graph.xsd    sequential_control_graph.xml

xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd         plps/achieve_door_open.xml
xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd         plps/achieve_door_unlock.xml
xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd         plps/achieve_key_take.xml
xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd         plps/achieve_move_to.xml
xmllint --xinclude --schema ../../PLP-schemas/ObservePLP_schema.xsd         plps/observe_is_door_locked.xml
xmllint --xinclude --schema ../../PLP-schemas/ObservePLP_schema.xsd         plps/observe_is_door_open.xml
xmllint --xinclude --schema ../../PLP-schemas/MaintainPLP_schema.xsd        plps/maintain_key_hold.xml

java -jar ../../CodeGenerator/out/artifacts/CodeGenerator_jar/CodeGenerator.jar -verify plps concurrent_control_graph.xml  concurrent_generated_system.xml  concurrent_configurations.xml
java -jar ../../CodeGenerator/out/artifacts/CodeGenerator_jar/CodeGenerator.jar -verify plps condition_control_graph.xml   condition_generated_system.xml   condition_configurations.xml
java -jar ../../CodeGenerator/out/artifacts/CodeGenerator_jar/CodeGenerator.jar -verify plps probability_control_graph.xml probability_generated_system.xml probability_configurations.xml
java -jar ../../CodeGenerator/out/artifacts/CodeGenerator_jar/CodeGenerator.jar -verify plps sequential_control_graph.xml  sequential_generated_system.xml  sequential_configurations.xml

echo "DONE"
