package verification;

import conditions.*;
import loader.PLPLoader;
import modules.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import plpEtc.FieldType;
import plpEtc.Range;
import plpFields.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static loader.PLPLoader.*;

/**
 * Created by alexds9 on 28/11/16.
 */

public class VerificationManager {

    private String configuration_file;

    private String control_graph_file;
    private String output_uppaal_file;
    private List<PLP> all_plps = new LinkedList<>();

    private PLPCatalog                  plp_catalog;
    private VerificationVariableManager variable_manager;
    private VerificationSettings        settings;
    private XMLtoUppaalConverter        xml_to_uppaal_converter;
    private UppaalSystem                uppaal_system;
    private VerificationReports         reports;
    private VerificationGenerator       generator;
    private ConcurrentModule            concurrent_module;


    public VerificationManager(String control_graph_file, String output_uppaal_file, String configuration_file) {

        this.control_graph_file         = control_graph_file;
        this.output_uppaal_file         = output_uppaal_file;
        this.configuration_file         = configuration_file;

        this.all_plps                   = new LinkedList<>();

        this.plp_catalog                = new PLPCatalog();
        this.variable_manager           = new VerificationVariableManager( this.plp_catalog );
        this.settings                   = new VerificationSettings();
        this.uppaal_system              = new UppaalSystem();
        this.reports                    = new VerificationReports();
        this.xml_to_uppaal_converter    = new XMLtoUppaalConverter( this.variable_manager, this.settings, this.reports );
        this.generator                  = new VerificationGenerator( this.variable_manager, this.settings, this.xml_to_uppaal_converter, this.reports,
                                                                     this.plp_catalog );
        this.concurrent_module          = new ConcurrentModule();

        this.all_plps.addAll(getAchievePLPs());
        this.all_plps.addAll(getMaintainPLPs());
        this.all_plps.addAll(getObservePLPs());
        this.all_plps.addAll(getDetectPLPs());
    }



    private void uppaal_file_initialize() {
        generator.verification_declarations_start           = new StringBuffer( load_resource_file("/verification/verification_declarations_start") );
        generator.verification_declarations_end             = load_resource_file("/verification/verification_declarations_end"                      );
        generator.verification_modules_start                = load_resource_file("/verification/verification_modules_start"                         );
        generator.verification_modules_end                  = new StringBuffer( load_resource_file("/verification/verification_modules_end"       ) );
        generator.verification_system_declarations_start    = load_resource_file("/verification/verification_system_declarations_start"             );
        generator.verification_system_declarations_end      = load_resource_file("/verification/verification_system_declarations_end"               );
        generator.verification_queries                      = load_resource_file("/verification/verification_queries"                               );
        generator.verification_plp_achieve                  = load_resource_file("/verification/verification_plp_achieve"                           );
        generator.verification_plp_detect                   = load_resource_file("/verification/verification_plp_detect"                            );
        generator.verification_plp_maintain                 = load_resource_file("/verification/verification_plp_maintain"                          );
        generator.verification_plp_observe                  = load_resource_file("/verification/verification_plp_observe"                           );
    }



    private String load_resource_file(String resource_path) {
        try {
            InputStream inputStream;


            inputStream = this.getClass().getResource(resource_path).openStream();
            return new BufferedReader(new InputStreamReader(inputStream)).lines().parallel().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return "";
        }
    }



