package ga.opc.ua.client;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.api.identity.AnonymousProvider;
import org.eclipse.milo.opcua.sdk.client.api.identity.IdentityProvider;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.types.structured.EndpointDescription;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

public interface ClientWriter {

    default String getEndpointUrl() {
        return "opc.tcp://localhost:12686/milo";
    }
    default String getEndpointUrl(String ip, String port) {
        return "opc.tcp://"+ip+":"+port+"/";
    }



//    default SecurityPolicy getSecurityPolicy() {
//        return SecurityPolicy.None;
//    }

    default IdentityProvider getIdentityProvider() {
        return new AnonymousProvider();
    }

    void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) throws Exception;
}
