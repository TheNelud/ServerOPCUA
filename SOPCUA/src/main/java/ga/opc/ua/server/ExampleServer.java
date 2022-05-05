package ga.opc.ua.server;

import ga.opc.ua.client.ClientRunner;
import ga.opc.ua.client.WriteFromText;

import ga.opc.ua.distributor.Distributor;
import ga.opc.ua.distributor.models.ClientConfig;
import ga.opc.ua.distributor.models.Config;
import ga.opc.ua.distributor.models.ServerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import org.eclipse.milo.opcua.sdk.server.OpcUaServer;
import org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig;
import org.eclipse.milo.opcua.sdk.server.identity.CompositeValidator;
import org.eclipse.milo.opcua.sdk.server.identity.UsernameIdentityValidator;
import org.eclipse.milo.opcua.sdk.server.identity.X509IdentityValidator;
import org.eclipse.milo.opcua.sdk.server.util.HostnameUtil;
import org.eclipse.milo.opcua.stack.core.StatusCodes;
import org.eclipse.milo.opcua.stack.core.UaRuntimeException;
import org.eclipse.milo.opcua.stack.core.security.DefaultCertificateManager;
import org.eclipse.milo.opcua.stack.core.security.DefaultTrustListManager;
import org.eclipse.milo.opcua.stack.core.security.SecurityPolicy;
import org.eclipse.milo.opcua.stack.core.transport.TransportProfile;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.enumerated.MessageSecurityMode;
import org.eclipse.milo.opcua.stack.core.types.structured.BuildInfo;
import org.eclipse.milo.opcua.stack.core.util.CertificateUtil;
import org.eclipse.milo.opcua.stack.core.util.NonceUtil;
import org.eclipse.milo.opcua.stack.server.EndpointConfiguration;
import org.eclipse.milo.opcua.stack.server.security.DefaultServerCertificateValidator;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.eclipse.milo.opcua.sdk.server.api.config.OpcUaServerConfig.*;

public class ExampleServer {

    private static final Distributor distributor = new Distributor();
    private static final String ip = distributor.getIp();
    private static final String port = distributor.getPort();
//    public static final String directory_with_files = distributor.getPath();
//    private static final int query_time = distributor.getQuery();

//    private static final int TCP_BIND_PORT = 12686;
//    private static final int HTTPS_BIND_PORT = 8443;

    public static final Logger LOGGER = LogManager.getLogger(ExampleServer.class);
    public static final Marker marker_err = MarkerManager.getMarker("Criticals_server");
    public static final Marker marker_inp = MarkerManager.getMarker("NotWrited_Server");

    static {

        try {
            NonceUtil.blockUntilSecureRandomSeeded(10, TimeUnit.SECONDS);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    public static void main(String[] args) throws Exception {
        LOGGER.info(marker_err, "Starting Java OPC UA server version alpha 1.0.3");

        try {
            ExampleServer server = new ExampleServer();
            server.startup().get();
            final CompletableFuture<Void> future = new CompletableFuture<>();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> future.complete(null)));

            WriteFromText example = new WriteFromText();

            ExecutorService executorService = Executors.newFixedThreadPool(2);
            executorService.submit(new ClientRunner(example, true, ip, port));
            executorService.shutdown();

            executorService.awaitTermination(1, TimeUnit.DAYS);
            future.get();
        } catch (Exception e) {
            LOGGER.info(marker_err, "Close connection...");
//            LOGGER.info(marker_err, ExceptionUtils.getStackTrace(e));
            System.exit(0);
        }
    }

    private OpcUaServer server;
    private static ExampleNamespace exampleNamespace;

