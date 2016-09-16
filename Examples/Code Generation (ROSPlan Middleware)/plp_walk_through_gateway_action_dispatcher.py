#!/usr/bin/env python
import rospy
import sys
from rosplan_dispatch_msgs.msg import ActionDispatch
from rosplan_dispatch_msgs.msg import ActionFeedback
from rosplan_knowledge_msgs.msg import KnowledgeItem
from diagnostic_msgs.msg import KeyValue

from rosplan_knowledge_msgs.srv import KnowledgeUpdateServiceRequest
from rosplan_knowledge_msgs.srv import KnowledgeUpdateServiceResponse
from rosplan_knowledge_msgs.srv import KnowledgeUpdateService

from rosplan_knowledge_msgs.srv import GetInstanceServiceRequest
from rosplan_knowledge_msgs.srv import GetInstanceServiceResponse
from rosplan_knowledge_msgs.srv import GetInstanceService

import mongodb_store.message_store
from std_msgs.msg import String

from nav_msgs.msg import Odometry
from service_robot_msgs.msg import WalkGWcmd
from sensor_msgs.msg import LaserScan
from dynamixel_msgs.msg import MotorStateList

class PLP_walk_through_gateway_parameters(object):
    def __init__(self):
        self.callback = None
        # Execution Parameters
        self.areaA = None
        self.areaB = None
        self.gateway = None
        # Input Parameters
        self.gateway_location_gateway = None
        self.laser_scan = None
        self.odometry = None
        self.arm_controller = None
        # Output Parameters
        self.result = None

class PLP_walk_through_gateway_variables(object):
    def __init__(self):
        self.current_Aspeed = None
        self.current_Lspeed = None
        self.collision_alert = None
        self.arm_moving = None



class walk_through_gateway_dispatcher(object):

    def __init__(self):
        self.update_knowledge_client = rospy.ServiceProxy("/kcl_rosplan/update_knowledge_base", KnowledgeUpdateService)
        self.message_store = mongodb_store.message_store.MessageStoreProxy()
        self.action_feedback_pub = rospy.Publisher("/kcl_rosplan/action_feedback", ActionFeedback, queue_size=10)

        self.action_publisher_0 = rospy.Publisher("/walk_through_gateway_cmd", WalkGWcmd, queue_size=10)

        self.plp_params = PLP_walk_through_gateway_parameters()
        self.plp_vars = PLP_walk_through_gateway_variables()
        self.current_action = None

        rospy.Subscriber("/kcl_rosplan/action_dispatch", ActionDispatch, self.dispatch_action)

        # TODO: uncomment the following lines to receive values for input parameters if needed
        # For example, for more trigger conditions or variable calculation