    private void configuration_load_variable( Node currentNode, boolean is_parameter ) throws VerificationException
    {
        if ( currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE ) {
            String got_name                 = currentNode.getAttributes().getNamedItem("name").getNodeValue();
            String got_plp_name             = "";
            String got_value                = currentNode.getAttributes().getNamedItem("value").getNodeValue();
            String got_min_value            = "";
            String got_max_value            = "";
            String got_is_exclusive_access  = "";
            String set_plp                  = "G";
            String set_id                   = "NO";
            String set_value                = "";
            String set_overwrite            = "NO";
            String set_min_value            = "";
            String set_max_value            = "";

            Node node_min_value = currentNode.getAttributes().getNamedItem("min_value");
            if ( node_min_value != null && node_min_value.getNodeType() == Node.ATTRIBUTE_NODE) {
                got_min_value = node_min_value.getNodeValue();
            }

            Node node_max_value = currentNode.getAttributes().getNamedItem("max_value");
            if ( node_max_value != null && node_max_value.getNodeType() == Node.ATTRIBUTE_NODE) {
                got_max_value = node_max_value.getNodeValue();
            }

            Node node_set_is_exclusive_access = currentNode.getAttributes().getNamedItem("is_exclusive_access");
            if ( node_set_is_exclusive_access != null && node_set_is_exclusive_access.getNodeType() == Node.ATTRIBUTE_NODE) {
                got_is_exclusive_access = node_set_is_exclusive_access.getNodeValue();
            }


            int variable_id;
            VerificationVariable variable_data;
            int plp_id;

            if ( is_parameter )
            {
                Node node_plp_name = currentNode.getAttributes().getNamedItem("plp_name");
                if ( node_plp_name != null && node_plp_name.getNodeType() == Node.ATTRIBUTE_NODE) {
                    got_plp_name = node_plp_name.getNodeValue();

                    if ( true == this.plp_catalog.plp_name_is_exist(got_plp_name) )
                    {
                        plp_id           = this.plp_catalog.find_plp_id_by_name( got_plp_name );
                        int parameter_id = this.variable_manager.local_parameters_get_id( plp_id, got_name );
                        VerificationParameter parameter_data = this.variable_manager.local_parameters_get_data(plp_id, parameter_id);

                        variable_id = parameter_data.variable_id;
                        set_plp     = String.valueOf(plp_id);
                    }
                    else
                    {
                        throw new VerificationException("Parameter \"" + got_name + "\" invalid PLP ID." );
                    }
                }
                else
                {
                    throw new VerificationException("Parameter \"" + got_name + "\" missing valid PLP." );
                }
            }
            else
            {
                if ( true == this.variable_manager.global_variable_is_exist( got_name ) ) {
                    variable_id = this.variable_manager.global_variable_get_id(got_name);
                }
                else
                {
                    variable_data   = new VerificationVariable(VerificationVariable.VerificationVariableType.type_control,
                            VerificationVariable.VerificationValueType.value_integer, 0, false, false,
                            0, 0);
                    variable_id     = this.variable_manager.global_variable_add(got_name, variable_data );
                }
            }

            variable_data   = this.variable_manager.global_variable_get_data(variable_id);
            set_id          = String.valueOf(variable_id);

            if ( true == variable_data.is_set )
            {
                set_overwrite = "YES";
            }

            variable_data.is_set = true;
            variable_data.value = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int(got_value);
            if ( ( VerificationVariable.VerificationValueType.value_boolean == variable_data.value_type) &&
                 ( 0 != variable_data.value ) )
            {
                    variable_data.value = 1;
            }


            if ( false == got_min_value.isEmpty() )
            {
                variable_data.min_value = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int(got_min_value);
                set_min_value = String.valueOf( variable_data.min_value );
                variable_data.is_in_range = true;

                if ( variable_data.value < variable_data.min_value )
                {
                    variable_data.value = variable_data.min_value;
                }
            }

            if ( false == got_max_value.isEmpty() )
            {
                variable_data.max_value = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int(got_max_value);
                set_max_value = String.valueOf( variable_data.max_value );
                variable_data.is_in_range = true;

                if ( variable_data.value > variable_data.max_value )
                {
                    variable_data.value = variable_data.max_value;
                }
            }

            set_value = String.valueOf(variable_data.value);

            if ( false == got_is_exclusive_access.isEmpty() )
            {
                if ( true == got_is_exclusive_access.equals("false") )
                {
                    variable_data.is_exclusive_access = false;
                }
                else
                {
                    variable_data.is_exclusive_access = true;
                }
            }

            System.out.format("%3s|%4s|%-28.28s| %9s [%7s,%7s]%3s |%7s [%7s,%7s]\n", set_plp, set_id, got_name, got_value, got_min_value, got_max_value, set_overwrite, set_value, set_min_value, set_max_value);

        }
    }

    private Element file_open(String file_name ) throws VerificationException
    {
        Element rootElement;

        try {
            File plpFile = new File(file_name);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(plpFile);

            doc.getDocumentElement().normalize();

            rootElement = doc.getDocumentElement();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new VerificationException("Error on load of configuration file.");
        }

        return rootElement;
    }

    private void configuration_file_load_settings( Element rootElement ) throws VerificationException
    {
        int setting_amount = rootElement.getElementsByTagName("setting").getLength();

        if ( setting_amount > 0 )
        {

            System.out.println("******************************************* Load Settings ******************************************");
            System.out.format( "%-70.70s|%-29.29s\n", " Setting_Name", " Setting_Value" );
            System.out.println("----------------------------------------------------------------------------------------------------");
            for ( int i = 0 ; i < setting_amount ; i++  ) {
                Node currentNode = rootElement.getElementsByTagName("setting").item(i);


                if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {

                    Node node_setting_name  = currentNode.getAttributes().getNamedItem("name");
                    Node node_setting_value = currentNode.getAttributes().getNamedItem("value");
                    if ( node_setting_name  != null && node_setting_name.getNodeType()  == Node.ATTRIBUTE_NODE &&
                         node_setting_value != null && node_setting_value.getNodeType() == Node.ATTRIBUTE_NODE ) {

                        String setting_name     = node_setting_name.getNodeValue();
                        String setting_value    = node_setting_value.getNodeValue();

                        if (true == this.settings.is_exist_setting(setting_name)) {
                            this.settings.set(setting_name, setting_value);
                            System.out.format("%-70.70s|%-29.29s\n", setting_name, setting_value );

                        }
                    }
                }
            }

            System.out.println("****************************************************************************************************");
        }
    }

