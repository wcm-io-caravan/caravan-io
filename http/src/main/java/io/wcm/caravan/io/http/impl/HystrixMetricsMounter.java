/*
 * #%L
 * wcm.io
 * %%
 * Copyright (C) 2014 wcm.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package io.wcm.caravan.io.http.impl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import com.netflix.hystrix.contrib.codahalemetricspublisher.HystrixCodaHaleMetricsPublisher;
import com.netflix.hystrix.strategy.HystrixPlugins;

/**
 * Mounter for the Hystrix Metrics plugin.
 */
@Component
public class HystrixMetricsMounter {

  private static final Logger LOG = LoggerFactory.getLogger(HystrixMetricsMounter.class);

  @Reference
  private MetricRegistry metricRegistry;
  @Reference
  private GraphiteIntegrationConfig graphiteIntegrationConfig;

  private GraphiteRegistration graphiteRegistration = new GraphiteRegistration();

  @Activate
  void activate() throws IOException {
    HystrixCodaHaleMetricsPublisher publisher = new HystrixCodaHaleMetricsPublisher(metricRegistry);
    HystrixPlugins.getInstance().registerMetricsPublisher(publisher);

    graphiteRegistration.interrupt();
    graphiteRegistration.start();
  }

  @Deactivate
  protected void deactivate() throws IOException {
    graphiteRegistration.interrupt();

    metricRegistry.getNames().stream().forEach(name -> metricRegistry.remove(name));
    HystrixPlugins.reset();

  }

  /**
   * register the graphite reporter only if the first connection could be established, to prevent lots of connection exception in logs.
   */
  private class GraphiteRegistration extends Thread {

    private Graphite graphiteServer;

    @Override
    public void run() {
      closeConnection();

      if (graphiteIntegrationConfig.isEnabled()) {
        String hostName = graphiteIntegrationConfig.getGraphiteHostName();
        int port = graphiteIntegrationConfig.getGraphiteSocketPort();

        LOG.info("Graphite publisher is enabled.");
        graphiteServer = new Graphite(new InetSocketAddress(hostName, port));

        if (establishConnection()) {
          int pushInterval = graphiteIntegrationConfig.getPushInterval();
          GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry).build(graphiteServer);
          reporter.start(pushInterval, TimeUnit.SECONDS);
          LOG.info("Sending every {} seconds to {}:{}", pushInterval, hostName, port);
        }
      }
      else {
        LOG.info("Graphite publisher is disabled.");
      }
    }

    private boolean establishConnection() {
      int sleepMinutes = 2;
      while (!Thread.currentThread().isInterrupted()) {
        try {
          graphiteServer.connect();
          LOG.info("Connection to graphite server is established.");
          return true;
        }
        catch (IOException e) {
          LOG.error("Unable to connect to {}:{}! wait {} minutes and reconnect. ERROR: {}",
            graphiteIntegrationConfig.getGraphiteHostName(),
            graphiteIntegrationConfig.getGraphiteSocketPort(),
            sleepMinutes,
            e.getMessage());
          try {
            Thread.sleep(TimeUnit.MINUTES.toMillis(sleepMinutes));
          }
          catch (InterruptedException e1) {
            return false;
          }
        }
      }
      return false;
    }


    void closeConnection() {
      if (graphiteServer != null && graphiteServer.isConnected()) {
        try {
          graphiteServer.close();
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

}
