#!/usr/bin/env python
import rospy
import sys

from nav_msgs.msg import Odometry
from geometry_msgs.msg import Pose
from grid_map.msg import GridMap
from plp_monitors.msg import PLPMessage
from PLP_maintain_direction_on_map_logic import *
from PLP_maintain_direction_on_map_classes import *

PLP_TOPIC = "/plp/messages"

class PLP_maintain_direction_on_map_ros_harness(object):

    def __init__(self):

        self.plp_constants = {
            "max_allowed_change_rate": ''' TODO: wasn't specified ''',
            "required_distance": ''' TODO: wasn't specified ''',
            "exception_distance": ''' TODO: wasn't specified ''',
            "path_offset": ''' TODO: wasn't specified ''',
        }
        # The following method call is for any initialization code you might need
        self.node_setup()

        # Setup internal PLP objects.
        self.plp = None
        self.plp_params = PLP_maintain_direction_on_map_parameters()

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
        rospy.init_node("plp_maintain_direction_on_map", anonymous=False)
        self.publisher = rospy.Publisher(PLP_TOPIC, PLPMessage, queue_size=5)
        rospy.Subscriber("/maintain_dir_cmd", Pose, self.param_goal_location_updated)
        rospy.Subscriber("/map_publisher", GridMap, self.param_map_updated)
        # No glue mapping for parameter: max_change_rate_path_tangent
        # No glue mapping for parameter: gas_level
        rospy.Subscriber("/komodo_1/odom_pub", Odometry, self.param_odometry_updated)


        if self.monitor:
            rospy.loginfo("<PLP:maintain_direction_on_map>: Monitoring")
        if self.capture:
            rospy.loginfo("<PLP:maintain_direction_on_map>: Capturing (filename: " + self.capture_filename + ")")

        rospy.loginfo("<PLP:maintain_direction_on_map> Harness - Started")

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
        rospy.loginfo("<PLP:maintain_direction_on_map> terminated")

        if plp_termination.is_success():
            msg = "Success"
        else:
            msg = "Failure occurred " + plp_termination.get_message()

        self.publisher.publish(
            PLPMessage("maintain_direction_on_map", "MaintainPLP", msg))
        self.reset_harness_data()

    def plp_no_preconditions(self):
        """
        Called when the PLP is active and would have given an estimation, but the preconditions don't hold
        """
        self.publisher.publish(
            PLPMessage("maintain_direction_on_map", "info", "<PLP:maintain_direction_on_map> triggered, but its preconditions don't hold"))

    def plp_missing_data(self):
        """
        Called by the PLP when it should have delivered an estimation, but there is not enough data (missing parameter)
        """
        self.publisher.publish(PLPMessage(None, "maintain_direction_on_map", "info", "<PLP:maintain_direction_on_map> triggered, but its missing some data"))

    def plp_monitor_message(self, message):
        self.publisher.publish(
            PLPMessage("maintain_direction_on_map", "monitor",
                       repr(message)))

    def plp_estimation(self, plp_est):
        """
        The PLP is active, and gives an estimation.
        """
        self.publisher.publish(
            PLPMessage("maintain_direction_on_map", "estimation",
                       repr(plp_est)))

    def reset_harness_data(self):
        self.plp = None
        self.plp_params.callback = None
        self.plp_params = PLP_maintain_direction_on_map_parameters()
        self.triggered = False
        self.timer1.shutdown()

    def trigger_plp_task(self):
        # Creates a PLP and starts the monitoring, if there's no PLP yet.
        rospy.loginfo("<PLP:maintain_direction_on_map> trigger detected, starting " + "monitoring" if self.monitor else "capturing")
        self.plp = PLP_maintain_direction_on_map_logic(self.plp_constants, self.plp_params, self)
        self.plp_params.callback = self.plp
        # Progress measures callbacks
        self.timer1 = rospy.Timer(rospy.Duration(1.0), harness.plp.monitor_progress_moving)
        self.plp.request_estimation()

    def capture_params(self):
        capture_file = open(self.capture_filename, "w")
        capture_file.write("Parameter: goal_location, Value: ")
        capture_file.write(repr(self.plp_params.goal_location))
        capture_file.write("Parameter: map, Value: ")
        capture_file.write(repr(self.plp_params.map))
        capture_file.write("Parameter: max_change_rate_path_tangent, Value: ")
        capture_file.write(repr(self.plp_params.max_change_rate_path_tangent))
        capture_file.write("Parameter: gas_level, Value: ")
        capture_file.write(repr(self.plp_params.gas_level))
        capture_file.write("Parameter: odometry, Value: ")
        capture_file.write(repr(self.plp_params.odometry))

        capture_file.close()
        rospy.loginfo("<PLP:maintain_direction_on_map> captured parameters at trigger time to file '%s'" % self.capture_filename)

    def param_goal_location_updated(self, msg):
        self.plp_params.set_goal_location(msg)
        self.consider_trigger()

    def param_map_updated(self, msg):
        self.plp_params.set_map(msg)
        # If this parameter effects the trigger for the robotic module, uncomment the following line
        # self.consider_trigger()

    # TODO: Implement update function for parameter: max_change_rate_path_tangent. No glue mapping found.

    # TODO: Implement update function for parameter: gas_level. No glue mapping found.

    def param_odometry_updated(self, msg):
        self.plp_params.set_odometry(msg)
        # If this parameter effects the trigger for the robotic module, uncomment the following line
        # self.consider_trigger()

    def check_trigger(self):
        # The execution parameters are considered the trigger
        # If the trigger includes requirements on other parameters, add them using self.plp_params.<param_name> and uncomment the relevant line in the update functions above
        # You can also use the defined constants using self.plp_constants[<constant_name>]
        # (All the parameters are defined in PLP_maintain_direction_on_map_classes.py)
        return not ((self.plp_params.goal_location is None))

if __name__ == '__main__':
    try:
        rospy.loginfo("<PLP:maintain_direction_on_map> node starting")
        harness = PLP_maintain_direction_on_map_ros_harness()
        rospy.spin()
    except rospy.ROSInterruptException:
        pass
