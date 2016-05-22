/**
 * 
 */
package io.vertx.core.net.impl;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyConnectionEvent;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import io.netty.resolver.NoopAddressResolverGroup;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;

/**
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 *
 */
public class ProxyChannelProvider implements ChannelProvider {

  private static final Logger log = LoggerFactory.getLogger(ProxyChannelProvider.class);

  private final ChannelProviderAdditionalOperations addl;

  public ProxyChannelProvider(ChannelProviderAdditionalOperations addl) {
    this.addl = addl;
  }

  @Override
  public void connect(VertxInternal vertx, Bootstrap bootstrap, ProxyOptions options, String host, int port,
      Handler<AsyncResult<Channel>> channelHandler) {

    final String proxyHost = options.getProxyHost();
    final int proxyPort = options.getProxyPort();
    final String proxyUsername = options.getProxyUsername();
    final String proxyPassword = options.getProxyPassword();
    final ProxyType proxyType = options.getProxyType();

    vertx.resolveHostname(proxyHost, dnsRes -> {
      if (dnsRes.succeeded()) {
        InetAddress address = dnsRes.result();
        InetSocketAddress proxyAddr = new InetSocketAddress(address, proxyPort);
        ProxyHandler proxy;

        switch (proxyType) {
          default:
          case HTTP:
            log.info("configuring http connect proxy");
            proxy = proxyUsername != null && proxyPassword != null ? new HttpProxyHandler(proxyAddr, proxyUsername, proxyPassword) : new HttpProxyHandler(proxyAddr);
            break;
          case SOCKS5:
            log.info("configuring socks5 proxy");
            proxy = proxyUsername != null && proxyPassword != null ? new Socks5ProxyHandler(proxyAddr, proxyUsername, proxyPassword) : new Socks5ProxyHandler(proxyAddr);
            break;
          case SOCKS4:
            log.info("configuring socks4 proxy");
            // apparently SOCKS4 only supports a username?
            proxy = proxyUsername != null ? new Socks4ProxyHandler(proxyAddr, proxyUsername) : new Socks4ProxyHandler(proxyAddr);
            break;
        }

        bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
        InetSocketAddress t = InetSocketAddress.createUnresolved(host, port);

        bootstrap.handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addFirst("proxy", proxy);
            // set up other pipeline entries
            addl.channelStartup(ch);
            addl.pipelineSetup(ch.pipeline());
            pipeline.addLast(new ChannelInboundHandlerAdapter() {
              @Override
              public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                log.info("userEventTriggered "+evt.toString());
                if (evt instanceof ProxyConnectionEvent) {
                  pipeline.remove(proxy);
                  addl.pipelineDeprov(pipeline);
                  pipeline.remove(this);
                  callHandler(channelHandler, Future.succeededFuture(ch));
                }
                ctx.fireUserEventTriggered(evt);
              }
            });
          }
        });
        ChannelFuture future = bootstrap.connect(t);

        future.addListener(res -> {
          if (!res.isSuccess()) {
            callHandler(channelHandler, Future.failedFuture(res.cause()));
          }
        });
      } else {
        callHandler(channelHandler, Future.failedFuture(dnsRes.cause()));
      }
    });
  }

  /**
   * @param channelHandler
   * @param future
   */
  private void callHandler(Handler<AsyncResult<Channel>> channelHandler, Future<Channel> future) {
    String type = future.succeeded() ? "succeeded" : "failed";
    log.info("calling channelHandler with "+type+" future "+future.toString());
    channelHandler.handle(future);
  }
}
