#!/usr/bin/env python
import rospy
import sys

from nav_msgs.msg import Odometry
from service_robot_msgs.msg import ObGWcmd
from sensor_msgs.msg import Image
from dynamixel_msgs.msg import MotorStateList
from geometry_msgs.msg import Pose
from plp_monitors.msg import PLPMessage
from PLP_observe_gateway_logic import *
from PLP_observe_gateway_classes import *

PLP_TOPIC = "/plp/messages"

class PLP_observe_gateway_ros_harness(object):

    def __init__(self):

        self.plp_constants = {
        }
        # The following method call is for any initialization code you might need
        self.node_setup()

        # Setup internal PLP objects.
        self.plp = None
        self.plp_params = PLP_observe_gateway_parameters()

        # Parse arguments. decide on trigger mode: Capture, Run PLP, or both.
        self.capture = False
        self.monitor = False
        self.capture_filename = ""
        start_args = rospy.myargv(argv=sys.argv)
        for arg in start_args[1:]:
            if arg == "-capture":
                self.capture = True
            elif arg == "-monitor":
                self.monitor = True
            elif self.capture:
                self.capture_filename = arg
            else:
                rospy.loginfo("Unsupported input argument. Usage:\n -capture <file_name> \n -monitor")

        # default: just use the PLP.
        if not (self.capture or self.monitor):
            self.monitor = True

        if self.capture and self.capture_filename == "":
            self.capture_filename = "capture-file"

        self.triggered = False

        # ROS related stuff
        rospy.init_node("plp_observe_gateway", anonymous=False)
        self.publisher = rospy.Publisher(PLP_TOPIC, PLPMessage, queue_size=5)
        rospy.Subscriber("/observe_gateway_cmd", ObGWcmd, self.param_areaA_updated)
        rospy.Subscriber("/observe_gateway_cmd", ObGWcmd, self.param_areaB_updated)
        rospy.Subscriber("/observe_gateway_cmd", ObGWcmd, self.param_gateway_updated)
        rospy.Subscriber("/komodo_1/komodo_1_Asus_Camera/rgb/image_raw", Image, self.param_rgb_image_updated)
        rospy.Subscriber("/komodo_1/odom_pub", Odometry, self.param_odometry_updated)
        rospy.Subscriber("/komodo_1/motor_states/arm_port", MotorStateList, self.param_arm_controller_updated)
        rospy.Subscriber("/observe_gateway_res", Pose, self.param_gateway_location_gateway_updated)


        if self.monitor:
            rospy.loginfo("<PLP:observe_gateway>: Monitoring")
        if self.capture:
            rospy.loginfo("<PLP:observe_gateway>: Capturing (filename: " + self.capture_filename + ")")

        rospy.loginfo("<PLP:observe_gateway> Harness - Started")

    def node_setup(self):
        """
         Custom node initialization code here
        """
        return

    def consider_trigger(self):
        """
        Tests whether or not to trigger the plp, based on the check_trigger function
        """

        if self.monitor and not self.triggered:
            if self.check_trigger():
                self.triggered = True
                self.trigger_plp_task()
        if self.capture:
            if self.check_trigger():
                self.triggered = True
                self.capture_params()


    # PLP Callback methods
    def plp_terminated(self, plp_termination):
        """
        The PLP detected that one of its termination conditions have occurred.
        Deletes the current PLP, resets the harness.
        :param plp_termination: The termination message sent from the PLP.
        """
        rospy.loginfo("<PLP:observe_gateway> terminated")

        if plp_termination.is_success():
            msg = "Success"
        else:
            msg = "Failure occurred " + plp_termination.get_message()

        self.publisher.publish(
            PLPMessage("observe_gateway", "ObservePLP", msg))
        self.reset_harness_data()

    def plp_no_preconditions(self):
        """
        Called when the PLP is active and would have given an estimation, but the preconditions don't hold
        """
        self.publisher.publish(
            PLPMessage("observe_gateway", "info", "<PLP:observe_gateway> triggered, but its preconditions don't hold"))

    def plp_missing_data(self):
        """
        Called by the PLP when it should have delivered an estimation, but there is not enough data (missing parameter)
        """
        self.publisher.publish(PLPMessage(None, "observe_gateway", "info", "<PLP:observe_gateway> triggered, but its missing some data"))

    def plp_monitor_message(self, message):
        self.publisher.publish(
            PLPMessage("observe_gateway", "monitor",
                       repr(message)))

    def plp_estimation(self, plp_est):
        """
        The PLP is active, and gives an estimation.
        """
        self.publisher.publish(
            PLPMessage("observe_gateway", "estimation",
                       repr(plp_est)))

    def reset_harness_data(self):
        self.plp = None
        self.plp_params.callback = None
        self.plp_params = PLP_observe_gateway_parameters()
        self.triggered = False

    def trigger_plp_task(self):
        # Creates a PLP and starts the monitoring, if there's no PLP yet.
        rospy.loginfo("<PLP:observe_gateway> trigger detected, starting " + "monitoring" if self.monitor else "capturing")
        self.plp = PLP_observe_gateway_logic(self.plp_constants, self.plp_params, self)
        self.plp_params.callback = self.plp
        # Progress measures callbacks
        self.plp.request_estimation()

    def capture_params(self):
        capture_file = open(self.capture_filename, "w")
        capture_file.write("Parameter: areaA, Value: ")
        capture_file.write(repr(self.plp_params.areaA))
        capture_file.write("Parameter: areaB, Value: ")
        capture_file.write(repr(self.plp_params.areaB))
        capture_file.write("Parameter: gateway, Value: ")
        capture_file.write(repr(self.plp_params.gateway))
        capture_file.write("Parameter: rgb_image, Value: ")
        capture_file.write(repr(self.plp_params.rgb_image))
        capture_file.write("Parameter: odometry, Value: ")
        capture_file.write(repr(self.plp_params.odometry))
        capture_file.write("Parameter: arm_controller, Value: ")
        capture_file.write(repr(self.plp_params.arm_controller))
        capture_file.write("Parameter: gateway_location(gateway), Value: ")
        capture_file.write(repr(self.plp_params.gateway_location_gateway))

        capture_file.close()
        rospy.loginfo("<PLP:observe_gateway> captured parameters at trigger time to file '%s'" % self.capture_filename)

    def param_areaA_updated(self, msg):
        self.plp_params.set_areaA(msg.areaA)
        self.consider_trigger()

    def param_areaB_updated(self, msg):
        self.plp_params.set_areaB(msg.areaB)
        self.consider_trigger()

    def param_gateway_updated(self, msg):
        self.plp_params.set_gateway(msg.gateway)
        self.consider_trigger()

    def param_rgb_image_updated(self, msg):
        self.plp_params.set_rgb_image(msg)
        # If this parameter effects the trigger for the robotic module, uncomment the following line
        # self.consider_trigger()

    def param_odometry_updated(self, msg):
        self.plp_params.set_odometry(msg)
        # If this parameter effects the trigger for the robotic module, uncomment the following line
        # self.consider_trigger()

    def param_arm_controller_updated(self, msg):
        self.plp_params.set_arm_controller(msg)
        # If this parameter effects the trigger for the robotic module, uncomment the following line
        # self.consider_trigger()

    def param_gateway_location_gateway_updated(self, msg):
        self.plp_params.set_gateway_location_gateway(msg)
        # If this parameter effects the trigger for the robotic module, uncomment the following line
        # self.consider_trigger()

    def check_trigger(self):
        # The execution parameters are considered the trigger
        # If the trigger includes requirements on other parameters, add them using self.plp_params.<param_name> and uncomment the relevant line in the update functions above
        # You can also use the defined constants using self.plp_constants[<constant_name>]
        # (All the parameters are defined in PLP_observe_gateway_classes.py)
        return not ((self.plp_params.areaA is None) or (self.plp_params.areaB is None) or (self.plp_params.gateway is None))

if __name__ == '__main__':
    try:
        rospy.loginfo("<PLP:observe_gateway> node starting")
        harness = PLP_observe_gateway_ros_harness()
        rospy.spin()
    except rospy.ROSInterruptException:
        pass
