package org.spectrum.sqlchecker.infrastructure.database.socket;

import com.mysql.cj.conf.PropertySet;
import com.mysql.cj.conf.RuntimeProperty;
import com.mysql.cj.protocol.StandardSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.SocketChannel;

/**
 * MySQL Connector/J socket factory that supports Unix domain sockets.
 */
public class UnixDomainSocketFactory extends StandardSocketFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnixDomainSocketFactory.class);

    public static boolean isSupported() {
        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.socket(); // Some JDKs don't support Socket for UNIX channels.
            return true;
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }

    @Override
    public <T extends Closeable> T connect(String host, int port, PropertySet props, int loginTimeout) throws IOException {
        String socketPath = getSocketPath(props);
        if (socketPath == null || socketPath.isBlank()) {
            return super.connect(host, port, props, loginTimeout);
        }

        try {
            return connectUnixSocket(host, port, props, loginTimeout, socketPath);
        } catch (UnsupportedOperationException unsupported) {
            LOGGER.warn("Unix domain sockets are not supported by this JVM; falling back to TCP for {}:{}.", host, port);
            return super.connect(host, port, props, loginTimeout);
        }
    }

    private <T extends Closeable> T connectUnixSocket(
            String host,
            int port,
            PropertySet props,
            int loginTimeout,
            String socketPath
    ) throws IOException {
        this.loginTimeoutCountdown = loginTimeout;
        this.host = host;
        this.port = port;

        Socket unixSocket = createUnixSocket(socketPath);
        configureSocket(unixSocket, props);
        this.rawSocket = unixSocket;
        this.sslSocket = unixSocket;
        resetLoginTimeCountdown();

        @SuppressWarnings("unchecked")
        T result = (T) unixSocket;
        return result;
    }

    @Override
    protected Socket createSocket(PropertySet props) {
        String socketPath = getSocketPath(props);
        if (socketPath == null || socketPath.isBlank()) {
            return super.createSocket(props);
        }
        try {
            return createUnixSocket(socketPath);
        } catch (IOException | UnsupportedOperationException e) {
            return super.createSocket(props);
        }
    }

    private Socket createUnixSocket(String socketPath) throws IOException {
        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        channel.connect(UnixDomainSocketAddress.of(socketPath));
        return channel.socket();
    }

    private String getSocketPath(PropertySet props) {
        String socketPath = getStringProperty(props, "unix_socket");
        if (socketPath == null || socketPath.isBlank()) {
            socketPath = getStringProperty(props, "unixSocket");
        }
        if (socketPath == null || socketPath.isBlank()) {
            socketPath = getStringProperty(props, "socket");
        }
        return socketPath;
    }

    private String getStringProperty(PropertySet props, String key) {
        try {
            RuntimeProperty<String> property = props.getStringProperty(key);
            if (property != null) {
                return property.getValue();
            }
        } catch (Exception ignored) {
        }
        try {
            String value = props.exposeAsProperties().getProperty(key);
            if (value != null) {
                return value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
