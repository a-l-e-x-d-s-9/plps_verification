def update_assumptions_fail(self):
    assumption_handler_req = ChangeOnFailRequest()
    assumption_handler_req.name = self.current_action.name
    assumption_handler_req.parameters = self.current_action.parameters
    self.change_assumptions_fail_client.call(assumption_handler_req)