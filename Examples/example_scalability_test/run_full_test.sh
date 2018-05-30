#!/usr/bin/env bash

./multiply_ptas.py control_graph.xml plps/move_forward.xml 1
./verify_example.sh
uppaal --no-splash --no-antialias control_graph.xml
