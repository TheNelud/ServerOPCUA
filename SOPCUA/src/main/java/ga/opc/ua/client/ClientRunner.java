package ga.opc.ua.client;

import ga.opc.ua.distributor.Distributor;
import ga.opc.ua.distributor.models.ClientConfig;
import ga.opc.ua.distributor.models.Config;
import ga.opc.ua.server.ExampleServer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.client.security.DefaultClientCertificateValidator;
import org.eclipse.milo.opcua.stack.core.Stack;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.Unsigned.uint;

public class ClientRunner implements Runnable{

    static {
        // Required for SecurityPolicy.Aes256_Sha256_RsaPss
        Security.addProvider(new BouncyCastleProvider());
    }

    private final Distributor distributor = new Distributor();
    private final String ip = distributor.getIp();
    private final String port = distributor.getPort();


    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final CompletableFuture<OpcUaClient> future = new CompletableFuture<>();

    private ExampleServer exampleServer;

    private DefaultTrustListManager trustListManager;

    private final ClientWriter clientWriter;
    private final boolean serverRequired;

    public ClientRunner(ClientWriter clientWriter, boolean serverRequired) throws Exception {
        this.clientWriter = clientWriter;
        this.serverRequired = serverRequired;

        if (serverRequired){
            exampleServer = new ExampleServer();
            exampleServer.startup().get();
        }
    }

    public ClientRunner(ClientWriter clientWriter, boolean serverRequired, String ip, String port) throws Exception {
        this.clientWriter = clientWriter;
        this.serverRequired = serverRequired;
        if (serverRequired) {
            exampleServer = new ExampleServer();
            exampleServer.startup().get();
        }


    }

    private OpcUaClient createClient() throws Exception {
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "client", "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security dir: " + securityTempDir);
        }

        File pkiDir = securityTempDir.resolve("pki").toFile();

        LoggerFactory.getLogger(getClass())
                .info("security dir: {}", securityTempDir.toAbsolutePath());
        LoggerFactory.getLogger(getClass())
                .info("security pki dir: {}", pkiDir.getAbsolutePath());

        trustListManager = new DefaultTrustListManager(pkiDir);

        DefaultClientCertificateValidator certificateValidator =
                new DefaultClientCertificateValidator(trustListManager);

        return OpcUaClient.create(
                clientWriter.getEndpointUrl(ip, port),
                endpoints ->
                        endpoints.stream()
                                .findFirst(),
                configBuilder ->
                        configBuilder
                                .setApplicationName(LocalizedText.english("eclipse milo opc-ua client"))
                                .setApplicationUri("urn:eclipse:milo:examples:client")
                                .setCertificateValidator(certificateValidator)
                                .setIdentityProvider(clientWriter.getIdentityProvider())
                                .setRequestTimeout(uint(5000))
                                .build()
        );
    }
    @Override
    public void run(){
        try{
            OpcUaClient client = createClient();
            client.getConfig().getCertificate().ifPresent(
                    certificate ->
                            exampleServer.getServer().getConfig().getTrustListManager().addTrustedCertificate(certificate)
            );

            exampleServer.getServer().getConfig().getCertificateManager().getCertificates().forEach(
                    certificate ->
                            trustListManager.addTrustedCertificate(certificate)
            );

            future.whenCompleteAsync((c, ex) -> {
                if (ex != null) {
                    logger.error("Error running example: {}", ex.getMessage(), ex);
                }

                try {
                    client.disconnect().get();
                    if (serverRequired && exampleServer != null) {
                        exampleServer.shutdown().get();
                    }
                    Stack.releaseSharedResources();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("Error disconnecting: {}", e.getMessage(), e);
                }

                try {
                    Thread.sleep(1000);
                    System.exit(0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            });

            try {
                clientWriter.run(client, future);
                future.get(15, TimeUnit.DAYS);
            } catch (Throwable t) {
                logger.error("Error running client example: {}", t.getMessage(), t);
                future.completeExceptionally(t);
            }


        }catch (Throwable t){
            logger.error("Error getting client: {}", t.getMessage(), t);

            future.completeExceptionally(t);

            try {
                Thread.sleep(1000);
                System.exit(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            Thread.sleep(999_999_999);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
