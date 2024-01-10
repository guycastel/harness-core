package io.harness.gitsync.common.scmerrorhandling.handlers.harnesscode;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ScmBadRequestException;
import io.harness.exception.ScmUnexpectedException;
import io.harness.exception.WingsException;
import io.harness.gitsync.common.scmerrorhandling.dtos.ErrorMetadata;
import io.harness.gitsync.common.scmerrorhandling.handlers.ScmApiErrorHandler;
import io.harness.gitsync.common.scmerrorhandling.util.ErrorMessageFormatter;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(PIPELINE)
@Singleton
public class HarnessCodeGenericApiErrorHandler implements ScmApiErrorHandler {
  @Override
  public void handleError(int statusCode, String errorMessage, ErrorMetadata errorMetadata) throws WingsException {
    switch (statusCode) {
      case 400:
      case 404:
        throw new ScmBadRequestException(ErrorMessageFormatter.formatMessage(errorMessage, errorMetadata));
        // throwing not found on 500 status code as we don't ever expect 500 from harness code and as of today most
        // of the time it is due to not found
      case 500:
        throw new ScmBadRequestException(ErrorMessageFormatter.formatMessage("not found", errorMetadata));
      default: {
        log.error(
            String.format("New error in harness code ops. Status code: [%d]. message: [%s]", statusCode, errorMessage),
            new Exception());
        throw new ScmUnexpectedException(ErrorMessageFormatter.formatMessage(errorMessage, errorMetadata));
      }
    }
  }
}
