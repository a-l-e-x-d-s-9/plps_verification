from PLPClasses import *
from PLP_maintain_direction_on_map_classes import *

# TODO update this variable to the max variable history needed
PLP_maintain_direction_on_map_HISTORY_LENGTH = 2

class PLP_maintain_direction_on_map_logic(object):

    def __init__(self, constant_map, parameters, callback):

        self.maintained_condition_true = True
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
        return not ((self.plp_params.goal_location is None) or (self.plp_params.map is None) or (self.plp_params.max_change_rate_path_tangent is None) or (self.plp_params.gas_level is None) or (self.plp_params.odometry is None))

    # The following methods are used to check the observable conditions of the PLP.
    # Access parameters using: self.plp_params of type PLP_maintain_direction_on_map_parameters
    # Access variables using: self.variables() of type PLP_maintain_direction_on_map_variables
    # Access variable history using: self.variables_history[index]
    # Access constants using: self.constants[constant_name]

    def check_condition_off_road(self):
        # TODO implement code that checks the following formula
        # Formula: [vehicle_off_road = TRUE]
        return False

    def check_condition_damaged_vehicle(self):
        # TODO implement code that checks the following predicate condition
        # Predicate: (damaged_vehicle)
        return False

    def check_condition_correct_path(self):
        # TODO implement code that checks the following formula
        # Formula: [current_path_direction in [current_robot_direction-path_offset, current_robot_direction+path_offset]]
        return False

    def check_condition_at_goal(self):
        # TODO implement code that checks the following formula
        # Formula: [distance_to_goal <_equal required_distance]
        return False

    def check_condition_moving(self):
        # TODO implement code that checks the following formula
        # Formula: [linear_speed > 0]
        return False

    def check_condition_wet_path(self):
        # TODO implement code that checks the following predicate condition
        # Predicate: (wet_path)
        return False

    def check_condition_allowed_change_rate(self):
        # TODO implement code that checks the following formula
        # Formula: [max_change_rate_path_tangent <_equal max_allowed_change_rate]
        return False

    def validate_preconditions(self):
        return (self.check_condition_allowed_change_rate())

    def estimate(self):
        result = PLPAchieveEstimation()
        result.success = self.estimate_success()
        result.success_time = self.estimate_success_time()
        result.side_effects["gas_down"] = self.estimate_gas_down_side_effect()
        result.add_failure(self.estimate_off_roadANDnot_damaged_vehicle_failure())
        result.add_failure(self.estimate_off_roadANDdamaged_vehicle_failure())
        result.failure_time = self.estimate_failure_time()
        return result

    def estimate_success(self):
        result = ""
        if check_condition_wet_path(self):
            # TODO Implement the code that computes and returns the following probability
            # probability = 0.8
            result = to_implement

            if not check_condition_wet_path(self):
                # TODO Implement the code that computes and returns the following probability
                # probability = 0.95
                result = to_implement

                return result

            def estimate_success_time(self):
                result = ""
                # TODO Implement the code that computes and returns the following distribution
                # distribution = Uniform(runtime_given_path_length,runtime_given_path_length)
                result = to_implement
                return result

            def estimate_gas_down_side_effect(self):
                result = ""
                #TODO Implement the code that computes the parameters new value "val" to be the following:
                # new value = gas_level-gas_consumed
                val = to_implement
                result += "gas_level  = " + repr(val) + ","
                return result

            def estimate_off_roadANDnot_damaged_vehicle_failure(self):
                failureMode = PLPFailureMode()
                failureMode.name = "[AND [vehicle_off_road = TRUE] [Not (damaged_vehicle)]]"
                result = ""
                if check_condition_wet_path(self):
                    # TODO Implement the code that computes and returns the following probability
                    # probability = 0.15
                    result = to_implement

                    if not check_condition_wet_path(self):
                        # TODO Implement the code that computes and returns the following probability
                        # probability = 0.045
                        result = to_implement

                        failureMode.probability = result
                        return failureMode

                    def estimate_off_roadANDdamaged_vehicle_failure(self):
                        failureMode = PLPFailureMode()
                        failureMode.name = "[AND [vehicle_off_road = TRUE] (damaged_vehicle)]"
                        result = ""
                        if check_condition_wet_path(self):
                            # TODO Implement the code that computes and returns the following probability
                            # probability = 0.05
                            result = to_implement

                            if not check_condition_wet_path(self):
                                # TODO Implement the code that computes and returns the following probability
                                # probability = 0.005
                                result = to_implement

                                failureMode.probability = result
                                return failureMode

                            def estimate_failure_time(self):
                                result = ""
                                return result

                            def detect_success(self):
                                if (self.check_condition_at_goal()):
                                    return PLPTermination(True, " Maintained: [distance_to_goal <_equal required_distance]")
                                else:
                                    return None

                            def detect_failures(self):
                                if (self.check_condition_off_road()):
                                    return PLPTermination(False, " Failed by condition: [vehicle_off_road = TRUE]")
                                if ((self.check_condition_off_road()) and (not (self.check_condition_damaged_vehicle()))):
                                    return PLPTermination(False, " Failed by condition: [AND [vehicle_off_road = TRUE] [Not (damaged_vehicle)]]")
                                if ((self.check_condition_off_road()) and (self.check_condition_damaged_vehicle())):
                                    return PLPTermination(False, " Failed by condition: [AND [vehicle_off_road = TRUE] (damaged_vehicle)]")
                                return None

                            def monitor_conditions(self):
                                if not (False):
                                    self.callback.plp_monitor_message(PLPMonitorMessage("[path_free = TRUE]", False, "Concurrency condition doesn't hold"))


                            def monitor_maintained_condition(self):
                                if not self.maintained_condition_true:
                                    if (self.check_condition_correct_path()):
                                        self.maintained_condition_true = True
                                else:
                                    if not (self.check_condition_correct_path()):
                                        self.callback.plp_monitor_message(PLPMonitorMessage("[current_path_direction in [current_robot_direction-path_offset, current_robot_direction+path_offset]]", False, "Maintain condition doesn't hold"))

                            # Checks progress measures. Callback function for ROS Timer
                            def monitor_progress_moving(self, event):
                                if not (self.check_condition_moving()):
                                    self.callback.plp_monitor_message(PLPMonitorMessage("[linear_speed > 0]", False, "Progress measure doesn't hold"))

                            def calculate_variables(self):
                                variables = PLP_maintain_direction_on_map_variables()
                                variables.gas_consumed = self.calc_gas_consumed()
                                variables.current_path_direction = self.calc_current_path_direction()
                                variables.current_robot_direction = self.calc_current_robot_direction()
                                variables.distance_to_goal = self.calc_distance_to_goal()
                                variables.runtime_given_path_length = self.calc_runtime_given_path_length()
                                variables.vehicle_off_road = self.calc_vehicle_off_road()
                                if len(self.variables_history) >= PLP_maintain_direction_on_map_HISTORY_LENGTH:
                                    self.variables_history = [variables] + self.variables_history[0:-1]
                                else:
                                    self.variables_history = [variables] + self.variables_history

                            def variables(self):
                                # The newest variables
                                return self.variables_history[0]

                            # The following methods are used to update the variables
                            # Access parameters using: self.plp_params of type PLP_maintain_direction_on_map_parameters
                            # Access constants using: self.constants[constant_name]

                            def calc_gas_consumed(self):
                                # TODO Implement code to calculate gas_consumed
                                # return the value of the variable 
                                return None

                            def calc_current_path_direction(self):
                                # TODO Implement code to calculate current_path_direction
                                # return the value of the variable 
                                return None

                            def calc_current_robot_direction(self):
                                # TODO Implement code to calculate current_robot_direction
                                # return the value of the variable 
                                return None

                            def calc_distance_to_goal(self):
                                # TODO Implement code to calculate distance_to_goal
                                # return the value of the variable 
                                return None

                            def calc_runtime_given_path_length(self):
                                # TODO Implement code to calculate runtime_given_path_length
                                # return the value of the variable 
                                return None

                            def calc_vehicle_off_road(self):
                                # TODO Implement code to calculate vehicle_off_road
                                # return the value of the variable 
                                return None


                            def parameters_updated(self):
                                # Called when parameters where updated (might effect variables)
                                # Triggers estimation and monitoring. You can uncomment one if you're not interested in it
                                termination = self.detect_termination()
                                if termination is None:
                                    self.request_estimation()
                                    self.monitor_conditions()
                                    self.monitor_maintained_condition()
                                else:
                                    self.callback.plp_terminated(termination)