    private void configuration_file_load_variables( Element rootElement ) throws VerificationException
    {
        System.out.println("******************************************* Configurations *****************************************");
        System.out.println("PLP| ID |Variable_Name               | Got_Value [Got_Min,Got_Max]Rep?|  Value [Set_Min,Set_Max]");
        System.out.println("----------------------------------------------------------------------------------------------------");

        int variables_amount = rootElement.getElementsByTagName("variable").getLength();
        for ( int i = 0 ; i < variables_amount ; i++  ) {
            Node currentNode = rootElement.getElementsByTagName("variable").item(i);

            configuration_load_variable( currentNode, false );
        }

        int parameters_amount = rootElement.getElementsByTagName("parameter").getLength();
        for ( int i = 0 ; i < parameters_amount ; i++  ) {
            Node currentNode = rootElement.getElementsByTagName("parameter").item(i);

            configuration_load_variable( currentNode, true );
        }

        System.out.println("****************************************************************************************************");

    }

    private boolean is_node_element( Node node )
    {
        return ( node != null ) && ( node.getNodeType() == Node.ELEMENT_NODE );
    }

    private boolean is_node_attribute( Node node )
    {
        return ( node != null ) && ( node.getNodeType() == Node.ATTRIBUTE_NODE );
    }

    private ControlGraph file_load_control_graph( Element elementBase ) throws VerificationException
    {
        Node elementRoot = elementBase.getElementsByTagName("root").item(0);
        ControlGraph control_graph = new ControlGraph( this.xml_to_uppaal_converter, this.plp_catalog );

        if ( is_node_element( elementRoot ) ) {
            Node attributeRootID = elementRoot.getAttributes().getNamedItem("root_name");

            if ( is_node_attribute( attributeRootID ) ) {

                control_graph.root_name = elementRoot.getAttributes().getNamedItem("root_name").getNodeValue();


                int node_probability_amount = elementBase.getElementsByTagName("node_probability").getLength();

                for (int node_probability_index = 0; node_probability_index < node_probability_amount; node_probability_index++) {

                    Node probabilityNode = elementBase.getElementsByTagName("node_probability").item(node_probability_index);

                    if ( is_node_element( probabilityNode ) ) {
                        Node attributeID            = probabilityNode.getAttributes().getNamedItem("node_name");
                        Node attributeStartPolicy   = probabilityNode.getAttributes().getNamedItem("start_policy");
                        Node attributeWaitTime      = probabilityNode.getAttributes().getNamedItem("wait_time");

                        if ( is_node_attribute( attributeID          ) &&
                             is_node_attribute( attributeStartPolicy ) ){

                            ControlNodeProbability node_probability = new ControlNodeProbability();

                            node_probability.set_concurrent_process_id( this.concurrent_module.allocate_concurrent_process_id() );

                            node_probability.set_node_name( attributeID.getNodeValue() );
                            node_probability.set_start_policy( ControlNodeInterface.start_policy_enum_from_string( attributeStartPolicy.getNodeValue() ) );

                            if ( is_node_attribute( attributeWaitTime ) ){
                                node_probability.wait_time = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int( attributeWaitTime.getNodeValue() );
                            }

                            int probability_child_amount = ((Element)probabilityNode).getElementsByTagName("probability_for_successor_node").getLength();

                            for ( int probability_child_index = 0 ; probability_child_index < probability_child_amount ; probability_child_index++ )
                            {
                                Node elementProbabilityForSuccessorNode = ((Element)probabilityNode).getElementsByTagName("probability_for_successor_node").item(probability_child_index);

                                if ( is_node_element( elementProbabilityForSuccessorNode ) )
                                {
                                    Node attributeProbabilitySuccessor  = elementProbabilityForSuccessorNode.getAttributes().getNamedItem("probability");
                                    Node attributeSuccessorID           = elementProbabilityForSuccessorNode.getAttributes().getNamedItem("node_name");


                                    ControlProbabilityForSuccessor probability_for_successor_node = new ControlProbabilityForSuccessor();

                                    if ( is_node_attribute( attributeProbabilitySuccessor ) &&
                                         is_node_attribute( attributeSuccessorID          ) ) {

                                        probability_for_successor_node.probability  = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int( attributeProbabilitySuccessor.getNodeValue() );
                                        probability_for_successor_node.node_name = attributeSuccessorID.getNodeValue();

                                        NodeList nodeListUpdate     = ((Element)elementProbabilityForSuccessorNode).getElementsByTagName("update");
                                        int element_update_amount   = nodeListUpdate.getLength();

                                        for (int update_index = 0; update_index < element_update_amount ; update_index++ ) {
                                            Node elementUpdate = nodeListUpdate.item(update_index);

                                            if ( is_node_element( elementUpdate ) ) {
                                                List<Condition> new_conditions = PLPLoader.parseConditions( (Element)elementUpdate );
                                                if ( ( null != new_conditions     ) &&
                                                     ( 1 == new_conditions.size() )  ) {
                                                    probability_for_successor_node.updates.add( new_conditions.get(0) );
                                                }
                                            }
                                        }

                                        node_probability.probability_for_successor_nodes.add(probability_for_successor_node);
                                    }
                                    else
                                    {
                                        throw new VerificationException("control_graph, node_probability, probability_for_successor_node missing probability or node_name.");
                                    }
                                }
                            }


                            control_graph.control_nodes.add( node_probability );
                        }
                        else
                        {
                            throw new VerificationException("control_graph, node_probability missing node_name or start_policy.");
                        }
                    }
                }


                int node_concurrent_amount = elementBase.getElementsByTagName("node_concurrent").getLength();

                for (int node_concurrent_index = 0; node_concurrent_index < node_concurrent_amount; node_concurrent_index++) {

                    Node concurrentNode = elementBase.getElementsByTagName("node_concurrent").item(node_concurrent_index);

                    if ( is_node_element( concurrentNode ) ) {
                        Node attributeID            = concurrentNode.getAttributes().getNamedItem("node_name");
                        Node attributeStartPolicy   = concurrentNode.getAttributes().getNamedItem("start_policy");
                        Node attributeWaitTime      = concurrentNode.getAttributes().getNamedItem("wait_time");

                        if ( is_node_attribute( attributeID          ) &&
                             is_node_attribute( attributeStartPolicy ) ){

                            ControlNodeConcurrent node_concurrent = new ControlNodeConcurrent();

                            node_concurrent.set_concurrent_process_id( this.concurrent_module.allocate_concurrent_process_id() );

                            node_concurrent.set_node_name( attributeID.getNodeValue() );
                            node_concurrent.set_start_policy( ControlNodeInterface.start_policy_enum_from_string( attributeStartPolicy.getNodeValue() ) );

                            if ( is_node_attribute( attributeWaitTime ) ){
                                node_concurrent.wait_time = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int( attributeWaitTime.getNodeValue() );
                            }

                            NodeList nodeListUpdate     = ((Element)concurrentNode).getElementsByTagName("update");
                            int element_update_amount   = nodeListUpdate.getLength();

                            for (int update_index = 0; update_index < element_update_amount ; update_index++ ) {
                                Node elementUpdate = nodeListUpdate.item(update_index);

                                if ( is_node_element( elementUpdate ) ) {
                                    List<Condition> new_conditions = PLPLoader.parseConditions( (Element)elementUpdate );
                                    if ( ( null != new_conditions     ) &&
                                            ( 1 == new_conditions.size() )  ) {
                                        node_concurrent.updates.add( new_conditions.get(0) );
                                    }
                                }
                            }

                            NodeList nodeListRunNodes   = ((Element)concurrentNode).getElementsByTagName("run_node");
                            int element_run_node_amount   = nodeListRunNodes.getLength();

                            for (int run_bode_index = 0; run_bode_index < element_run_node_amount ; run_bode_index++ ) {
                                Node elementRunNode = nodeListRunNodes.item(run_bode_index);

                                if ( is_node_element( elementRunNode )  ) {
                                    Node attributeNodeID    = elementRunNode.getAttributes().getNamedItem("node_name");

                                    if ( is_node_attribute( attributeNodeID )  ) {
                                        node_concurrent.run_nodes.add( attributeNodeID.getNodeValue() );
                                    }
                                }
                            }

                            control_graph.control_nodes.add( node_concurrent );
                        }
                    }
                    else
                    {
                        throw new VerificationException("control_graph, node_concurrent missing node_name or start_policy.");
                    }
                }


                int node_sequential_amount = elementBase.getElementsByTagName("node_sequential").getLength();

                for (int node_sequential_index = 0; node_sequential_index < node_sequential_amount; node_sequential_index++) {

                    Node sequentialNode = elementBase.getElementsByTagName("node_sequential").item(node_sequential_index);

                    if ( is_node_element( sequentialNode ) ) {
                        Node attributeID            = sequentialNode.getAttributes().getNamedItem("node_name");
                        Node attributeStartPolicy   = sequentialNode.getAttributes().getNamedItem("start_policy");

                        Node attributeNextNodeID    = sequentialNode.getAttributes().getNamedItem("next_node_name");

                        if ( is_node_attribute( attributeID          ) &&
                             is_node_attribute( attributeStartPolicy ) ){

                            ControlNodeSequential node_sequential = new ControlNodeSequential();

                            node_sequential.set_concurrent_process_id( this.concurrent_module.allocate_concurrent_process_id() );

                            node_sequential.set_node_name( attributeID.getNodeValue() );
                            node_sequential.set_start_policy( ControlNodeInterface.start_policy_enum_from_string( attributeStartPolicy.getNodeValue() ) );

                            if ( ( is_node_attribute( attributeNextNodeID )              ) &&
                                 ( false == attributeNextNodeID.getNodeValue().isEmpty() ) )
                            {
                                node_sequential.next_node_name = attributeNextNodeID.getNodeValue();
                            }

                            NodeList nodeListRunPLP = ((Element)sequentialNode).getElementsByTagName("run_plp");
                            int run_plp_amount = nodeListRunPLP.getLength();

                            for ( int run_plp_index = 0 ; run_plp_index < run_plp_amount ; run_plp_index++ )
                            {
                                Node elementRunPLP = nodeListRunPLP.item(run_plp_index);

                                if ( is_node_element( elementRunPLP ) )
                                {
                                    Node attributePLPName   = elementRunPLP.getAttributes().getNamedItem("plp_name");
                                    Node attributeWaitTime  = elementRunPLP.getAttributes().getNamedItem("wait_time");

                                    ControlUpdateForPLP update_for_plp = new ControlUpdateForPLP();

                                    if ( is_node_attribute( attributeWaitTime   ) )
                                    {
                                        update_for_plp.wait_time  = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int( attributeWaitTime.getNodeValue() );
                                    }

                                    if ( is_node_attribute( attributePLPName    )  )
                                    {

                                        update_for_plp.plp_name   = attributePLPName.getNodeValue();

                                        NodeList nodeListUpdate     = ((Element)elementRunPLP).getElementsByTagName("update");
                                        int element_update_amount   = nodeListUpdate.getLength();

                                        for (int update_index = 0; update_index < element_update_amount ; update_index++ ) {
                                            Node elementUpdate = nodeListUpdate.item(update_index);

                                            if ( is_node_element( elementUpdate ) ) {
                                                List<Condition> new_conditions = PLPLoader.parseConditions( (Element)elementUpdate );
                                                if ( ( null != new_conditions     ) &&
                                                        ( 1 == new_conditions.size() )  ) {
                                                    update_for_plp.updates.add( new_conditions.get(0) );
                                                }
                                            }
                                        }

                                        node_sequential.update_for_plp.add( update_for_plp );
                                    }
                                    else
                                    {
                                        throw new VerificationException("control_graph, node_sequential missing plp_name.");
                                    }
                                }
                            }


                            control_graph.control_nodes.add( node_sequential );
                        }
                        else
                        {
                            throw new VerificationException("control_graph, node_sequential missing node_name or start_policy.");
                        }
                    }
                }



                int node_condition_amount = elementBase.getElementsByTagName("node_condition").getLength();

                for (int node_condition_index = 0; node_condition_index < node_condition_amount; node_condition_index++) {

                    Node conditionNode = elementBase.getElementsByTagName("node_condition").item(node_condition_index);


                    if ( is_node_element( conditionNode ) ) {
                        Node attributeID            = conditionNode.getAttributes().getNamedItem("node_name");
                        Node attributeStartPolicy   = conditionNode.getAttributes().getNamedItem("start_policy");
                        Node attributeWaitTime      = conditionNode.getAttributes().getNamedItem("wait_time");

                        if ( is_node_attribute( attributeID          ) &&
                             is_node_attribute( attributeStartPolicy ) ){

                            ControlNodeCondition node_condition = new ControlNodeCondition();

                            node_condition.set_concurrent_process_id( this.concurrent_module.allocate_concurrent_process_id() );

                            node_condition.set_node_name( attributeID.getNodeValue() );
                            node_condition.set_start_policy( ControlNodeInterface.start_policy_enum_from_string( attributeStartPolicy.getNodeValue() ) );

                            if ( is_node_attribute( attributeWaitTime ) ){
                                node_condition.wait_time = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int( attributeWaitTime.getNodeValue() );
                            }

                            int run_node_amount = ((Element)conditionNode).getElementsByTagName("run_node").getLength();

                            for ( int run_node_index = 0 ; run_node_index < run_node_amount ; run_node_index++ )
                            {
                                Node elementRunNode = ((Element)conditionNode).getElementsByTagName("run_node").item(run_node_index);

                                if ( is_node_element( elementRunNode ) )
                                {
                                    Node attributeSuccessorID           = elementRunNode.getAttributes().getNamedItem("node_name");


                                    ControlConditionOfSuccessor condition_of_successor_node = new ControlConditionOfSuccessor();

                                    if ( is_node_attribute( attributeSuccessorID ) ) {

                                        condition_of_successor_node.node_name = attributeSuccessorID.getNodeValue();

                                        NodeList nodeListPrecondition = ((Element)elementRunNode).getElementsByTagName("preconditions");
                                        if ( ( 1 == nodeListPrecondition.getLength()                ) &&
                                             ( is_node_element( nodeListPrecondition.item(0) ) ) )
                                        {
                                            condition_of_successor_node.condition = PLPLoader.parseConditions( (Element)nodeListPrecondition.item(0) );

                                            NodeList nodeListUpdate         = ((Element)elementRunNode).getElementsByTagName("update");
                                            int element_update_amount       = nodeListUpdate.getLength();

                                            for (int update_index = 0; update_index < element_update_amount ; update_index++ ) {
                                                Node elementUpdate = nodeListUpdate.item(update_index);

                                                if ( is_node_element( elementUpdate ) ) {
                                                    List<Condition> new_conditions = PLPLoader.parseConditions( (Element)elementUpdate );
                                                    if ( ( null != new_conditions     ) &&
                                                            ( 1 == new_conditions.size() )  ) {
                                                        condition_of_successor_node.updates.add( new_conditions.get(0) );
                                                    }
                                                }
                                            }

                                            node_condition.condition_of_successor_nodes.add(condition_of_successor_node);

                                        }


                                    }
                                }
                            }


                            control_graph.control_nodes.add( node_condition );
                        }
                    }
                    else
                    {
                        throw new VerificationException("control_graph, node_condition missing node_name or start_policy.");
                    }
                }

            }
            else
            {
                throw new VerificationException("control_graph missing root_name.");
            }
        }

        control_graph.finalize_when_all_data_exist();

        return control_graph;
    }