    public ExampleServer() throws Exception {
        Path securityTempDir = Paths.get(System.getProperty("java.io.tmpdir"), "server", "security");
        Files.createDirectories(securityTempDir);
        if (!Files.exists(securityTempDir)) {
            throw new Exception("unable to create security temp dir: " + securityTempDir);
        }

        File pkiDir = securityTempDir.resolve("pki").toFile();

        LoggerFactory.getLogger(getClass())
                .info("security dir: {}", securityTempDir.toAbsolutePath());
        LoggerFactory.getLogger(getClass())
                .info("security pki dir: {}", pkiDir.getAbsolutePath());

        KeyStoreLoader loader = new KeyStoreLoader().load(securityTempDir);

        DefaultCertificateManager certificateManager = new DefaultCertificateManager(
                loader.getServerKeyPair(),
                loader.getServerCertificateChain()
        );

        DefaultTrustListManager trustListManager = new DefaultTrustListManager(pkiDir);

        DefaultServerCertificateValidator certificateValidator =
                new DefaultServerCertificateValidator(trustListManager);

        UsernameIdentityValidator identityValidator = new UsernameIdentityValidator(
                true,
                authChallenge -> {
                    String username = authChallenge.getUsername();
                    String password = authChallenge.getPassword();

                    boolean userOk = "user".equals(username) && "password1".equals(password);
                    boolean adminOk = "admin".equals(username) && "password2".equals(password);

                    return userOk || adminOk;
                }
        );

        X509IdentityValidator x509IdentityValidator = new X509IdentityValidator(c -> true);

        // If you need to use multiple certificates you'll have to be smarter than this.
        X509Certificate certificate = certificateManager.getCertificates()
                .stream()
                .findFirst()
                .orElseThrow(() -> new UaRuntimeException(StatusCodes.Bad_ConfigurationError, "no certificate found"));

        // The configured application URI must match the one in the certificate(s)
        String applicationUri = CertificateUtil
                .getSanUri(certificate)
                .orElseThrow(() -> new UaRuntimeException(
                        StatusCodes.Bad_ConfigurationError,
                        "certificate is missing the application URI"));

        Set<EndpointConfiguration> endpointConfigurations = createEndpointConfigurations(certificate);

        OpcUaServerConfig serverConfig = OpcUaServerConfig.builder()
                .setApplicationUri(applicationUri)
                .setApplicationName(LocalizedText.english("Eclipse Milo OPC UA Example Server"))
                .setEndpoints(endpointConfigurations)
                .setBuildInfo(
                        new BuildInfo(
                                "urn:eclipse:milo:example-server",
                                "eclipse",
                                "eclipse milo example server",
                                OpcUaServer.SDK_VERSION,
                                "", DateTime.now()))
                .setCertificateManager(certificateManager)
                .setTrustListManager(trustListManager)
                .setCertificateValidator(certificateValidator)
                .setIdentityValidator(new CompositeValidator(identityValidator, x509IdentityValidator))
                .setProductUri("urn:eclipse:milo:example-server")
                .build();

        server = new OpcUaServer(serverConfig);

        exampleNamespace = new ExampleNamespace(server);
        exampleNamespace.startup();
    }

    private Set<EndpointConfiguration> createEndpointConfigurations(X509Certificate certificate) {
        Set<EndpointConfiguration> endpointConfigurations = new LinkedHashSet<>();

        List<String> bindAddresses = newArrayList();
        bindAddresses.add("0.0.0.0");

        Set<String> hostnames = new LinkedHashSet<>();
        hostnames.add(HostnameUtil.getHostname());
        hostnames.addAll(HostnameUtil.getHostnames(ExampleServer.ip));

        for (String bindAddress : bindAddresses) {
            for (String hostname : hostnames) {
                EndpointConfiguration.Builder builder = EndpointConfiguration.newBuilder()
                        .setBindAddress(bindAddress)
                        .setHostname(hostname)
                        .setPath("/")
                        .setCertificate(certificate)
                        .addTokenPolicies(
                                USER_TOKEN_POLICY_ANONYMOUS,
                                USER_TOKEN_POLICY_USERNAME,
                                USER_TOKEN_POLICY_X509);


                EndpointConfiguration.Builder noSecurityBuilder = builder.copy()
                        .setSecurityPolicy(SecurityPolicy.None)
                        .setSecurityMode(MessageSecurityMode.None);

                endpointConfigurations.add(buildTcpEndpoint(noSecurityBuilder));
                // endpointConfigurations.add(buildHttpsEndpoint(noSecurityBuilder));

                // TCP Basic256Sha256 / SignAndEncrypt
                endpointConfigurations.add(buildTcpEndpoint(
                        builder.copy()
                                .setSecurityPolicy(SecurityPolicy.Basic256Sha256)
                                .setSecurityMode(MessageSecurityMode.SignAndEncrypt))
                );
            }
        }

        return endpointConfigurations;
    }

    private static EndpointConfiguration buildTcpEndpoint(EndpointConfiguration.Builder base) {
        return base.copy()
                .setTransportProfile(TransportProfile.TCP_UASC_UABINARY)
                .setBindPort(Integer.parseInt(ExampleServer.port))
                .build();
    }

    public OpcUaServer getServer() {
        return server;
    }

    public CompletableFuture<OpcUaServer> startup() {
        return server.startup();
    }

    public CompletableFuture<OpcUaServer> shutdown() {
        exampleNamespace.shutdown();

        return server.shutdown();
    }

}
