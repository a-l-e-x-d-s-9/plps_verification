from PLPClasses import *
from PLP_observe_gateway_classes import *

# TODO update this variable to the max variable history needed
PLP_observe_gateway_HISTORY_LENGTH = 2

class PLP_observe_gateway_logic(object):

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
        return not ((self.plp_params.areaA is None) or (self.plp_params.areaB is None) or (self.plp_params.gateway is None) or (self.plp_params.rgb_image is None) or (self.plp_params.odometry is None) or (self.plp_params.arm_controller is None))

    # The following methods are used to check the observable conditions of the PLP.
    # Access parameters using: self.plp_params of type PLP_observe_gateway_parameters
    # Access variables using: self.variables() of type PLP_observe_gateway_variables
    # Access variable history using: self.variables_history[index]
    # Access constants using: self.constants[constant_name]

    def check_condition_no_linear_speed(self):
        # TODO implement code that checks the following formula
        # Formula: [begin_Lspeed = 0]
        return False

    def check_condition_connected_areaA_areaB_gateway(self):
        # TODO implement code that checks the following predicate condition
        # Predicate: (connected areaA areaB gateway)
        return False

    def check_condition_no_angular_speed(self):
        # TODO implement code that checks the following formula
        # Formula: [begin_Aspeed = 0]
        return False

    def check_condition_at_areaA(self):
        # TODO implement code that checks the following predicate condition
        # Predicate: (at areaA)
        return False

    def check_condition_arm_not_moving(self):
        # TODO implement code that checks the following formula
        # Formula: [arm_moving = FALSE]
        return False

    def validate_preconditions(self):
        return ((self.check_condition_at_areaA()) and (self.check_condition_connected_areaA_areaB_gateway()) and (self.check_condition_no_angular_speed()) and (self.check_condition_no_linear_speed()))

    def estimate(self):
        result = PLPObserveEstimation()
        result.observation_is_correct_prob = self.estimate_correct_observation()
        result.success_time = self.estimate_success_time()
        result.failure_to_observe_prob = self.estimate_failure_to_observe()
        result.failure_time = self.estimate_failure_time()
        return result

    def estimate_correct_observation(self):
        result = ""
        # TODO Implement the code that computes and returns the following probability
        # probability = 0.95
        result = to_implement
        return result

    def estimate_success_time(self):
        result = ""
        # TODO Implement the code that computes and returns the following distribution
        # distribution = Normal(8,5)
        result = to_implement
        return result

    def estimate_failure_to_observe(self):
        result = ""
        if ((False) or (False)):
            # TODO Implement the code that computes and returns the following probability
            # probability = 1
            result = to_implement

            if ((False) and (False)):
                # TODO Implement the code that computes and returns the following probability
                # probability = 0.1
                result = to_implement

                return result

            def estimate_failure_time(self):
                result = ""
                # TODO Implement the code that computes and returns the following distribution
                # distribution = Uniform(0,20)
                result = to_implement
                return result

            def detect_success(self):
                if self.plp_params.gateway_location_gateway is not None:
                    # TODO: Optionally, add more conditions on the returned value, to determine if the observation finished successfully
                    return PLPTermination(True, " Observed: gateway_location(gateway)")
                return None

            def detect_failures(self):
                # TODO: Implement failure to observe condition. No failed termination conditions specified
                return None

            def monitor_conditions(self):
                if not (self.check_condition_arm_not_moving()):
                    self.callback.plp_monitor_message(PLPMonitorMessage("[arm_moving = FALSE]", False, "Concurrency condition doesn't hold"))

            def calculate_variables(self):
                variables = PLP_observe_gateway_variables()
                variables.collision_alert = self.calc_collision_alert()
                variables.arm_moving = self.calc_arm_moving()
                variables.begin_Aspeed = self.calc_begin_Aspeed()
                variables.begin_Lspeed = self.calc_begin_Lspeed()
                if len(self.variables_history) >= PLP_observe_gateway_HISTORY_LENGTH:
                    self.variables_history = [variables] + self.variables_history[0:-1]
                else:
                    self.variables_history = [variables] + self.variables_history

            def variables(self):
                # The newest variables
                return self.variables_history[0]

            # The following methods are used to update the variables
            # Access parameters using: self.plp_params of type PLP_observe_gateway_parameters
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


            def parameters_updated(self):
                # Called when parameters where updated (might effect variables)
                # Triggers estimation and monitoring. You can uncomment one if you're not interested in it
                termination = self.detect_termination()
                if termination is None:
                    self.request_estimation()
                    self.monitor_conditions()
                else:
                    self.callback.plp_terminated(termination)
