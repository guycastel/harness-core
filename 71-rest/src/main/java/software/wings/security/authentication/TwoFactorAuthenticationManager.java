package software.wings.security.authentication;

import static io.harness.data.encoding.EncodingUtils.decodeBase64ToString;
import static io.harness.eraro.ErrorCode.GENERAL_ERROR;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.WingsException;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.User;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.UserService;

import java.util.Optional;

@Singleton
public class TwoFactorAuthenticationManager {
  private static final Logger logger = LoggerFactory.getLogger(TwoFactorAuthenticationManager.class);

  @Inject private TOTPAuthHandler totpHandler;
  @Inject private UserService userService;
  @Inject private AccountService accountService;
  @Inject private AuthService authService;
  @Inject private EventPublishHelper eventPublishHelper;

  public TwoFactorAuthHandler getTwoFactorAuthHandler(TwoFactorAuthenticationMechanism mechanism) {
    switch (mechanism) {
      default:
        return totpHandler;
    }
  }

  public User authenticate(String jwtTokens) {
    String[] decryptedData = decodeBase64ToString(jwtTokens).split(":");
    if (decryptedData.length < 2) {
      throw new WingsException(ErrorCode.INVALID_CREDENTIAL);
    }
    String jwtToken = decryptedData[0];
    String passcode = decryptedData[1];

    User user = userService.verifyJWTToken(jwtToken, JWT_CATEGORY.MULTIFACTOR_AUTH);
    if (user == null) {
      throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST);
    }

    user = getTwoFactorAuthHandler(user.getTwoFactorAuthenticationMechanism()).authenticate(user, passcode);
    return authService.generateBearerTokenForUser(user);
  }

  public TwoFactorAuthenticationSettings createTwoFactorAuthenticationSettings(
      User user, TwoFactorAuthenticationMechanism mechanism) {
    return getTwoFactorAuthHandler(mechanism).createTwoFactorAuthenticationSettings(user);
  }

  public User enableTwoFactorAuthenticationSettings(User user, TwoFactorAuthenticationSettings settings) {
    if (settings.getMechanism() == null) {
      throw new WingsException(ErrorCode.INVALID_TWO_FACTOR_AUTHENTICATION_CONFIGURATION, USER);
    }
    if (!isAllowed2FAEnable(user)) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam(
              "message", "Two Factor authentication is not supported for Lite account. Please upgrade to Harness Pro.");
    }
    settings.setTwoFactorAuthenticationEnabled(true);
    return applyTwoFactorAuthenticationSettings(user, settings);
  }

  private User applyTwoFactorAuthenticationSettings(User user, TwoFactorAuthenticationSettings settings) {
    return getTwoFactorAuthHandler(settings.getMechanism()).applyTwoFactorAuthenticationSettings(user, settings);
  }

  public User disableTwoFactorAuthentication(User user) {
    // disable 2FA only if admin has not enforced 2FA.
    if (isAllowed2FADisable(user)) {
      if (user.isTwoFactorAuthenticationEnabled() && user.getTwoFactorAuthenticationMechanism() != null) {
        logger.info("Disabling 2FA for User={}, tfEnabled={}, tfMechanism={}", user.getEmail(),
            user.isTwoFactorAuthenticationEnabled(), user.getTwoFactorAuthenticationMechanism());
        return getTwoFactorAuthHandler(user.getTwoFactorAuthenticationMechanism()).disableTwoFactorAuthentication(user);
      }
    } else {
      logger.info("Could not disable 2FA for User={}, tfEnabled={}, tfMechanism={}", user.getEmail(),
          user.isTwoFactorAuthenticationEnabled(), user.getTwoFactorAuthenticationMechanism());
    }
    return user;
  }

  private boolean isAllowed2FADisable(User user) {
    if (user.getAccounts() != null) {
      return !(user.getAccounts().size() == 1 && user.getAccounts().get(0).isTwoFactorAdminEnforced());
    } else {
      return false;
    }
  }

  private boolean isAllowed2FAEnable(User user) {
    return CollectionUtils.isEmpty(user.getAccounts()) || !isPrimaryAccountLite(user);
  }

  private boolean isPrimaryAccountLite(User user) {
    return getPrimaryAccount(user).map(account -> accountService.isAccountLite(account.getUuid())).orElse(false);
  }

  private Optional<Account> getPrimaryAccount(User user) {
    return user.getAccounts().stream().findFirst();
  }

  public boolean getTwoFactorAuthAdminEnforceInfo(String accountId) {
    return accountService.getTwoFactorEnforceInfo(accountId);
  }

  public boolean isTwoFactorEnabledForAdmin(String accountId, User user) {
    boolean twoFactorEnabled = false;
    if (accountId != null && user != null) {
      twoFactorEnabled = userService.isTwoFactorEnabledForAdmin(accountId, user.getUuid());
    }
    return twoFactorEnabled;
  }

  public boolean overrideTwoFactorAuthentication(String accountId, TwoFactorAdminOverrideSettings settings) {
    try {
      if (settings != null) {
        if (accountService.isAccountLite(accountId) && settings.isAdminOverrideTwoFactorEnabled()) {
          throw new WingsException(GENERAL_ERROR, USER)
              .addParam("message",
                  "Two Factor authentication is not supported for Lite account. Please upgrade to Harness Pro.");
        }
        // Update 2FA enforce flag
        accountService.updateTwoFactorEnforceInfo(accountId, settings.isAdminOverrideTwoFactorEnabled());

        // Enable 2FA for all users if admin enforced
        if (settings.isAdminOverrideTwoFactorEnabled()) {
          logger.info("Enabling 2FA for all users in the account who have 2FA disabled ={}", accountId);
          boolean success =
              userService.overrideTwoFactorforAccount(accountId, settings.isAdminOverrideTwoFactorEnabled());
          if (success) {
            eventPublishHelper.publishSetup2FAEvent(accountId);
          }
          return success;
        }
      }
    } catch (Exception ex) {
      throw new WingsException(GENERAL_ERROR, USER)
          .addParam("message", "Exception occurred while enforcing Two factor authentication for users");
    }

    return false;
  }

  public boolean sendTwoFactorAuthenticationResetEmail(String userId) {
    User user = userService.get(userId);
    if (user.isTwoFactorAuthenticationEnabled()) {
      return getTwoFactorAuthHandler(user.getTwoFactorAuthenticationMechanism()).resetAndSendEmail(user);
    } else {
      logger.warn("Two Factor authentication is not enabled for user [{}]", userId);
      return false;
    }
  }
}
