package io.vertx.core.http.impl.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyConnectionEvent;
import io.netty.resolver.NoopAddressResolverGroup;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.impl.ChannelProvider;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.impl.ChannelProviderAdditionalOperations;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ProxyChannelProvider implements ChannelProvider {

  private static final Logger log = LoggerFactory.getLogger(ProxyChannelProvider.class);

  @Override
  public void connect(VertxInternal vertx, Bootstrap bootstrap, HttpClientOptions options, String host, int port, Handler<AsyncResult<Channel>> channelHandler) {
    ProxyOptions proxyOptions = options.getProxyOptions();
    String proxyHost = proxyOptions.getProxyHost();
    int proxyPort = proxyOptions.getProxyPort();
    String proxyUsername = proxyOptions.getProxyUsername();
    String proxyPassword = proxyOptions.getProxyPassword();

    vertx.resolveHostname(proxyHost, dnsRes -> {
      if (dnsRes.succeeded()) {
        InetAddress address = dnsRes.result();
        InetSocketAddress proxyAddr = new InetSocketAddress(address, proxyPort);
        HttpProxyHandler proxy;
        if (proxyUsername != null && proxyPassword != null) {
          proxy = new HttpProxyHandler(proxyAddr, proxyUsername, proxyPassword);
        } else {
          proxy = new HttpProxyHandler(proxyAddr);
        }
        bootstrap.handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ChannelPipeline pipeline = ch.pipeline();
            pipeline.addLast("proxy", proxy);
//            addl.pipelineSetup(pipeline);
            pipeline.addLast(new ChannelInboundHandlerAdapter() {
              @Override
              public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                log.info("userEventTriggered "+evt.toString());
                if (evt instanceof ProxyConnectionEvent) {
                  log.info("proxyconnectevent "+evt.toString());
                  pipeline.remove(proxy);
//                  addl.pipelineDeprov(pipeline);
                  pipeline.remove(this);
                  channelHandler.handle(Future.succeededFuture(ch));
                }
              }
            });
          }
        });
        // do not resolve the hostname on the client
        bootstrap.resolver(NoopAddressResolverGroup.INSTANCE);
        InetSocketAddress t = InetSocketAddress.createUnresolved(host, port);
        ChannelFuture future1 = bootstrap.connect(t);
        future1.addListener(f -> {
          if (!f.isSuccess()) {
            channelHandler.handle(Future.failedFuture(f.cause()));
          }
        });
      } else {
        channelHandler.handle(Future.failedFuture(dnsRes.cause()));
      }
    });
  }
}
