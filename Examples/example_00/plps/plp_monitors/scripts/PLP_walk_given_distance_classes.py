class PLP_walk_given_distance_parameters(object):
    def __init__(self):
        self.callback = None
        # Execution Parameters
        self.linear_distance = None
        self.angular_distance = None
        # Input Parameters
        self.laser_scan = None
        self.odometry = None
        self.energy_level = None
        # Output Parameters
        self.result = None

    def set_linear_distance(self, a_linear_distance):
        self.linear_distance = a_linear_distance
        if self.callback:
            self.callback.parameters_updated()

    def set_angular_distance(self, a_angular_distance):
        self.angular_distance = a_angular_distance
        if self.callback:
            self.callback.parameters_updated()

    def set_laser_scan(self, a_laser_scan):
        self.laser_scan = a_laser_scan
        if self.callback:
            self.callback.parameters_updated()

    def set_odometry(self, a_odometry):
        self.odometry = a_odometry
        if self.callback:
            self.callback.parameters_updated()

    def set_energy_level(self, a_energy_level):
        self.energy_level = a_energy_level
        if self.callback:
            self.callback.parameters_updated()

    def set_result(self, a_result):
        self.result = a_result
        if self.callback:
            self.callback.parameters_updated()

class PLP_walk_given_distance_variables(object):
    def __init__(self):
        self.current_angle = None
        self.current_position = None
        self.current_linear_speed = None
        self.current_angular_speed = None
        self.begin_point_angle = None
        self.begin_point_position = None
        self.collision_alert = None
        self.arm_moving = None
        self.energy_consumed = None
        self.distance_to_target = None
        self.robot_at_target = None
        self.finished_not_at_target = None

