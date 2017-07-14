#!/usr/bin/env bash
set -e

xmllint --xinclude --schema ../../Verification-schemas/configurations.xsd   configurations.xml
xmllint --xinclude --schema ../../Verification-schemas/control_graph.xsd    control_graph.xml

xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd         plps/achieve_walk_given_distance.xml
xmllint --xinclude --schema ../../PLP-schemas/DetectPLP_schema.xsd          plps/detect_wall.xml
xmllint --xinclude --schema ../../PLP-schemas/MaintainPLP_schema.xsd        plps/maintain_direction_on_map.xml
xmllint --xinclude --schema ../../PLP-schemas/MaintainPLP_schema.xsd        plps/maintain_direction_on_map_2.xml
xmllint --xinclude --schema ../../PLP-schemas/ObservePLP_schema.xsd         plps/observe_gateway.xml


java -jar ../../CodeGenerator/out/artifacts/CodeGenerator_jar/CodeGenerator.jar -verify plps control_graph.xml generated_system.xml configurations.xml

echo "DONE"
