#!/usr/bin/env python
import rospy
import sys
from rosplan_knowledge_msgs.msg import KnowledgeItem
from diagnostic_msgs.msg import KeyValue

from rosplan_knowledge_msgs.srv import KnowledgeUpdateServiceRequest
from rosplan_knowledge_msgs.srv import KnowledgeUpdateServiceResponse
from rosplan_knowledge_msgs.srv import KnowledgeUpdateService

import mongodb_store.message_store

from std_msgs.msg import String
from rosgraph_msgs.msg import Log
from std_srvs.srv import Empty
from std_srvs.srv import EmptyRequest
from std_srvs.srv import EmptyResponse

import os.path
import itertools
import copy

from plp_middleware.srv import ChangeOnFailRequest
from plp_middleware.srv import ChangeOnFailResponse
from plp_middleware.srv import ChangeOnFail

from plp_middleware.srv import ChangeOnContradictionRequest
from plp_middleware.srv import ChangeOnContradictionResponse
from plp_middleware.srv import ChangeOnContradiction

from rosplan_knowledge_msgs.srv import GetDomainOperatorDetailsServiceRequest
from rosplan_knowledge_msgs.srv import GetDomainOperatorDetailsServiceResponse
from rosplan_knowledge_msgs.srv import GetDomainOperatorDetailsService

from rosplan_knowledge_msgs.srv import GetDomainPredicateDetailsServiceRequest
from rosplan_knowledge_msgs.srv import GetDomainPredicateDetailsServiceResponse
from rosplan_knowledge_msgs.srv import GetDomainPredicateDetailsService

from rosplan_knowledge_msgs.srv import GetAttributeServiceRequest
from rosplan_knowledge_msgs.srv import GetAttributeServiceResponse
from rosplan_knowledge_msgs.srv import GetAttributeService

from rosplan_knowledge_msgs.srv import GetInstanceServiceRequest
from rosplan_knowledge_msgs.srv import GetInstanceServiceResponse
from rosplan_knowledge_msgs.srv import GetInstanceService

