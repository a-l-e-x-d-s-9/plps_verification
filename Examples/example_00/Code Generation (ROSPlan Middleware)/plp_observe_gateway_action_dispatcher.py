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
from service_robot_msgs.msg import ObGWcmd
from sensor_msgs.msg import Image
from dynamixel_msgs.msg import MotorStateList
from geometry_msgs.msg import Pose

class PLP_observe_gateway_parameters(object):
    def __init__(self):
        self.callback = None
        # Execution Parameters
        self.areaA = None
        self.areaB = None
        self.gateway = None
        # Input Parameters
        self.rgb_image = None
        self.odometry = None
        self.arm_controller = None
        # Output Parameters
        self.gateway_location_gateway = None

class PLP_observe_gateway_variables(object):
    def __init__(self):
        self.collision_alert = None
        self.arm_moving = None
        self.begin_Aspeed = None
        self.begin_Lspeed = None



class observe_gateway_dispatcher(object):

    def __init__(self):
        self.update_knowledge_client = rospy.ServiceProxy("/kcl_rosplan/update_knowledge_base", KnowledgeUpdateService)
        self.message_store = mongodb_store.message_store.MessageStoreProxy()
        self.action_feedback_pub = rospy.Publisher("/kcl_rosplan/action_feedback", ActionFeedback, queue_size=10)

        self.action_publisher_0 = rospy.Publisher("/observe_gateway_cmd", ObGWcmd, queue_size=10)

        self.plp_params = PLP_observe_gateway_parameters()
        self.plp_vars = PLP_observe_gateway_variables()
        self.current_action = None

        rospy.Subscriber("/kcl_rosplan/action_dispatch", ActionDispatch, self.dispatch_action)

        # TODO: uncomment the following lines to receive values for input parameters if needed
        # For example, for more trigger conditions or variable calculation

#         rospy.Subscriber("/komodo_1/komodo_1_Asus_Camera/rgb/image_raw", Image, self.param_rgb_image_updated)
#         rospy.Subscriber("/komodo_1/odom_pub", Odometry, self.param_odometry_updated)
#         rospy.Subscriber("/komodo_1/motor_states/arm_port", MotorStateList, self.param_arm_controller_updated)
        rospy.Subscriber("/observe_gateway_res", Pose, self.param_gateway_location_gateway_updated)

    def param_rgb_image_updated(self, msg):
        self.plp_params.rgb_image = msg
        self.parameters_updated()

    def param_odometry_updated(self, msg):
        self.plp_params.odometry = msg
        self.parameters_updated()

    def param_arm_controller_updated(self, msg):
        self.plp_params.arm_controller = msg
        self.parameters_updated()

    def param_gateway_location_gateway_updated(self, msg):
        self.plp_params.gateway_location_gateway = msg
        # Update the message store with the output value for possible module triggers
        self.message_store.update_named("output_gateway_location_gateway",self.plp_params.gateway_location_gateway)

        self.parameters_updated()


    def parameters_updated(self):
        self.calculate_variables()
        if self.current_action == None:
            return
        if self.detect_success():
            rospy.loginfo("observe_gateway_action_dispatcher: detected success")
            self.update_success()
            self.reset_dispatcher()
        elif self.detect_failures():
            rospy.loginfo("observe_gateway_action_dispatcher: detected failure")
            self.update_fail()
            self.reset_dispatcher()

    def calculate_variables(self):
        self.plp_vars.collision_alert = self.calc_collision_alert()
        self.plp_vars.arm_moving = self.calc_arm_moving()
        self.plp_vars.begin_Aspeed = self.calc_begin_Aspeed()
        self.plp_vars.begin_Lspeed = self.calc_begin_Lspeed()

    # The following methods are used to update the variables
    # Access parameters using: self.parameters of type PLP_observe_gateway_parameters
    # Access constants using: self.constants[constant_name]

    def calc_collision_alert(self):
        # TODO Implement code to calculate collision_alert
        # return the value of the variable 
        return None

    def calc_arm_moving(self):
        # TODO Implement code to calculate arm_moving
        # return the value of the variable 
        return None

    def calc_begin_Aspeed(self):
        # TODO Implement code to calculate begin_Aspeed
        # return the value of the variable 
        return None

    def calc_begin_Lspeed(self):
        # TODO Implement code to calculate begin_Lspeed
        # return the value of the variable 
        return None



    def detect_success(self):
        if self.plp_params.gateway_location_gateway is not None:
            # TODO: Optionally, add more conditions on the returned value, to determine if the observation finished successfully
            return True
        return None

    def detect_failures(self):
        # TODO: Implement failure to observe detection. No failed termination conditions specified
        return None

    def dispatch_action(self, action):
        if not action.name == "observe_gateway":
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
            in_obgwcmd = ObGWcmd()
            in_obgwcmd.areaA = self.plp_params.areaA
            in_obgwcmd.areaB = self.plp_params.areaB
            in_obgwcmd.gateway = self.plp_params.gateway
            self.action_publisher_0.publish(in_obgwcmd)
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
        self.changeKMSFact("k_gateway_location", [["gw", parametersDic["gateway"]]], KnowledgeUpdateServiceRequest.ADD_KNOWLEDGE)
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
        self.plp_params = PLP_observe_gateway_parameters()
        self.plp_vars = PLP_observe_gateway_variables()
        self.current_action = None


if __name__ == '__main__':
    try:
        rospy.init_node("plp_observe_gateway_action_dispatcher", anonymous=False)
        rospy.loginfo("Starting observe_gateway action dispatcher")
        observe_gateway_dispatcher()
        rospy.spin()

    except rospy.ROSInterruptException:
        pass
