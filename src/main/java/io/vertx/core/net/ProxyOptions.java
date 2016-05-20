/*
 * Copyright (c) 2011-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.vertx.core.net;

import java.util.Objects;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Proxy options for a NetClient or HttpClient
 * @author <a href="http://oss.lehmann.cx/">Alexander Lehmann</a>
 */
@DataObject(generateConverter = true)
public class ProxyOptions implements Cloneable {

  private String proxyHost;
  private int proxyPort;
  private String proxyUsername;
  private String proxyPassword;
  private ProxyType proxyType;

  /**
   * Default constructor
   */
  public ProxyOptions() {
    proxyHost = "localhost";
    proxyPort = 3128;
    proxyUsername = null;
    proxyPassword = null;
    proxyType = ProxyType.HTTP;
  }

  /**
   * Copy constructor
   *
   * @param other  the options to copy
   */
  public ProxyOptions(ProxyOptions other) {
    proxyHost = other.getProxyHost();
    proxyPort = other.getProxyPort();
    proxyUsername = other.getProxyUsername();
    proxyPassword = other.getProxyPassword();
    proxyType = other.getProxyType();
  }

  /**
   * Create options from JSON
   *
   * @param json  the JSON
   */
  public ProxyOptions(JsonObject json) {
    this();
    ProxyOptionsConverter.fromJson(json, this);
  }

  /**
   * @return  proxy host
   */
  public String getProxyHost() {
    return proxyHost;
  }

  /**
   * Set proxy host
   *
   * @param proxyHost the proxy host to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setProxyHost(String proxyHost) {
    this.proxyHost = proxyHost;
    return this;
  }

  /**
   * @return  proxy host
   */
  public int getProxyPort() {
    return proxyPort;
  }

  /**
   * Set proxy host
   *
   * @param proxyHost the proxy host to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setProxyPort(int proxyPort) {
    this.proxyPort = proxyPort;
    return this;
  }

  /**
   * @return  proxy host
   */
  public String getProxyUsername() {
    return proxyUsername;
  }

  /**
   * Set proxy host
   *
   * @param proxyHost the proxy host to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setProxyUsername(String proxyUsername) {
    this.proxyUsername = proxyUsername;
    return this;
  }

  /**
   * @return  proxy host
   */
  public String getProxyPassword() {
    return proxyPassword;
  }

  /**
   * Set proxy host
   *
   * @param proxyHost the proxy host to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setProxyPassword(String proxyPassword) {
    this.proxyPassword = proxyPassword;
    return this;
  }

  /**
   * @return  proxy host
   */
  public ProxyType getProxyType() {
    return proxyType;
  }

  /**
   * Set proxy host
   *
   * @param proxyHost the proxy host to connect to
   * @return a reference to this, so the API can be used fluently
   */
  public ProxyOptions setProxyType(ProxyType proxyType) {
    Objects.requireNonNull(proxyType, "ProxyType may not be null");
    this.proxyType = proxyType;
    return this;
  }

  @Override
  public ProxyOptions clone() {
    return new ProxyOptions(this);
  }

}
