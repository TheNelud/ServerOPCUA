
package ga.opc.ua.server;

import ga.opc.ua.distributor.Seeker;
import ga.opc.ua.server.methods.GenerateEventMethod;
import ga.opc.ua.server.methods.SqrtMethod;
import ga.opc.ua.server.types.CustomEnumType;
import ga.opc.ua.server.types.CustomStructType;
import ga.opc.ua.server.types.CustomUnionType;
import org.eclipse.milo.opcua.sdk.core.AccessLevel;
import org.eclipse.milo.opcua.sdk.core.Reference;
import org.eclipse.milo.opcua.sdk.core.ValueRank;
import org.eclipse.milo.opcua.sdk.core.ValueRanks;
import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.DataItem;
import org.eclipse.milo.opcua.sdk.server.api.ManagedNamespaceWithLifecycle;
import org.eclipse.milo.opcua.sdk.server.api.MonitoredItem;
import org.eclipse.milo.opcua.sdk.server.dtd.DataTypeDictionaryManager;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.BaseEventTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.objects.ServerTypeNode;
import org.eclipse.milo.opcua.sdk.server.model.nodes.variables.AnalogItemTypeNode;
import org.eclipse.milo.opcua.sdk.server.nodes.*;
import org.eclipse.milo.opcua.sdk.server.nodes.factories.NodeFactory;
import org.eclipse.milo.opcua.sdk.server.nodes.filters.AttributeFilters;
import org.eclipse.milo.opcua.sdk.server.util.SubscriptionModel;
import org.eclipse.milo.opcua.stack.core.AttributeId;
import org.eclipse.milo.opcua.stack.core.BuiltinDataType;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UInteger;
import org.eclipse.milo.opcua.stack.core.types.enumerated.StructureType;
import org.eclipse.milo.opcua.stack.core.types.structured.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;


import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.*;

public class ExampleNamespace extends ManagedNamespaceWithLifecycle  {

    public static final String NAMESPACE_URI = "gazprom:automation:hello-world";
    public static Object[][] STATIC_SCALAR_NODES_FROM_FILE;


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private volatile Thread eventThread;
    private volatile boolean keepPostingEvents = true;

    private final Random random = new Random();

    private final DataTypeDictionaryManager dictionaryManager;

    private final SubscriptionModel subscriptionModel;

    ExampleNamespace(OpcUaServer server) {
        super(server, NAMESPACE_URI);


        subscriptionModel = new SubscriptionModel(server, this);
        dictionaryManager = new DataTypeDictionaryManager(getNodeContext(), NAMESPACE_URI);

        getLifecycleManager().addLifecycle(dictionaryManager);
        getLifecycleManager().addLifecycle(subscriptionModel);

        getLifecycleManager().addStartupTask(this::createAndAddNodes);
    }

    private void createAndAddNodes() {
        // Create a "TagS" folder and add it to the node manager
        NodeId folderNodeId = newNodeId("TagS");

        UaFolderNode folderNode = new UaFolderNode(
                getNodeContext(),
                folderNodeId,
                newQualifiedName("TagS"),
                LocalizedText.english("TagS")
        );

        getNodeManager().addNode(folderNode);

        // Make sure our new folder shows up under the server's Objects folder.
        folderNode.addReference(new Reference(
                folderNode.getNodeId(),
                Identifiers.Organizes,
                Identifiers.ObjectsFolder.expanded(),
                false
        ));

        // Add the rest of the nodes
        addVariableNodes(folderNode);

    }

    private void addVariableNodes(UaFolderNode rootNode) {
        addScalarNodes(rootNode);
    }

    //TODO asdasd
    private void addScalarNodes(UaFolderNode rootNode) {
        UaFolderNode scalarTypesFolder = new UaFolderNode(
                getNodeContext(),
                newNodeId("TagS/ScalarTypes"),
                newQualifiedName("ScalarTypes"),
                LocalizedText.english("ScalarTypes")
        );

        getNodeManager().addNode(scalarTypesFolder);
        rootNode.addOrganizes(scalarTypesFolder);

        File currentDirFile = new File(".");
        String helper = currentDirFile.getAbsolutePath();
        Seeker seeker = new Seeker();

        List<String> list = null;
        try {
            list = Files.readAllLines(Paths.get(seeker.runToDirectory("Namespace",helper).get(0)));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        STATIC_SCALAR_NODES_FROM_FILE = new Object[list.size()][3];
        int i=0;
        for (String str :list){
            String[] preInd = str.split(",");
            if (preInd.length==3){
                STATIC_SCALAR_NODES_FROM_FILE[i][0]=preInd[0];
                STATIC_SCALAR_NODES_FROM_FILE[i][1]= Identifiers.Double;
                STATIC_SCALAR_NODES_FROM_FILE[i][2]=new Variant(preInd[2]);

            } else
            if (preInd.length==4){
                STATIC_SCALAR_NODES_FROM_FILE[i][0]=preInd[0];
                STATIC_SCALAR_NODES_FROM_FILE[i][1]= Identifiers.Int16;
                STATIC_SCALAR_NODES_FROM_FILE[i][2]=new Variant(preInd[3]);
            }
            i++;

        }
        for (Object[] os : STATIC_SCALAR_NODES_FROM_FILE) {
            String name = (String) os[0];
            NodeId typeId = (NodeId) os[1];
            try {
                Variant variant = (Variant) os[2];

                UaVariableNode node = new UaVariableNode.UaVariableNodeBuilder(getNodeContext())
                        .setNodeId(newNodeId("TagS/ScalarTypes/" + name))
                        .setAccessLevel(AccessLevel.READ_WRITE)
                        .setUserAccessLevel(AccessLevel.READ_WRITE)
                        .setBrowseName(newQualifiedName(name))
                        .setDisplayName(LocalizedText.english(name))
                        .setDataType(typeId)
                        .setTypeDefinition(Identifiers.BaseDataVariableType)
                        .build();

                node.setValue(new DataValue(variant));

                node.getFilterChain().addLast(new AttributeLoggingFilter(AttributeId.Value::equals));

                getNodeManager().addNode(node);
                scalarTypesFolder.addOrganizes(node);
            }
            catch (Exception e){
                logger.info("error value'{}' to nodeId={}", os[2], name);
//                LOGGER.info(marker_inp,"error value'{}' to nodeId={} \n Tag not wrote", os[2], name);
                continue;
            }

        }
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    @Override
    public void onDataItemsCreated(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsCreated(dataItems);
    }

    @Override
    public void onDataItemsModified(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsModified(dataItems);
    }

    @Override
    public void onDataItemsDeleted(List<DataItem> dataItems) {
        subscriptionModel.onDataItemsDeleted(dataItems);
    }

    @Override
    public void onMonitoringModeChanged(List<MonitoredItem> monitoredItems) {
        subscriptionModel.onMonitoringModeChanged(monitoredItems);
    }

}