    public void concurrent_modules_initialize()
    {
        int plps_amount = this.plp_catalog.get_plps_amount();
        int reserved_variable_id_first = this.variable_manager.concurrent_modules_reserve_variables( plps_amount );

        int reserved_variable_id_current = reserved_variable_id_first;
        for ( int module_index = 0 ; module_index < plps_amount ; module_index++ )
        {
            VerificationVariable variable_for_module = new VerificationVariable();

            variable_for_module.variable_type   = VerificationVariable.VerificationVariableType.type_concurrent_module;
            variable_for_module.value_type      = VerificationVariable.VerificationValueType.value_boolean;
            variable_for_module.value           = 0;
            variable_for_module.is_in_range     = true;
            variable_for_module.min_value       = 0;
            variable_for_module.max_value       = 1;
            variable_for_module.is_set          = true;

            this.variable_manager.global_variable_concurrent_module_add( reserved_variable_id_current,
                    this.variable_manager.concurrent_module_variable_name( this.plp_catalog.find_plp_name_by_id( module_index )),
                    variable_for_module );

            reserved_variable_id_current++;
        }

    }

    private void initialize_repeat_variable_for_plp( int plp_id )
    {
        String uppaal_variable_repeat = this.generator.get_plp_repeat_variable( plp_id );

        this.variable_manager.global_variable_add( uppaal_variable_repeat,
                new VerificationVariable(
                        VerificationVariable.VerificationVariableType.type_regular,
                        VerificationVariable.VerificationValueType.value_boolean,
                        0,
                        true,
                        true,
                        0,
                        1));

        this.reports.add_info("PLP \"" + this.plp_catalog.find_plp_name_by_id(plp_id) + "\" uses repeat variable: \"" + uppaal_variable_repeat + "\".");
    }

