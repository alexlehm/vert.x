package io.vertx.test.core;

import java.util.Base64;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.streams.Pump;

/**
 * Http Connect Proxy
 * <p>
 * A simple Http CONNECT proxy for testing https proxy functionality. HTTP server running on localhost allowing CONNECT
 * requests only. This is basically a socket forwarding protocol allowing to use the proxy server to connect to the
 * internet.
 * <p>
 * Usually the server will be started in @Before and stopped in @After for a unit test using HttpClient with the
 * setProxyXXX methods.
 *
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
public class SocksProxy {

  private static final Logger log = LoggerFactory.getLogger(SocksProxy.class);

  private static final Buffer clientInit = Buffer.buffer(new byte[] { 5, 1, 0 });
  private static final Buffer serverReply = Buffer.buffer(new byte[] { 5, 0 });
  private static final Buffer clientRequest = Buffer.buffer(new byte[] { 5, 1, 0, 3 });
  private static final Buffer connectResponse = Buffer.buffer(new byte[] { 5, 0, 0, 1, 0x7f, 0, 0, 1, 0x27, 0x10 });
  private static final Buffer errorResponse = Buffer.buffer(new byte[] { 5, 4, 0, 1, 0, 0, 0, 0, 0, 0 });

  private final String username;
  private NetServer server;
  private String lastUri;
  private String forceUri;

  public SocksProxy(String username) {
    this.username = username;
  }

  /**
   * check the last accessed host:ip
   * 
   * @return the lastUri
   */
  public String getLastUri() {
    return lastUri;
  }

  /**
   * force uri to connect to a given string (e.g. "localhost:4443") this is to simulate a host that only resolves on the
   * proxy
   */
  public void setForceUri(String uri) {
    forceUri = uri;
  }

  /**
   * Start the server.
   * 
   * @param vertx
   *          Vertx instance to use for creating the server and client
   * @param finishedHandler
   *          will be called when the start has started
   */
  public void start(Vertx vertx, Handler<Void> finishedHandler) {
    NetServerOptions options = new NetServerOptions();
    options.setHost("localhost").setPort(11080);
    server = vertx.createNetServer(options);
    server.connectHandler(socket -> {
      socket.handler(buffer -> {
        if (!buffer.equals(clientInit)) {
          throw new IllegalStateException("expected "+toHex(clientInit)+", got "+toHex(buffer));
        }
        log.info("got request: "+toHex(buffer));
        socket.handler(buffer2 -> {
          if(!buffer2.getBuffer(0, clientRequest.length()).equals(clientRequest)) {
            throw new IllegalStateException("expected "+toHex(clientRequest)+", got "+toHex(buffer2));
          }
          int stringLen = buffer2.getUnsignedByte(4);
          log.info("string len "+stringLen);
          if (buffer2.length()!=7+stringLen) {
            throw new IllegalStateException("format error in client request, got "+toHex(buffer2));
          }
          String host = buffer2.getString(5, 5+stringLen);
          int port = buffer2.getUnsignedShort(5+stringLen);
          log.info("got request: "+toHex(buffer2));
          log.info("connect: "+host+":"+port);
          socket.handler(null);
          log.info("connecting to " + host + ":" + port);
          lastUri = host+":"+port;
          NetClient netClient = vertx.createNetClient(new NetClientOptions());
          netClient.connect(port, host, result -> {
            if (result.succeeded()) {
              log.info("writing: " + toHex(connectResponse));
              socket.write(connectResponse);
              log.info("connected, starting pump");
              NetSocket clientSocket = result.result();
              socket.closeHandler(v -> clientSocket.close());
              clientSocket.closeHandler(v -> socket.close());
              Pump.pump(socket, clientSocket).start();
              Pump.pump(clientSocket, socket).start();
            } else {
              log.error("exception", result.cause());
              log.info("writing: " + toHex(errorResponse));
              socket.write(errorResponse);
              socket.close();
            }
          });
        });
        log.info("writing: "+toHex(serverReply));
        socket.write(serverReply);
      });
    });
    server.listen(result -> {
      log.info("socks5 server started");
      finishedHandler.handle(null);
    });
  }

  /**
   * @param buffer
   * @return
   */
  private String toHex(Buffer buffer) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < buffer.length(); i++) {
      sb.append(String.format("%02X ", buffer.getByte(i)));
    }
    return sb.toString();
  }

  /**
   * Stop the server.
   *
   * Doesn't wait for the close operation to finish
   */
  public void stop() {
    if (server != null) {
      server.close();
      server = null;
    }
  }
}
