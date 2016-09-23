# Welcome to the PLP-repo

## Video Example
The following is a link to a video demonstrating the steps in our methodology for Plug&Play planning. In addition, the video presents an execution example of a service robot project, using our methodology:
https://drive.google.com/file/d/0B8OEQJ-dGujlR3E5eFRBRDVLUHc/view

## Synopsis
We aim to show that given PLPs (written in XML) for some robotic modules, we can provide automatic monitoring and plug&play planning.
This project has three parts:
1. PLP->PDDL compiler. Two modes: Near-fully-observable mode and Partially-observable mode.
2. ROSPlan middleware code generator. Generates a ROS package that allows planning and executing plans with ROSPlan without having to write a lot of code to integrate robotic modules with ROSPlan.
3. Monitoring code generator. Generates a ROS package that allows to automatically monitor robotic module's execution, alert on abnormal behavior and provide success/fail estimations.

## Installation

There are three Java projects: 1. CodeGenerator - automatically generated monitoring code & ROSPlan middleware. 2. PDDLCompiler - compiles PLPs into PDDL in order to plan with them. 3. PLPLib - a collection of libraries for representing PLPs and their fields, and a Loader which loads the XML files.

All of the tools are precompiled and can be used by executing the appropriate jar files (contained in "out/artifacts").

## Running Instructions

### PLP->PDDL compiler 

#### For the Near-fully-observable mode:
> java -jar PDDLCompiler.jar -NFO [plp-folder-path]

#### For the Partially-observable mode:
> java -jar PDDLCompiler.jar -PO [plp-folder-path]

After running the previous line, fill in the problem.pddl file with the goal and the known facts about the initial state, and run the following:

> java -jar PDDLCompiler.jar -POprob [plp-folder-path]

### ROSPlan middleware generator

#### If the PDDL domain was generated in the Near-fully-observable mode:
> java -jar CodeGenerator.jar -dispatcher -nfo [plp-folder-path] [pddl-domain-path]

#### If the PDDL domain was generated in the Partially-observable mode:
> java -jar CodeGenerator.jar -dispatcher -po [plp-folder-path] [pddl-domain-path]

### Monitoring code generation

> java -jar CodeGenerator.jar -monitor [plp-folder-path]

## Code Examples

Under "Examples" there are examples of PLPs, glue files and generated ROSPlan middleware.

## Motivation

We introduce Performance Level Profiles to address current issues in robotics such as: Automatic code monitoring and reuse, identifying abnormal behavior of robotic modules and  automatically planning and executing a plan, that acheives a required goal, which is built using basic underlying robotic modules (Plug&Play planning).

