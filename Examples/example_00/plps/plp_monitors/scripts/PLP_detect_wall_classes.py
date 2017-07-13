class PLP_detect_wall_parameters(object):
    def __init__(self):
        self.callback = None
        # Execution Parameters
        # Input Parameters
        self.laser_scan = None
        # Output Parameters
        self.output = None

    def set_laser_scan(self, a_laser_scan):
        self.laser_scan = a_laser_scan
        if self.callback:
            self.callback.parameters_updated()

    def set_output(self, a_output):
        self.output = a_output
        if self.callback:
            self.callback.parameters_updated()

class PLP_detect_wall_variables(object):
    def __init__(self):
        self.distance_to_wall = None
        self.initial_distance_to_wall = None

