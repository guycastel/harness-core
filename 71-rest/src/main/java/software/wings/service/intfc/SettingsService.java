package software.wings.service.intfc;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ValidationResult;
import software.wings.service.intfc.ownership.OwnedByAccount;
import software.wings.service.intfc.ownership.OwnedBySettingAttribute;
import software.wings.settings.SettingValue;
import software.wings.settings.SettingValue.SettingVariableTypes;
import software.wings.settings.UsageRestrictions;
import software.wings.utils.ArtifactType;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public interface SettingsService extends OwnedByAccount, OwnedBySettingAttribute {
  /**
   * List.
   *
   * @param req the req
   * @param appIdFromRequest
   * @param envIdFromRequest
   * @return the page response
   */
  PageResponse<SettingAttribute> list(
      PageRequest<SettingAttribute> req, String appIdFromRequest, String envIdFromRequest);

  PageResponse<SettingAttribute> list(PageRequest<SettingAttribute> req, String appIdFromRequest,
      String envIdFromRequest, String accountId, boolean gitSshConfigOnly, boolean withArtifactStreamCount,
      String artifactStreamSearchString, int maxArtifactStreams, ArtifactType artifactType);

  List<SettingAttribute> getFilteredSettingAttributes(
      List<SettingAttribute> inputSettingAttributes, String appIdFromRequest, String envIdFromRequest);

  @ValidationGroups(Create.class) SettingAttribute save(@Valid SettingAttribute settingAttribute);

  @ValidationGroups(Create.class) SettingAttribute forceSave(@Valid SettingAttribute settingAttribute);

  @ValidationGroups(Create.class) SettingAttribute save(@Valid SettingAttribute settingAttribute, boolean pushToGit);

  SettingAttribute get(String appId, String varId);

  SettingAttribute get(String appId, String envId, String varId);

  SettingAttribute get(String varId);

  SettingAttribute getOnlyConnectivityError(String settingId);

  SettingAttribute getSettingAttributeByName(String accountId, String settingAttributeName);

  @ValidationGroups(Update.class) SettingAttribute update(@Valid SettingAttribute settingAttribute);

  @ValidationGroups(Update.class)
  SettingAttribute update(@Valid SettingAttribute settingAttribute, boolean updateConnectivity);

  @ValidationGroups(Update.class)
  SettingAttribute update(@Valid SettingAttribute settingAttribute, boolean updateConnectivity, boolean pushToGit);

  /**
   * INTERNAL API only no usage restriction is checked. Only update the usage restrictions of the specified setting
   * attribute. This API is primary called during migration of removing dangling app/env references,
   */
  void updateUsageRestrictionsInternal(String uuid, UsageRestrictions usageRestrictions);

  void delete(String appId, String varId);

  void delete(String appId, String varId, boolean pushToGit, boolean syncFromGit);

  boolean retainSelectedGitConnectorsAndDeleteRest(String accountId, List<String> gitConnectorToRetain);

  SettingAttribute getByName(String accountId, String appId, String attributeName);

  SettingAttribute getByName(String accountId, String appId, String envId, String attributeName);

  SettingAttribute fetchSettingAttributeByName(
      @NotEmpty String accountId, @NotEmpty String attributeName, @NotNull SettingVariableTypes settingVariableTypes);

  void createDefaultApplicationSettings(String appId, String accountId, boolean syncFromGit);

  List<SettingAttribute> getSettingAttributesByType(String appId, String type);

  List<SettingAttribute> getFilteredSettingAttributesByType(
      String appId, String type, String currentAppId, String currentEnvId);

  List<SettingAttribute> getSettingAttributesByType(String appId, String envId, String type);

  List<SettingAttribute> getFilteredSettingAttributesByType(
      String appId, String envId, String type, String currentAppId, String currentEnvId);

  List<SettingAttribute> getSettingAttributesByType(String accountId, String appId, String envId, String type);

  List<SettingAttribute> getGlobalSettingAttributesByType(String accountId, String type);

  List<SettingAttribute> getFilteredGlobalSettingAttributesByType(
      String accountId, String type, String currentAppId, String currentEnvId);

  void deleteSettingAttributesByType(String accountId, String appId, String envId, String type);

  SettingValue getSettingValueById(String accountId, String id);

  ValidationResult validate(SettingAttribute settingAttribute);

  ValidationResult validate(String varId);

  ValidationResult validateConnectivity(SettingAttribute settingAttribute);

  void deleteByYamlGit(String appId, String settingAttributeId, boolean syncFromGit);
  Map<String, String> listAccountDefaults(String accountId);

  Map<String, String> listAppDefaults(String accountId, String appId);

  GitConfig fetchGitConfigFromConnectorId(String gitConnectorId);

  String fetchAccountIdBySettingId(String settingId);

  UsageRestrictions getUsageRestrictionsForSettingId(String settingId);

  void openConnectivityErrorAlert(String accountId, String settingId, String settingCategory, String connectivityError);

  void closeConnectivityErrorAlert(String accountId, String settingId);
}