    public void create_system() throws VerificationException
    {

        System.out.println("****************************************** Generation Start ****************************************");

        uppaal_file_initialize();

        Element configuration_root = file_open( this.configuration_file);
        configuration_file_load_settings( configuration_root );

        if (null != this.all_plps) {
            for (PLP current_plp : this.all_plps) {
                int plp_id = this.plp_catalog.plp_map_add( current_plp.getBaseName() );

                List<Variable> plp_variables = current_plp.getVariables();

                if (null != plp_variables) {
                    for (Variable current_variable : plp_variables) {

                        if (false == this.variable_manager.global_variable_is_exist(current_variable.getName())) {
                            VerificationVariable variable_data = new VerificationVariable();
                            variable_data.variable_type = VerificationVariable.VerificationVariableType.type_regular;
                            variable_data.is_set = false;

                            if (FieldType.Boolean == current_variable.getType()) {
                                variable_data.value_type = VerificationVariable.VerificationValueType.value_boolean;
                            } else if (FieldType.Integer == current_variable.getType()) {
                                variable_data.value_type = VerificationVariable.VerificationValueType.value_integer;
                            } else if (FieldType.Real == current_variable.getType()) {
                                variable_data.value_type = VerificationVariable.VerificationValueType.value_real;
                            }

                            List<Range> ranges = current_variable.getPossibleRanges();
                            if ( ( null != ranges            ) &&
                                 ( false == ranges.isEmpty() ) ) {
                                final int FIRST_INDEX = 0;
                                Range first_range = ranges.get(FIRST_INDEX);

                                variable_data.is_set        = false;
                                variable_data.is_in_range   = true;

                                int min_range               = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int( first_range.getMinValue() );
                                if ( false == first_range.isMinInclusive() )
                                {
                                    min_range++;
                                }
                                variable_data.min_value = min_range;

                                int max_range               = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int( first_range.getMaxValue() );
                                if ( false == first_range.isMaxInclusive() )
                                {
                                    max_range--;
                                }
                                variable_data.max_value = max_range;
                            }

                            this.variable_manager.global_variable_add(current_variable.getName(), variable_data);
                        }

                    }
                }

                List<Constant> plp_constants = current_plp.getConstants();
                if (null != plp_constants) {
                    for (Constant current_constant : plp_constants) {
                        if ( false == this.variable_manager.global_variable_is_exist( current_constant.getName() ) ) {
                            VerificationVariable variable_data = new VerificationVariable();
                            variable_data.variable_type = VerificationVariable.VerificationVariableType.type_constant;
                            variable_data.is_set = false;

                            if (FieldType.Boolean == current_constant.getType()) {
                                variable_data.value_type    = VerificationVariable.VerificationValueType.value_boolean;
                            } else if (FieldType.Integer == current_constant.getType()) {
                                variable_data.value_type = VerificationVariable.VerificationValueType.value_integer;
                            } else if (FieldType.Real == current_constant.getType()) {
                                variable_data.value_type = VerificationVariable.VerificationValueType.value_real;
                            }

                            String constant_value = current_constant.getValue();
                            if ( ( null != constant_value            ) &&
                                 ( false == constant_value.isEmpty() ) ) {
                                variable_data.is_set        = true;
                                variable_data.value         = this.xml_to_uppaal_converter.convert_xml_value_to_uppaal_int( constant_value );
                                variable_data.is_in_range   = true;
                                variable_data.min_value     = variable_data.value;
                                variable_data.max_value     = variable_data.value;
                            }

                            this.variable_manager.global_variable_add(current_constant.getName(), variable_data);
                        }

                    }
                }

                try {
                    this.variable_manager.local_parameters_init( plp_id );

                    this.xml_to_uppaal_converter.parameters_add( plp_id, current_plp.getInputParams(),           VerificationParameter.VerificationParameterType.type_input );
                    this.xml_to_uppaal_converter.parameters_add( plp_id, current_plp.getOutputParams(),          VerificationParameter.VerificationParameterType.type_output );
                    this.xml_to_uppaal_converter.parameters_add( plp_id, current_plp.getExecParams(),            VerificationParameter.VerificationParameterType.type_execution );
                    this.xml_to_uppaal_converter.parameters_add( plp_id, current_plp.getUnobservableParams(),    VerificationParameter.VerificationParameterType.type_unobservable );
                } catch (VerificationException exception) {
                    throw new VerificationException("problem with addition of parameters; " + exception.get_message());
                }

                List<RequiredResource> plp_resources = current_plp.getRequiredResources();
                if (null != plp_resources) {
                    for (RequiredResource current_resource : plp_resources) {
                        if (false == this.variable_manager.global_variable_is_exist(current_resource.getName())) {
                            VerificationVariable variable_data = new VerificationVariable();
                            variable_data.variable_type = VerificationVariable.VerificationVariableType.type_resource;
                            variable_data.is_set = false;
                            variable_data.value_type = VerificationVariable.VerificationValueType.value_real;


                            double resource_value = current_resource.getQuantity();
                            if (resource_value != this.variable_manager.RESOURCE_UNDEFINED) {
                                variable_data.is_set = true;
                                variable_data.value = this.xml_to_uppaal_converter.convert_xml_double_to_uppaal_int( resource_value );
                            }

                            this.variable_manager.global_variable_add(current_resource.getName(), variable_data);
                        }
                    }
                }


                try {

                    this.xml_to_uppaal_converter.recursive_add_all_predicates(current_plp.getPreConditions());
                    this.xml_to_uppaal_converter.recursive_add_all_predicates(current_plp.getConcurrencyConditions());

                    if (AchievePLP.class.isInstance(current_plp)) {
                        AchievePLP achieve_plp = (AchievePLP) current_plp;

                        initialize_repeat_variable_for_plp( plp_id );

                        this.xml_to_uppaal_converter.recursive_add_all_predicates(achieve_plp.getGoal());
                        this.xml_to_uppaal_converter.recursive_add_all_predicates(achieve_plp.getSuccessTerminationCond());
                        this.xml_to_uppaal_converter.recursive_add_all_predicates(achieve_plp.getFailTerminationCond());

                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.probs_to_conditions( achieve_plp.getSuccessProb() ) );
                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.probs_to_conditions( achieve_plp.getGeneralFailureProb() ) );

                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.dists_to_conditions( achieve_plp.getSuccessRuntime() ) );
                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.dists_to_conditions( achieve_plp.getFailRuntime() ) );

                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.failures_to_conditions( achieve_plp.getFailureModes() ) );
                        /*
                        private Condition goal;
                        private Condition successTerminationCond;
                        private Condition failTerminationCond;

                        private List<ConditionalProb> successProb;
                        private List<ConditionalProb> generalFailureProb;

                        private List<ConditionalDist> successRuntime;
                        private List<ConditionalDist> failRuntime;

                        private List<FailureMode> failureModes;
                        */

                    } else if (MaintainPLP.class.isInstance(current_plp)) {
                        MaintainPLP maintain_plp = (MaintainPLP) current_plp;

                        this.xml_to_uppaal_converter.recursive_add_all_predicates(maintain_plp.getMaintainedCondition());
                        this.xml_to_uppaal_converter.recursive_add_all_predicates(maintain_plp.getSuccessTerminationCondition());
                        this.xml_to_uppaal_converter.recursive_add_all_predicates(maintain_plp.getFailureTerminationConditions());

                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.probs_to_conditions( maintain_plp.getSuccessProb() ) );
                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.probs_to_conditions( maintain_plp.getGeneralFailureProb() ) );

                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.dists_to_conditions( maintain_plp.getTimeUntilTrue() ) );
                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.dists_to_conditions( maintain_plp.getSuccessRuntime() ) );
                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.dists_to_conditions( maintain_plp.getFailRuntime() ) );

                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.failures_to_conditions( maintain_plp.getFailureModes() ) );

                        /*
                        private Condition maintainedCondition;
                        private Condition successTerminationCondition;
                        private List<Condition> failureTerminationConditions;

                        private List<ConditionalProb> successProb;
                        private List<ConditionalProb> generalFailureProb;

                        private List<ConditionalDist> timeUntilTrue;
                        private List<ConditionalDist> successRuntime;
                        private List<ConditionalDist> failRuntime;

                        private List<FailureMode> failureModes;
                        */
                    } else if (ObservePLP.class.isInstance(current_plp)) {
                        ObservePLP observe_plp = (ObservePLP) current_plp;

                        initialize_repeat_variable_for_plp( plp_id );

                        this.xml_to_uppaal_converter.recursive_add_all_predicates(observe_plp.getFailTerminationCond());

                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.probs_to_conditions( observe_plp.getFailureToObserveProb() ) );
                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.probs_to_conditions( observe_plp.getCorrectObservationProb() ) );

                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.dists_to_conditions( observe_plp.getFailureRuntime() ) );
                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.dists_to_conditions( observe_plp.getSuccessRuntime() ) );


                    } else if (DetectPLP.class.isInstance(current_plp)) {
                        DetectPLP detect_plp = (DetectPLP) current_plp;

                        this.xml_to_uppaal_converter.recursive_add_all_predicates(detect_plp.getGoal());
                        this.xml_to_uppaal_converter.recursive_add_all_predicates(detect_plp.getFailTerminationCond());

                        this.xml_to_uppaal_converter.recursive_add_all_predicates( this.xml_to_uppaal_converter.probs_to_conditions( detect_plp.getSuccessProbGivenCondition() ) );
                        /*
                        private Condition goal;
                        private Condition failTerminationCond;

                        private List<ConditionalProb> successProbGivenCondition;
                        */
                    }
                } catch (VerificationException exception) {
                    throw new VerificationException("problem with addition of predicates; " + exception.get_message());
                }

            }
        }

        if ( 0 < this.plp_catalog.get_plps_amount() )
        {
            concurrent_modules_initialize();

            this.variable_manager.print_variables();
            configuration_file_load_variables( configuration_root );


            if ( null != this.all_plps ) {
                for (PLP current_plp : this.all_plps) {
                    this.generator.uppaal_file_add_plp(current_plp);
                }
            }
            this.concurrent_module.allocate_concurrent_process_id( this.plp_catalog.get_plps_amount() );

            Element control_graph_root = file_open( this.control_graph_file );
            ControlGraph control_graph = file_load_control_graph( control_graph_root );
            this.generator.add_control_graph( control_graph );

            this.variable_manager.print_variables();

            this.concurrent_module.generate_concurrent_module_update( this.generator.verification_modules_end );
            this.generator.generate_processes_update();
            this.generator.generate_variables_update( this.concurrent_module.get_concurrent_processes_amount() );
            this.generator.uppaal_file_store( this.output_uppaal_file );
        }
        else
        {
            reports.add_warning("There are no PLPs given.");
        }

        System.out.println("****************************************** Generation Done *****************************************");
        reports.print_info();
        reports.print_warnings();

    }


}
