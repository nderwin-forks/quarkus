package io.quarkus.devui.runtime.mcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;

/**
 * Normal Dev UI Json RPC Service for the Dev MPC Screen
 */
@ApplicationScoped
public class DevMcpJsonRpcService {

    private final BroadcastProcessor<McpClientInfo> connectedClientStream = BroadcastProcessor.create();

    // TODO: We need to be able to deregister a client if the connection drops. Mayby ping it ?

    private final Set<McpClientInfo> connectedClients = new HashSet<>();
    private McpServerConfiguration mcpServerConfiguration;

    @PostConstruct
    public void init() {
        this.mcpServerConfiguration = new McpServerConfiguration(loadConfiguration());
    }

    public Set<McpClientInfo> getConnectedClients() {
        if (!this.connectedClients.isEmpty()) {
            return this.connectedClients;
        }
        return null;
    }

    public Multi<McpClientInfo> getConnectedClientStream() {
        return connectedClientStream;
    }

    public void addClientInfo(McpClientInfo clientInfo) {
        this.connectedClients.add(clientInfo);
        this.connectedClientStream.onNext(clientInfo);
    }

    @Produces
    public McpServerConfiguration getMcpServerConfiguration() {
        return this.mcpServerConfiguration;
    }

    public McpServerConfiguration enable() {
        this.mcpServerConfiguration.enable();
        storeConfiguration(this.mcpServerConfiguration.toProperties());
        return this.mcpServerConfiguration;
    }

    public McpServerConfiguration disable() {
        this.mcpServerConfiguration.disable();
        storeConfiguration(this.mcpServerConfiguration.toProperties());
        return this.mcpServerConfiguration;
    }

    private Properties loadConfiguration() {
        Properties existingProps = new Properties();

        // Load existing configuration if present
        if (Files.exists(configFile)) {
            try (InputStream in = Files.newInputStream(configFile)) {
                existingProps.load(in);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return existingProps;
    }

    private boolean storeConfiguration(Properties p) {
        try (OutputStream out = Files.newOutputStream(configFile)) {
            p.store(out, "Dev MCP Configuration");
            return true;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    private final Path configDir = Paths.get(System.getProperty("user.home"), ".quarkus");
    private final Path configFile = configDir.resolve("dev-mcp.properties");

}
