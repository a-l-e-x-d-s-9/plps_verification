#!/usr/bin/env bash
set -e

xmllint --xinclude --schema ../../Verification-schemas/configurations.xsd    configurations.xml
xmllint --xinclude --schema ../../Verification-schemas/control_graph.xsd     control_graph.xml

xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd     plps/achieve_door_open.xml
xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd     plps/achieve_door_unlock.xml
xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd     plps/achieve_key_take.xml
xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd     plps/achieve_move_through_doorway.xml
xmllint --xinclude --schema ../../PLP-schemas/AchievePLP_schema.xsd     plps/achieve_move_to.xml
xmllint --xinclude --schema ../../PLP-schemas/ObservePLP_schema.xsd     plps/observe_is_door_locked.xml
xmllint --xinclude --schema ../../PLP-schemas/ObservePLP_schema.xsd     plps/observe_is_door_open.xml
xmllint --xinclude --schema ../../PLP-schemas/MaintainPLP_schema.xsd    plps/maintain_key_hold.xml


# java -jar ../../CodeGenerator/out/artifacts/CodeGenerator_jar/CodeGenerator.jar -verify plps control_graph.xml generated_system.xml configurations.xml


#~/Thesis/plps_verification/Examples/example_00
#~/Thesis/plps_verification/Examples/example_02
