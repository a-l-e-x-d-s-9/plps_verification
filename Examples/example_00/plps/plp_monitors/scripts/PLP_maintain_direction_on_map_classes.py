class PLP_maintain_direction_on_map_parameters(object):
    def __init__(self):
        self.callback = None
        # Execution Parameters
        self.goal_location = None
        # Input Parameters
        self.map = None
        self.max_change_rate_path_tangent = None
        self.gas_level = None
        self.odometry = None
        # Output Parameters

    def set_goal_location(self, a_goal_location):
        self.goal_location = a_goal_location
        if self.callback:
            self.callback.parameters_updated()

    def set_map(self, a_map):
        self.map = a_map
        if self.callback:
            self.callback.parameters_updated()

    def set_max_change_rate_path_tangent(self, a_max_change_rate_path_tangent):
        self.max_change_rate_path_tangent = a_max_change_rate_path_tangent
        if self.callback:
            self.callback.parameters_updated()

    def set_gas_level(self, a_gas_level):
        self.gas_level = a_gas_level
        if self.callback:
            self.callback.parameters_updated()

    def set_odometry(self, a_odometry):
        self.odometry = a_odometry
        if self.callback:
            self.callback.parameters_updated()

class PLP_maintain_direction_on_map_variables(object):
    def __init__(self):
        self.gas_consumed = None
        self.current_path_direction = None
        self.current_robot_direction = None
        self.distance_to_goal = None
        self.runtime_given_path_length = None
        self.vehicle_off_road = None

