package de.mannodermaus.junit5.condition;

import android.annotation.TargetApi;
import android.os.Build;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.Preconditions;

import java.util.Optional;

import static de.mannodermaus.junit5.condition.EnabledOnApiCondition.*;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

class DisabledOnApiCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@DisabledOnApi is not present");

  @TargetApi(24)
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<DisabledOnApi> optional = findAnnotation(context.getElement(), DisabledOnApi.class);

    if (optional.isPresent()) {
      DisabledOnApi annotation = optional.get();
      int minApi = annotation.min();
      int maxApi = annotation.max();

      boolean hasLowerBound = minApi != NOT_SET;
      boolean hasUpperBound = maxApi != NOT_SET;
      Preconditions.condition(hasLowerBound || hasUpperBound, "At least one value must be provided in @DisabledOnApi");

      // Constrain the current API Level based on the presence of "minApi" & "maxApi":
      // If either one is not set at all, that part of the conditional becomes true automatically
      return (!hasLowerBound || Build.VERSION.SDK_INT >= minApi) && (!hasUpperBound || Build.VERSION.SDK_INT <= maxApi)
          ? DISABLED_ON_CURRENT_API
          : ENABLED_ON_CURRENT_API;
    }

    return ENABLED_BY_DEFAULT;
  }
}
