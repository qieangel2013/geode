/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.test.dunit.rules;

import org.apache.geode.management.internal.cli.CliUtil;
import org.apache.geode.management.internal.cli.HeadlessGfsh;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.management.internal.cli.result.CommandResult;
import org.apache.geode.management.internal.cli.result.ErrorResultData;
import org.apache.geode.management.internal.cli.result.ResultBuilder;
import org.apache.geode.management.internal.cli.util.CommandStringBuilder;
import org.apache.geode.test.junit.rules.DescribedExternalResource;
import org.junit.runner.Description;

/**
 * Class which eases the connection to the jmxManager {@link ConnectionConfiguration} it allows for
 * the creation of per-test connections with different user/password combinations, or no username
 * and password
 */
public class GfshShellConnectionRule extends DescribedExternalResource {

  private int port = 0;
  private boolean useHttp = false;
  private HeadlessGfsh gfsh;
  private boolean authenticated;

  /**
   * Rule constructor
   */

  public GfshShellConnectionRule(int port) {
    this.useHttp = false;
    this.port = port;
  }

  public GfshShellConnectionRule(int port, boolean useHttp) {
    this.useHttp = useHttp;
    this.port = port;
  }

  protected void before(Description description) throws Throwable {
    CliUtil.isGfshVM = true;
    String shellId = getClass().getSimpleName() + "_" + description.getMethodName();
    gfsh = new HeadlessGfsh(shellId, 30, "gfsh_files"); // TODO: move to TemporaryFolder

    final CommandStringBuilder connectCommand = new CommandStringBuilder(CliStrings.CONNECT);

    ConnectionConfiguration config = description.getAnnotation(ConnectionConfiguration.class);
    if (config != null) {
      connectCommand.addOption(CliStrings.CONNECT__USERNAME, config.user());
      connectCommand.addOption(CliStrings.CONNECT__PASSWORD, config.password());
    }

    String endpoint;
    if (useHttp) {
      endpoint = "http://localhost:" + port + "/gemfire/v1";
      connectCommand.addOption(CliStrings.CONNECT__USE_HTTP, Boolean.TRUE.toString());
      connectCommand.addOption(CliStrings.CONNECT__URL, endpoint);
    } else {
      endpoint = "localhost[" + port + "]";
      connectCommand.addOption(CliStrings.CONNECT__JMX_MANAGER, endpoint);
    }
    System.out.println(getClass().getSimpleName() + " using endpoint: " + endpoint);

    gfsh.executeCommand(connectCommand.toString());

    CommandResult result = (CommandResult) gfsh.getResult();
    if (result.getResultData() instanceof ErrorResultData) {
      ErrorResultData errorResultData = (ErrorResultData) result.getResultData();
      this.authenticated =
          !(errorResultData.getErrorCode() == ResultBuilder.ERRORCODE_CONNECTION_ERROR);
    } else {
      this.authenticated = true;
    }
  }

  /**
   * Override to tear down your specific external resource.
   */
  protected void after(Description description) throws Throwable {
    if (gfsh != null) {
      gfsh.clearEvents();
      gfsh.executeCommand("disconnect");
      gfsh.executeCommand("exit");
      gfsh.terminate();
      gfsh.setThreadLocalInstance();
      gfsh = null;
    }
    CliUtil.isGfshVM = false;
  }

  public HeadlessGfsh getGfsh() {
    return gfsh;
  }

  public boolean isAuthenticated() {
    return authenticated;
  }
}