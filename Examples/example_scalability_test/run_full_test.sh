#!/usr/bin/env bash
set -e

PLPS=1
if [ $# -eq 1 ]; then
    PLPS=$1
fi
#echo $PLPS

./multiply_ptas.py control_graph.xml plps/move_forward_xml $PLPS
./verify_example.sh
uppaal --no-splash --no-antialias generated_system.xml
