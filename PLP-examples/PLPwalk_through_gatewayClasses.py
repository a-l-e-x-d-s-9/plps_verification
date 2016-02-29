class PLPwalk_through_gatewayParameters(object):
    def __init__(self):
        self.callback = None
        # Execution Parameters
        self.areaA = None
        self.areaB = None
        self.gateway = None
        # Input Parameters
        self.gateway_location_gateway = None
        self.laser_scan = None
        self.odometry = None
        self.arm_controller = None
        self.current_Aspeed = None
        self.current_Lspeed = None

    def set_gateway_location_gateway(self, a_gateway_location_gateway):
        self.gateway_location_gateway = a_gateway_location_gateway
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

    def set_arm_controller(self, a_arm_controller):
        self.arm_controller = a_arm_controller
        if self.callback:
            self.callback.parameters_updated()

    def set_current_Aspeed(self, a_current_Aspeed):
        self.current_Aspeed = a_current_Aspeed
        if self.callback:
            self.callback.parameters_updated()

    def set_current_Lspeed(self, a_current_Lspeed):
        self.current_Lspeed = a_current_Lspeed
        if self.callback:
            self.callback.parameters_updated()

class PLPwalk_through_gatewayVariables(object):
    def __init__(self):
        self.collision_alert = None
        self.arm_moving = None

