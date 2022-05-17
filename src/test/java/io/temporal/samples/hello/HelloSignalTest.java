/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.hello;

import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.hello.HelloSignal.GreetingWorkflow;
import io.temporal.testing.TestWorkflowRule;
import io.temporal.workflow.Workflow;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;

/** Unit test for {@link HelloSignal}. Doesn't use an external Temporal service. */
public class HelloSignalTest {
  private Logger logger = Workflow.getLogger(HelloSignalTest.class);

  @Rule
  public TestWorkflowRule testWorkflowRule =
      TestWorkflowRule.newBuilder()
          .setWorkflowTypes(HelloSignal.GreetingWorkflowImpl.class)
          .setUseTimeskipping(false)
          .build();

  @Test
  public void testSignal() throws TimeoutException, InterruptedException, ExecutionException {
    // Get a workflow stub using the same task queue the worker uses.
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder()
            .setTaskQueue(testWorkflowRule.getTaskQueue())
            .setWorkflowIdReusePolicy(
                WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
            .build();
    GreetingWorkflow workflow =
        testWorkflowRule
            .getWorkflowClient()
            .newWorkflowStub(GreetingWorkflow.class, workflowOptions);

    // Start workflow asynchronously to not use another thread to signal.
    WorkflowClient.start(workflow::getGreetings);

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send a signal to it using workflow stub immediately.
    // But just to demonstrate the unit testing of a long running workflow adding a long sleep here.
    // This workflow keeps receiving signals until exit is called
    workflow.waitForName("World");
    workflow.waitForName("Universe");

    WorkflowStub stub = WorkflowStub.fromTyped(workflow);

    CompletableFuture<Void> future =
        CompletableFuture.runAsync(
            () -> {
              try {
                logger.info("Attempting to exit.");
                Thread.sleep(1000);
                workflow.exit();
              } catch (InterruptedException e) {
                throw new RuntimeException(e);
              }
            });

    logger.info("Getting result.");
    stub.getResult(10000, TimeUnit.MILLISECONDS, Void.class);

    future.get();
  }
}
