package io.harness.beans.seriazlier;

import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.product.ci.engine.proto.RunStep;
import io.harness.product.ci.engine.proto.Step;
import io.harness.product.ci.engine.proto.StepContext;
import org.apache.commons.codec.binary.Base64;

import java.util.Optional;

public class RunStepProtobufSerializer implements ProtobufSerializer<RunStepInfo> {
  @Override
  public String serialize(RunStepInfo object) {
    return Base64.encodeBase64String(convertRunStepInfo(object).toByteArray());
  }

  public Step convertRunStepInfo(RunStepInfo runStepInfo) {
    RunStep.Builder runStepBuilder = RunStep.newBuilder();
    runStepBuilder.addAllCommands(runStepInfo.getCommand());
    if (runStepInfo.getOutput() != null) {
      runStepBuilder.addAllEnvVarOutputs(runStepInfo.getOutput());
    }
    runStepBuilder.setContext(StepContext.newBuilder()
                                  .setNumRetries(runStepInfo.getRetry())
                                  .setExecutionTimeoutSecs(runStepInfo.getTimeout())
                                  .build());

    return Step.newBuilder()
        .setId(runStepInfo.getIdentifier())
        .setDisplayName(Optional.ofNullable(runStepInfo.getDisplayName()).orElse(""))
        .setRun(runStepBuilder.build())
        .build();
  }
}