class Manager(object):

    def __init__(self):
    	self.prompt_user = False
        self.finalize_sensed_nc_values = True

    	self.update_knowledge_client = rospy.ServiceProxy("/kcl_rosplan/update_knowledge_base", KnowledgeUpdateService)
        self.planner_command_publisher = rospy.Publisher("/kcl_rosplan/planning_commands", String, queue_size=5)
        rospy.Subscriber("/rosout_agg", Log, self.log_callback)
        self.handling_unsolvable = False

        self.operator_details_client = rospy.ServiceProxy("/kcl_rosplan/get_domain_operator_details", GetDomainOperatorDetailsService)
        self.predicate_details_client = rospy.ServiceProxy("/kcl_rosplan/get_domain_predicate_details", GetDomainPredicateDetailsService)
        self.current_knowledge_client = rospy.ServiceProxy("/kcl_rosplan/get_current_knowledge", GetAttributeService)
        self.get_instances_client = rospy.ServiceProxy("/kcl_rosplan/get_current_instances", GetInstanceService)
        self.get_goal_client = rospy.ServiceProxy("/kcl_rosplan/get_current_goals", GetAttributeService)
        self.clear_knowledge_client = rospy.ServiceProxy("/kcl_rosplan/clear_knowledge_base", Empty)

        self.save_initial_state()

        rospy.Service('/plp_middleware/change_assumptions_failed_action', ChangeOnFail, self.change_assumption_on_fail_callback)
        rospy.Service('/plp_middleware/change_assumptions_contradiction', ChangeOnContradiction, self.change_assumption_on_contradiction_callback)

        self.init_assumptions = []
        self.assumptions_history = []
        self.current_assumptions = []
        self.static_assumptions = []
        self.load_first_assumptions()

    def log_callback(self, message):
        if message.name == "/rosplan_planning_system":
            if "unsolvable" in message.msg and not self.handling_unsolvable:
                self.handling_unsolvable = True
                cancelMsg = String()
                cancelMsg.data = "cancel"
                self.planner_command_publisher.publish(cancelMsg)

                result = self.find_possible_values(range(0,len(self.init_assumptions)))
                if result:
	                planMsg = String()
	                planMsg.data = "plan"
	                self.handling_unsolvable = False
	                self.planner_command_publisher.publish(planMsg)
                else:
	            	rospy.logerr("Gone over all possible assumptions")

    def change_assumption_on_fail_callback(self, message):
    	#print message
        actionName = message.name
        parametersDic = self.toDictionary(message.parameters)
        actionDetails = self.operator_details_client.call(actionName).op
        if actionDetails:
        	gPreds = []
        	for condition in actionDetails.at_start_simple_condition + actionDetails.at_start_neg_condition:
        		if not "know_" in condition.name and not "kv_" in condition.name:
	        		gCondition = [condition.name]
	        		parList = []
	        		for typedParam in condition.typed_parameters:
	        			parList.append(parametersDic[typedParam.key])
	        		gCondition.append(parList)
	        		gPreds.append(gCondition)
	        		print gCondition
        	if self.change_grounded_preds_assumption(gPreds):
        		return True
        	else:
        		rospy.logerr("Gone over all possible assumptions")
        return False

    def change_assumption_on_contradiction_callback(self, message):
    	gPred = message.grounded_pred.translate(None,'\n()').split(' ')
    	gPredList = [gPred[0]] + [gPred[1:len(gPred)]]
    	gPredIndex = self.gpred_assumption_index(gPredList)
    	if gPredIndex is -1:
    		rospy.logerr("Grounded predicate: " + message.grounded_pred + " not in assumption list")
    		return False
    	if message.sensed:
    		if self.current_assumptions[gPredIndex] == 'F':
    			self.update_assumptions([gPredIndex],['T'],True)
    			if self.finalize_sensed_nc_values and self.init_assumptions[gPredIndex][3] == "WEAKNC":
    				self.static_assumptions.append(gPredIndex)
    			return True
    	else:
    		if self.current_assumptions[gPredIndex] == 'T':
    			self.update_assumptions([gPredIndex],['F'],True)
    			if self.finalize_sensed_nc_values and self.init_assumptions[gPredIndex][3] == "WEAKNC":
    				self.static_assumptions.append(gPredIndex)
    			return True
    	return False


    def toDictionary(self, pairs):
        result = []
        for pair in pairs:
            result.append((pair.key,pair.value))
        return dict(result)

    def load_first_assumptions(self):
    	fileName = os.path.join(os.path.dirname(__file__), 'assumptions.txt')
    	if os.path.isfile(fileName):
			asFile = open(fileName,'r')
			for line in asFile:
				line = line.translate(None,'\n()').split(' ')
				assumption = [line[0]] + [line[1:len(line)-2]] + line[len(line)-2:]
				self.init_assumptions.append(assumption)
				self.current_assumptions.append(assumption[2])
    	else:
    		rospy.logerr("Can't find assumptions.txt file")
    	self.add_curr_assumptions_to_history()

    def gpred_assumption_index(self, gPred):
    	index = 0
    	found = False
    	for assumption in self.init_assumptions:
    		#rospy.loginfo(repr(assumption)+repr(index))
    		if assumption[0] == gPred[0] and str(assumption[1]) == str(gPred[1]):
    			predIndex = index
    			found = True
    			break
    		index = index + 1
    	if not found:
    		return -1
    	return index

    def change_grounded_preds_assumption(self, gPreds):
    	indexList = []
    	for gPred in gPreds:
    		currIndex = self.gpred_assumption_index(gPred)
    		if currIndex is not -1:
    			indexList.append(currIndex)
    	#print indexList
    	result = self.find_possible_values(indexList)
    	if not result:
    		#rospy.loginfo("No assumptions to change for grounded predicates: "+repr(gPreds))
    		result = self.find_possible_values(range(0,len(self.init_assumptions)))
    	if not result:
    		return False
    	return True

    def find_possible_values(self, assumptionIndexList):
    	for staticIndex in self.static_assumptions:
    		assumptionIndexList.remove(staticIndex)

    	for i in range(1,len(assumptionIndexList)+1):
    		#print assumptionIndexList
    		combinations = list(itertools.combinations(assumptionIndexList, i))
    		#print repr(combinations)
    		#print repr(combinations)
    		allValuesForCombination = []
    		self.create_values_list(allValuesForCombination,'',i)
    		#print repr(allValuesForCombination)
    		for combination in combinations:
    			for values in allValuesForCombination:
    				if not self.in_history(combination,values):
    					self.update_assumptions(combination,values)
    					return True
    	return False
    
    def in_history(self, indexCombination, values):
    	temp_assumptions = copy.deepcopy(self.current_assumptions)
    	counter = 0
    	for index in indexCombination:
    		temp_assumptions[index] = values[counter]
    		counter = counter + 1
    	if temp_assumptions in self.assumptions_history:
    		return True
    	else:
    		return False

    def update_assumptions(self, indexCombination, values, fromSensed=False):
    	counter = 0
    	for index in indexCombination:
    		self.current_assumptions[index] = values[counter]
    		self.update_assumption(index,values[counter],fromSensed)
    		counter = counter + 1
    	self.add_curr_assumptions_to_history()
    	if self.prompt_user:
    		print "Would you like to load the initial state (with the new assumptions)? (y/n)"
    		response = raw_input()
    		if response == "y":
    			self.load_initial_state_new_assumptions()

    def update_assumption(self, index, value, fromSensed=False):
    	rospy.loginfo("Load assumption: " + self.init_assumptions[index][0] + repr(self.init_assumptions[index][1]) + " to " + value)
    	if fromSensed or self.init_assumptions[index][3] == 'STRONG':
    		if value == 'T':
    			self.changeKMSFact(self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.ADD_KNOWLEDGE)
    			self.changeKMSFact("know_"+self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.ADD_KNOWLEDGE)
    			self.changeKMSFact("know_not_"+self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE)
    		else:
    			self.changeKMSFact(self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE)
    			self.changeKMSFact("know_"+self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE)
    			self.changeKMSFact("know_not_"+self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.ADD_KNOWLEDGE)
    	elif 'WEAK' in self.init_assumptions[index][3]:
    		if value == 'T':
    			self.changeKMSFact(self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.ADD_KNOWLEDGE)
    			self.changeKMSFact("know_"+self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE)
    			self.changeKMSFact("know_not_"+self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE)
    		else:
    			self.changeKMSFact(self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE)
    			self.changeKMSFact("know_"+self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE)
    			self.changeKMSFact("know_not_"+self.init_assumptions[index][0], self.init_assumptions[index][1], KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE)
    		#if 'NC' in self.init_assumptions[index][3]:
    		#	self.static_assumptions.append([index,value])

    def add_curr_assumptions_to_history(self):
    	temp_assumptions = copy.deepcopy(self.current_assumptions)
    	self.assumptions_history.append(temp_assumptions)
    	#print self.assumptions_history
    
    def create_values_list(self, valuesList, currValues, numValues):
    	if (numValues == 0):
    		valuesList.append(currValues)
    	else:
    		self.create_values_list(valuesList, currValues + 'T', numValues-1)
    		self.create_values_list(valuesList, currValues + 'F', numValues-1)

    def changeKMSFact(self, name, paramsValues, changeType):
    	predicateDetails = self.predicate_details_client.call(name)
        knowledge = KnowledgeItem()
        knowledge.knowledge_type = KnowledgeItem.FACT
        knowledge.attribute_name = name
        for param, paramValue in zip(predicateDetails.predicate.typed_parameters, paramsValues):
            pair = KeyValue()
            pair.key = param.key
            pair.value = paramValue
            knowledge.values.append(pair)
        update_response = self.update_knowledge_client(changeType, knowledge)

    def save_initial_state(self):
    	self.initial_facts = self.current_knowledge_client.call('')
    	self.initial_goal = self.get_goal_client('')
    	self.initial_instances = self.get_instances_client('')


    def load_initial_state_new_assumptions(self):
    	self.clear_knowledge_client.call()
    	
    	for instance in self.initial_instances.instances:
    		knowledge = KnowledgeItem()
    		knowledge.knowledge_type = KnowledgeItem.INSTANCE
    		knowledge.instance_type = "object"
    		knowledge.instance_name = instance
    		self.update_knowledge_client(KnowledgeUpdateServiceRequest.ADD_KNOWLEDGE, knowledge)
    		
    	for goal in self.initial_goal.attributes:
    		self.update_knowledge_client(KnowledgeUpdateServiceRequest.ADD_GOAL, goal)

    	for fact in self.initial_facts.attributes:
    		self.update_knowledge_client(KnowledgeUpdateServiceRequest.ADD_KNOWLEDGE, fact)

    	rospy.loginfo("Initial state loaded successfuly. Now loading assumptions:")
    	for i in range(0,len(self.current_assumptions)):
    		self.update_assumption(i,self.current_assumptions[i])


if __name__ == '__main__':
    try:
        rospy.init_node("plp_middleware_assumption_manager", anonymous=False)
        rospy.loginfo("Starting PLP middleware assumption manager")
        Manager()
        rospy.spin()

    except rospy.ROSInterruptException:
        pass