#         rospy.Subscriber("/komodo_1/scan", LaserScan, self.param_laser_scan_updated)
#         rospy.Subscriber("/komodo_1/odom_pub", Odometry, self.param_odometry_updated)
#         rospy.Subscriber("/komodo_1/motor_states/arm_port", MotorStateList, self.param_arm_controller_updated)
        rospy.Subscriber("/walk_through_gateway_res", String, self.param_result_updated)

    # TODO: Implement update function for parameter: gateway_location(gateway). No glue mapping found.

    def param_laser_scan_updated(self, msg):
        self.plp_params.laser_scan = msg
        self.parameters_updated()

    def param_odometry_updated(self, msg):
        self.plp_params.odometry = msg
        self.parameters_updated()

    def param_arm_controller_updated(self, msg):
        self.plp_params.arm_controller = msg
        self.parameters_updated()

    def param_result_updated(self, msg):
        self.plp_params.result = msg
        # Update the message store with the output value for possible module triggers
        self.message_store.update_named("output_result",self.plp_params.result)

        self.parameters_updated()


    def parameters_updated(self):
        self.calculate_variables()
        if self.current_action == None:
            return
        if self.detect_success():
            rospy.loginfo("walk_through_gateway_action_dispatcher: detected success")
            self.update_success()
            self.reset_dispatcher()
        elif self.detect_failures():
            rospy.loginfo("walk_through_gateway_action_dispatcher: detected failure")
            self.update_fail()
            self.reset_dispatcher()

    def calculate_variables(self):
        self.plp_vars.current_Aspeed = self.calc_current_Aspeed()
        self.plp_vars.current_Lspeed = self.calc_current_Lspeed()
        self.plp_vars.collision_alert = self.calc_collision_alert()
        self.plp_vars.arm_moving = self.calc_arm_moving()

    # The following methods are used to update the variables
    # Access parameters using: self.parameters of type PLP_walk_through_gateway_parameters
    # Access constants using: self.constants[constant_name]

    def calc_current_Aspeed(self):
        # TODO Implement code to calculate current_Aspeed
        # return the value of the variable 
        return None

    def calc_current_Lspeed(self):
        # TODO Implement code to calculate current_Lspeed
        # return the value of the variable 
        return None

    def calc_collision_alert(self):
        # TODO Implement code to calculate collision_alert
        # return the value of the variable 
        return None

    def calc_arm_moving(self):
        # TODO Implement code to calculate arm_moving
        # return the value of the variable 
        return None



    def check_condition_no_linear_speed(self):
        # TODO implement code that checks the following formula
        # Formula: [begin_Lspeed = 0]
        return False

    def check_condition_no_angular_speed(self):
        # TODO implement code that checks the following formula
        # Formula: [begin_Aspeed = 0]
        return False

    def check_condition_result_is_success(self):
        # TODO implement code that checks the following formula
        # Formula: [result = success]
        return False

    def check_condition_result_is_false(self):
        # TODO implement code that checks the following formula
        # Formula: [result = failed]
        return False

    def check_condition_at_areaB(self):
        # TODO implement code that checks the following predicate condition
        # Predicate: (at areaB)
        return False

    def check_condition_collision(self):
        # TODO implement code that checks the following formula
        # Formula: [collision_alert = TRUE]
        return False

    def detect_success(self):
        if (self.check_condition_at_areaB()):
            return True
        elif (self.check_condition_result_is_success()):
            return True
        else:
            return None

    def detect_failures(self):
        if (self.check_condition_collision()):
            return True
        if ((not (self.check_condition_at_areaB())) and (self.check_condition_no_angular_speed()) and (self.check_condition_no_linear_speed())):
            return True
        if (self.check_condition_result_is_false()):
            return True
        return None

    def dispatch_action(self, action):
        if not action.name == "walk_through_gateway":
            return
        self.current_action = action

        for pair in action.parameters:
            if pair.key == "areaa":
                # Query the DB to get the real value of the PDDL parameter value received
                query_result = self.message_store.query_named(pair.value, String._type, False)
                # If there isn't a special value sent, use the PDDL parameter value received
                if not query_result:
                    self.plp_params.areaA = pair.value
                else:
                    self.plp_params.areaA = query_result[0][0]

            if pair.key == "areab":
                # Query the DB to get the real value of the PDDL parameter value received
                query_result = self.message_store.query_named(pair.value, String._type, False)
                # If there isn't a special value sent, use the PDDL parameter value received
                if not query_result:
                    self.plp_params.areaB = pair.value
                else:
                    self.plp_params.areaB = query_result[0][0]

            if pair.key == "gateway":
                # Query the DB to get the real value of the PDDL parameter value received
                query_result = self.message_store.query_named(pair.value, String._type, False)
                # If there isn't a special value sent, use the PDDL parameter value received
                if not query_result:
                    self.plp_params.gateway = pair.value
                else:
                    self.plp_params.gateway = query_result[0][0]

        # If some of the execution parameters weren't assigned:
        # Check if they were saved as output params from other modules
        if not self.plp_params.areaA:
            self.plp_params.areaA = self.message_store.query_named("output_areaA", String._type, False)
        if not self.plp_params.areaB:
            self.plp_params.areaB = self.message_store.query_named("output_areaB", String._type, False)
        if not self.plp_params.gateway:
            self.plp_params.gateway = self.message_store.query_named("output_gateway", String._type, False)

        # Check if the action can be dispatched (every execution parameter has a value)
        if self.check_can_dispatch():
            in_walkgwcmd = WalkGWcmd()
            in_walkgwcmd.areaA = self.plp_params.areaA
            in_walkgwcmd.areaB = self.plp_params.areaB
            in_walkgwcmd.gateway = self.plp_params.gateway
            self.action_publisher_0.publish(in_walkgwcmd)
        else:
            rospy.loginfo("Failed at running action: %s. Conditions not met for dispatch", action.name)

    def check_can_dispatch(self):
        canDispatch = True
        if (self.plp_params.areaA is None or self.plp_params.areaB is None or self.plp_params.gateway is None):
            canDispatch = False

        # TODO: Optionally, add more trigger requirements using self.plp_params.<parameter_name> and/or self.plp_vars.<variable_name>

        return canDispatch

    def update_success(self):
        # Update the effects in the KMS
        parametersDic = self.toDictionary(self.current_action.parameters)
        self.changeKMSFact("at", [["par", parametersDic["areab"]]], KnowledgeUpdateServiceRequest.ADD_KNOWLEDGE)
        instance_query_client = rospy.ServiceProxy("/kcl_rosplan/get_current_instances", GetInstanceService)
        forAllInstances = instance_query_client.call("object").instances
        for forAllInstance in forAllInstances:
            self.changeKMSFact("k_gateway_location", [["gw", parametersDic["gw"] if "gw" in parametersDic else forAllInstance]], KnowledgeUpdateServiceRequest.REMOVE_KNOWLEDGE)
        # Update the Planning System on failure
        actionFeedback = ActionFeedback()
        actionFeedback.action_id = self.current_action.action_id
        actionFeedback.status = "action achieved"
        self.action_feedback_pub.publish(actionFeedback)

    def update_fail(self):
        actionFeedback = ActionFeedback()
        actionFeedback.action_id = self.current_action.action_id
        actionFeedback.status = "action failed"
        self.action_feedback_pub.publish(actionFeedback)

    def changeKMSFact(self, name, params, changeType):
        knowledge = KnowledgeItem()
        knowledge.knowledge_type = KnowledgeItem.FACT
        knowledge.attribute_name = name
        for param in params:
            pair = KeyValue()
            pair.key = param[0]
            pair.value = param[1]
            knowledge.values.append(pair)
        update_response = self.update_knowledge_client(changeType, knowledge)
        if (update_response.success is not True):
            rospy.loginfo("Failed updating KMS with attribute: %s", knowledge.attribute_name)
        else:
            rospy.loginfo("Updated KMS with attribute: %s", knowledge.attribute_name)
    
    def toDictionary(self, pairs):
        result = []
        for pair in pairs:
            result.append((pair.key,pair.value))
        return dict(result)

    def reset_dispatcher(self):
        self.plp_params = PLP_walk_through_gateway_parameters()
        self.plp_vars = PLP_walk_through_gateway_variables()
        self.current_action = None


if __name__ == '__main__':
    try:
        rospy.init_node("plp_walk_through_gateway_action_dispatcher", anonymous=False)
        rospy.loginfo("Starting walk_through_gateway action dispatcher")
        walk_through_gateway_dispatcher()
        rospy.spin()

    except rospy.ROSInterruptException:
        pass
