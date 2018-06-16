#!/usr/bin/env bash
set -e

TREE_DEPTH=1
if [ $# -eq 1 ]; then
    TREE_DEPTH=$1
fi
#echo $TREE_DEPTH

./create_tree.py control_graph.xml configurations.xml plps/ $TREE_DEPTH
./verify_example.sh
#../../../uppaal64-4.1.19/uppaal --no-splash --no-antialias generated_system.xml
