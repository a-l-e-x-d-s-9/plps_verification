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

    def set_areaA(self, a_areaA):
        self.areaA = a_areaA
        if self.callback:
            self.callback.parameters_updated()

    def set_areaB(self, a_areaB):
        self.areaB = a_areaB
        if self.callback:
            self.callback.parameters_updated()

    def set_gateway(self, a_gateway):
        self.gateway = a_gateway
        if self.callback:
            self.callback.parameters_updated()

    def set_rgb_image(self, a_rgb_image):
        self.rgb_image = a_rgb_image
        if self.callback:
            self.callback.parameters_updated()

    def set_odometry(self, a_odometry):
        self.odometry = a_odometry
        if self.callback:
            self.callback.parameters_updated()

    def set_arm_controller(self, a_arm_controller):
        self.arm_controller = a_arm_controller
        if self.callback:
            self.callback.parameters_updated()

    def set_gateway_location_gateway(self, a_gateway_location_gateway):
        self.gateway_location_gateway = a_gateway_location_gateway
        if self.callback:
            self.callback.parameters_updated()

class PLP_observe_gateway_variables(object):
    def __init__(self):
        self.collision_alert = None
        self.arm_moving = None
        self.begin_Aspeed = None
        self.begin_Lspeed = None

