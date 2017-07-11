from PLPClasses import *
from PLP_walk_given_distance_classes import *

# TODO update this variable to the max variable history needed
PLP_walk_given_distance_HISTORY_LENGTH = 2

class PLP_walk_given_distance_logic(object):

    def __init__(self, constant_map, parameters, callback):

        # constants and fields
        self.constants = constant_map
        self.callback = callback
        self.plp_params = parameters
        self.variables_history = list()

    def request_estimation(self):
        """
        Manually trigger estimation attempt.
        Typical client code use is from the harness,
        immediately after instantiating the PLP object.
        """
        if self.can_estimate():
            res = self.get_estimation()
            if res is not None:
                self.callback.plp_estimation(res)
            else:
                self.callback.plp_no_preconditions()
        else:
            self.callback.plp_missing_data() # plp_cannot_estimate(msg)

    def get_estimation(self): 
        """
        Generate an estimation iff the preconditions are met.
        Otherwise, return None.
        """
        self.calculate_variables()
        if self.validate_preconditions():
            return self.estimate()
        else:
            return None

    def detect_termination(self):
        """
        See if any of the termination conditions applies.
        :return: A PLPTermination object, or None.
        """
        res = self.detect_success()
        if not ( res is None ):
            return res

        res = self.detect_failures()
        if not ( res is None ):
            return res

        return None

    def can_estimate(self):
        # Can estimate if got values for all of the parameters
        # TODO: if not all parameters needed in order to estimate, remove some of the following conditions:
        return not ((self.plp_params.linear_distance is None) or (self.plp_params.angular_distance is None) or (self.plp_params.laser_scan is None) or (self.plp_params.odometry is None) or (self.plp_params.energy_level is None))

    # The following methods are used to check the observable conditions of the PLP.
    # Access parameters using: self.plp_params of type PLP_walk_given_distance_parameters
    # Access variables using: self.variables() of type PLP_walk_given_distance_variables
    # Access variable history using: self.variables_history[index]
    # Access constants using: self.constants[constant_name]

    def check_condition_lspeed_in_range(self):
        # TODO implement code that checks the following formula
        # Formula: [current_Lspeed in [linear_speed-lspeed_offset, linear_speed+lspeed_offset]]
        return False

    def check_condition_closer_to_target(self):
        # TODO implement code that checks the following formula
        # Formula: [distance_to_target < distance_to_target@pre]
        return False

    def check_condition_aspeed_in_range(self):
        # TODO implement code that checks the following formula
        # Formula: [current_Aspeed in [angular_speed-aspeed_offset, angular_speed+aspeed_offset]]
        return False

    def check_condition_not_moving_linear(self):
        # TODO implement code that checks the following formula
        # Formula: [current_linear_speed = 0]
        return False

    def check_condition_not_arm_moving(self):
        # TODO implement code that checks the following formula
        # Formula: [arm_moving = FALSE]
        return False

    def check_condition_at_target(self):
        # TODO implement code that checks the following formula
        # Formula: [robot_at_target = TRUE]
        return False

    def check_condition_has_collision(self):
        # TODO implement code that checks the following formula
        # Formula: [collision_alert = TRUE]
        return False

    def check_condition_not_moving_angular(self):
        # TODO implement code that checks the following formula
        # Formula: [current_angular_speed = 0]
        return False

    def check_condition_finished_not_at_target(self):
        # TODO implement code that checks the following formula
        # Formula: [finished_not_at_target = TRUE]
        return False

    def validate_preconditions(self):
        return ((self.check_condition_not_moving_linear()) and (self.check_condition_not_moving_angular()))

    def estimate(self):
        result = PLPAchieveEstimation()
        result.success = self.estimate_success()
        result.success_time = self.estimate_success_time()
        result.side_effects["energy_down"] = self.estimate_energy_down_side_effect()
        result.add_failure(self.estimate_has_collision_failure())
        result.add_failure(self.estimate_finished_not_at_target_failure())
        result.failure_time = self.estimate_failure_time()
        return result

    def estimate_success(self):
        result = ""
        # TODO Implement the code that computes "prob" to be the following probability
        # probability = 0.95
        prob = to_implement
        result += "If [clear_path = TRUE] : " + repr(prob) + ","

        # TODO Implement the code that computes "prob" to be the following probability
        # probability = 0
        prob = to_implement
        result += "If [clear_path = FALSE] : " + repr(prob) + ","

        return result

    def estimate_success_time(self):
        result = ""
        # TODO Implement the code that computes and returns the following distribution
        # distribution = Uniform((linear_distance/req_linear_speed) + (angular_distance/req_angular_speed) - timeOffset,(linear_distance/req_linear_speed) + (angular_distance/req_angular_speed) + timeOffset)
        result = to_implement
        return result

    def estimate_energy_down_side_effect(self):
        result = ""
        #TODO Implement the code that computes the parameters new value "val" to be the following:
        # new value = energy_level - energy_consumed
        val = to_implement
        result += "energy_level  = " + repr(val) + ","
        return result

    def estimate_has_collision_failure(self):
        failureMode = PLPFailureMode()
        failureMode.name = "[collision_alert = TRUE]"
        result = ""
        # TODO Implement the code that computes "prob" to be the following probability
        # probability = 0
        prob = to_implement
        result += "If [clear_path = TRUE] : " + repr(prob) + ","

        # TODO Implement the code that computes "prob" to be the following probability
        # probability = 1
        prob = to_implement
        result += "If [clear_path = FALSE] : " + repr(prob) + ","

        failureMode.probability = result
        return failureMode

    def estimate_finished_not_at_target_failure(self):
        failureMode = PLPFailureMode()
        failureMode.name = "[finished_not_at_target = TRUE]"
        result = ""
        # TODO Implement the code that computes and returns the following probability
        # probability = 0.05
        result = to_implement
        failureMode.probability = result
        return failureMode

    def estimate_failure_time(self):
        result = ""
        # TODO Implement the code that computes and returns the following distribution
        # distribution = Uniform(0,(linear_distance/req_linear_speed) + (angular_distance/req_angular_speed) + timeOffset)
        result = to_implement
        return result

    def detect_success(self):
        if (self.check_condition_at_target()):
            return PLPTermination(True, " Achieved: [robot_at_target = TRUE]")
        else:
            return None

    def detect_failures(self):
        if (self.check_condition_has_collision()):
            return PLPTermination(False, " Failed by condition: [collision_alert = TRUE]")
        if (self.check_condition_finished_not_at_target()):
            return PLPTermination(False, " Failed by condition: [finished_not_at_target = TRUE]")
        return None

    def monitor_conditions(self):
        if not (self.check_condition_not_arm_moving()):
            self.callback.plp_monitor_message(PLPMonitorMessage("[arm_moving = FALSE]", False, "Concurrency condition doesn't hold"))

    # Checks progress measures. Callback function for ROS Timer
    def monitor_progress_closer_to_target(self, event):
        if not (self.check_condition_closer_to_target()):
            self.callback.plp_monitor_message(PLPMonitorMessage("[distance_to_target < distance_to_target@pre]", False, "Progress measure doesn't hold"))

    # Checks progress measures. Callback function for ROS Timer
    def monitor_progress_aspeed_in_rangeORlspeed_in_range(self, event):
        if not ((self.check_condition_aspeed_in_range()) or (self.check_condition_lspeed_in_range())):
            self.callback.plp_monitor_message(PLPMonitorMessage("[OR [current_Aspeed in [angular_speed-aspeed_offset, angular_speed+aspeed_offset]] [current_Lspeed in [linear_speed-lspeed_offset, linear_speed+lspeed_offset]]]", False, "Progress measure doesn't hold"))

    def calculate_variables(self):
        variables = PLP_walk_given_distance_variables()
        variables.current_angle = self.calc_current_angle()
        variables.current_position = self.calc_current_position()
        variables.current_linear_speed = self.calc_current_linear_speed()
        variables.current_angular_speed = self.calc_current_angular_speed()
        variables.begin_point_angle = self.calc_begin_point_angle()
        variables.begin_point_position = self.calc_begin_point_position()
        variables.collision_alert = self.calc_collision_alert()
        variables.arm_moving = self.calc_arm_moving()
        variables.energy_consumed = self.calc_energy_consumed()
        variables.distance_to_target = self.calc_distance_to_target()
        variables.robot_at_target = self.calc_robot_at_target()
        variables.finished_not_at_target = self.calc_finished_not_at_target()
        if len(self.variables_history) >= PLP_walk_given_distance_HISTORY_LENGTH:
            self.variables_history = [variables] + self.variables_history[0:-1]
        else:
            self.variables_history = [variables] + self.variables_history

    def variables(self):
        # The newest variables
        return self.variables_history[0]

    # The following methods are used to update the variables
    # Access parameters using: self.plp_params of type PLP_walk_given_distance_parameters
    # Access constants using: self.constants[constant_name]

    def calc_current_angle(self):
        # TODO Implement code to calculate current_angle
        # return the value of the variable 
        return None

    def calc_current_position(self):
        # TODO Implement code to calculate current_position
        # return the value of the variable 
        return None

    def calc_current_linear_speed(self):
        # TODO Implement code to calculate current_linear_speed
        # return the value of the variable 
        return None

    def calc_current_angular_speed(self):
        # TODO Implement code to calculate current_angular_speed
        # return the value of the variable 
        return None

    def calc_begin_point_angle(self):
        # TODO Implement code to calculate begin_point_angle
        # return the value of the variable 
        return None

    def calc_begin_point_position(self):
        # TODO Implement code to calculate begin_point_position
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

    def calc_energy_consumed(self):
        # TODO Implement code to calculate energy_consumed
        # return the value of the variable 
        return None

    def calc_distance_to_target(self):
        # TODO Implement code to calculate distance_to_target
        # return the value of the variable 
        return None

    def calc_robot_at_target(self):
        # TODO Implement code to calculate robot_at_target
        # return the value of the variable 
        return None

    def calc_finished_not_at_target(self):
        # TODO Implement code to calculate finished_not_at_target
        # return the value of the variable 
        return None


    def parameters_updated(self):
        # Called when parameters where updated (might effect variables)
        # Triggers estimation and monitoring. You can uncomment one if you're not interested in it
        termination = self.detect_termination()
        if termination is None:
            self.request_estimation()
            self.monitor_conditions()
        else:
            self.callback.plp_terminated(termination)
