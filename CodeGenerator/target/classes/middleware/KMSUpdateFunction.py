def changeKMSFact(self, name, params, changeType):
    knowledge = KnowledgeItem()
    knowledge.knowledge_type = KnowledgeItem.FACT
    knowledge.attribute_name = name
    for param in params:
        pair = KeyValue()
        pair.key = param[0]
        pair.value = param[1]
        knowledge.values.append(pair)
    update_response = self.update_knowledge_client(changeType, knowledge)
    if (update_response.success is not True):
        rospy.loginfo("Failed updating KMS with attribute: %s", knowledge.attribute_name)
    else:
        rospy.loginfo("Updated KMS with attribute: %s", knowledge.attribute_name)

def validateKMSFact(self, name, params, changeType):
    knowledge = KnowledgeItem()
    knowledge.knowledge_type = KnowledgeItem.FACT
    knowledge.attribute_name = name
    for param in params:
        pair = KeyValue()
        pair.key = param[0]
        pair.value = param[1]
        knowledge.values.append(pair)
    QueryItems = []
    QueryItems.append(knowledge)
    return self.query_client.call(QueryItems).all_true


def toDictionary(self, pairs):
    result = []
    for pair in pairs:
        result.append((pair.key,pair.value))
    return dict(result)
