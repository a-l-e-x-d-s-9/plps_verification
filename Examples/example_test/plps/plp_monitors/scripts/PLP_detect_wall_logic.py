from PLPClasses import *
from PLP_detect_wall_classes import *

# TODO update this variable to the max variable history needed
PLP_detect_wall_HISTORY_LENGTH = 2

class PLP_detect_wall_logic(object):

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
        return not ((self.plp_params.laser_scan is None))

    # The following methods are used to check the observable conditions of the PLP.
    # Access parameters using: self.plp_params of type PLP_detect_wall_parameters
    # Access variables using: self.variables() of type PLP_detect_wall_variables
    # Access variable history using: self.variables_history[index]
    # Access constants using: self.constants[constant_name]

    def check_condition_wall_far_away(self):
        # TODO implement code that checks the following formula
        # Formula: [initial_distance_to_wall > lower_bound]
        return False

    def validate_preconditions(self):
        return (self.check_condition_wall_far_away())

    def estimate(self):
        result = PLPDetectEstimation()
        result.detection_given_condition_prob = self.estimate_detection_given_condition_prob()

    def estimate_detection_given_condition_prob(self):
        result = ""
        # TODO Implement the code that computes and returns the following probability
        # probability = 0.95
        result = to_implement
        return result

    def detect_success(self):
        if self.plp_params.output is not None:
            # TODO: Optionally, add more conditions on the returned value, to determine if the detection finished successfully
            return PLPTermination(True, " Detected: [distance_to_wall <_equal upper_bound]")
        return None

    def detect_failures(self):
        # TODO: Implement failure to detect. No failed termination conditions specified
        return None

    def monitor_conditions(self):

    def calculate_variables(self):
        variables = PLP_detect_wall_variables()
        variables.distance_to_wall = self.calc_distance_to_wall()
        variables.initial_distance_to_wall = self.calc_initial_distance_to_wall()
        if len(self.variables_history) >= PLP_detect_wall_HISTORY_LENGTH:
            self.variables_history = [variables] + self.variables_history[0:-1]
        else:
            self.variables_history = [variables] + self.variables_history

    def variables(self):
        # The newest variables
        return self.variables_history[0]

    # The following methods are used to update the variables
    # Access parameters using: self.plp_params of type PLP_detect_wall_parameters
    # Access constants using: self.constants[constant_name]

    def calc_distance_to_wall(self):
        # TODO Implement code to calculate distance_to_wall
        # return the value of the variable 
        return None

    def calc_initial_distance_to_wall(self):
        # TODO Implement code to calculate initial_distance_to_wall
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
