package software.wings.beans.infrastructure;

import static com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Reference;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.ConfigFile;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * The Class Host.
 */
@Entity(value = "hosts", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Indexes(@Index(fields = { @Field("infraId")
                           , @Field("hostName") }, options = @IndexOptions(unique = true)))
public class Host extends Base {
  @NotEmpty private String infraId;
  private String hostName;
  private String osType;

  @FormDataParam("hostConnAttr") @NotNull private String hostConnAttr;

  @FormDataParam("bastionConnAttr") private String bastionConnAttr;

  @FormDataParam("configTag") @Reference(idOnly = true, ignoreMissing = true) @Transient private Tag configTag;

  @Transient private List<ConfigFile> configFiles = new ArrayList<>();

  @Transient @JsonProperty(access = WRITE_ONLY) private List<String> hostNames; // to support bulk add host API

  @FormDataParam("serviceTemplates")
  @Transient
  @JsonProperty(access = WRITE_ONLY)
  private List<ServiceTemplate> serviceTemplates; // to support bulk add host API

  /**
   * Gets infra id.
   *
   * @return the infra id
   */
  public String getInfraId() {
    return infraId;
  }

  /**
   * Sets infra id.
   *
   * @param infraId the infra id
   */
  public void setInfraId(String infraId) {
    this.infraId = infraId;
  }

  /**
   * Gets host name.
   *
   * @return the host name
   */
  public String getHostName() {
    return hostName;
  }

  /**
   * Sets host name.
   *
   * @param hostName the host name
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * Gets host conn attr.
   *
   * @return the host conn attr
   */
  public String getHostConnAttr() {
    return hostConnAttr;
  }

  /**
   * Sets host conn attr.
   *
   * @param hostConnAttr the host conn attr
   */
  public void setHostConnAttr(String hostConnAttr) {
    this.hostConnAttr = hostConnAttr;
  }

  /**
   * Gets bastion conn attr.
   *
   * @return the bastion conn attr
   */
  public String getBastionConnAttr() {
    return bastionConnAttr;
  }

  /**
   * Sets bastion conn attr.
   *
   * @param bastionConnAttr the bastion conn attr
   */
  public void setBastionConnAttr(String bastionConnAttr) {
    this.bastionConnAttr = bastionConnAttr;
  }

  /**
   * Gets config tag.
   *
   * @return the config tag
   */
  public Tag getConfigTag() {
    return configTag;
  }

  /**
   * Sets config tag.
   *
   * @param configTag the config tag
   */
  public void setConfigTag(Tag configTag) {
    this.configTag = configTag;
  }

  /**
   * Gets config files.
   *
   * @return the config files
   */
  public List<ConfigFile> getConfigFiles() {
    return configFiles;
  }

  /**
   * Sets config files.
   *
   * @param configFiles the config files
   */
  public void setConfigFiles(List<ConfigFile> configFiles) {
    this.configFiles = configFiles;
  }

  /**
   * Gets host names.
   *
   * @return the host names
   */
  public List<String> getHostNames() {
    return hostNames;
  }

  /**
   * Sets host names.
   *
   * @param hostNames the host names
   */
  public void setHostNames(List<String> hostNames) {
    this.hostNames = hostNames;
  }

  /**
   * Gets os type.
   *
   * @return the os type
   */
  public String getOsType() {
    return osType;
  }

  /**
   * Sets os type.
   *
   * @param osType the os type
   */
  public void setOsType(String osType) {
    this.osType = osType;
  }

  /**
   * Gets service templates.
   *
   * @return the service templates
   */
  public List<ServiceTemplate> getServiceTemplates() {
    return serviceTemplates;
  }

  /**
   * Sets service templates.
   *
   * @param serviceTemplates the service templates
   */
  public void setServiceTemplates(List<ServiceTemplate> serviceTemplates) {
    this.serviceTemplates = serviceTemplates;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("infraId", infraId)
        .add("hostName", hostName)
        .add("osType", osType)
        .add("hostConnAttr", hostConnAttr)
        .add("bastionConnAttr", bastionConnAttr)
        .add("configTag", configTag)
        .add("configFiles", configFiles)
        .add("hostNames", hostNames)
        .add("serviceTemplates", serviceTemplates)
        .toString();
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(infraId, hostName, osType, hostConnAttr, bastionConnAttr, configTag, configFiles, hostNames,
              serviceTemplates);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final Host other = (Host) obj;
    return Objects.equals(this.infraId, other.infraId) && Objects.equals(this.hostName, other.hostName)
        && Objects.equals(this.osType, other.osType) && Objects.equals(this.hostConnAttr, other.hostConnAttr)
        && Objects.equals(this.bastionConnAttr, other.bastionConnAttr)
        && Objects.equals(this.configTag, other.configTag) && Objects.equals(this.configFiles, other.configFiles)
        && Objects.equals(this.hostNames, other.hostNames)
        && Objects.equals(this.serviceTemplates, other.serviceTemplates);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String infraId;
    private String hostName;
    private String osType;
    private String hostConnAttr;
    private String bastionConnAttr;
    private Tag configTag;
    private List<ConfigFile> configFiles = new ArrayList<>();
    private List<String> hostNames; // to support bulk add host API
    private List<ServiceTemplate> serviceTemplates; // to support bulk add host API
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A host builder.
     *
     * @return the builder
     */
    public static Builder aHost() {
      return new Builder();
    }

    /**
     * With infra id builder.
     *
     * @param infraId the infra id
     * @return the builder
     */
    public Builder withInfraId(String infraId) {
      this.infraId = infraId;
      return this;
    }

    /**
     * With host name builder.
     *
     * @param hostName the host name
     * @return the builder
     */
    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With os type builder.
     *
     * @param osType the os type
     * @return the builder
     */
    public Builder withOsType(String osType) {
      this.osType = osType;
      return this;
    }

    /**
     * With host conn attr builder.
     *
     * @param hostConnAttr the host conn attr
     * @return the builder
     */
    public Builder withHostConnAttr(String hostConnAttr) {
      this.hostConnAttr = hostConnAttr;
      return this;
    }

    public Builder withHostConnAttr(SettingAttribute hostConnAttr) {
      this.hostConnAttr = hostConnAttr.getUuid();
      return this;
    }

    /**
     * With bastion conn attr builder.
     *
     * @param bastionConnAttr the bastion conn attr
     * @return the builder
     */
    public Builder withBastionConnAttr(String bastionConnAttr) {
      this.bastionConnAttr = bastionConnAttr;
      return this;
    }

    public Builder withBastionConnAttr(SettingAttribute bastionConnAttr) {
      this.bastionConnAttr = bastionConnAttr.getUuid();
      return this;
    }

    /**
     * With config tag builder.
     *
     * @param configTag the config tag
     * @return the builder
     */
    public Builder withConfigTag(Tag configTag) {
      this.configTag = configTag;
      return this;
    }

    /**
     * With config files builder.
     *
     * @param configFiles the config files
     * @return the builder
     */
    public Builder withConfigFiles(List<ConfigFile> configFiles) {
      this.configFiles = configFiles;
      return this;
    }

    /**
     * With host connection credential builder.
     *
     * @param hostConnectionCredential the host connection credential
     * @return the builder
     */
    public Builder withHostConnectionCredential(HostConnectionCredential hostConnectionCredential) {
      return this;
    }

    /**
     * With host names builder.
     *
     * @param hostNames the host names
     * @return the builder
     */
    public Builder withHostNames(List<String> hostNames) {
      this.hostNames = hostNames;
      return this;
    }

    /**
     * With service templates builder.
     *
     * @param serviceTemplates the service templates
     * @return the builder
     */
    public Builder withServiceTemplates(List<ServiceTemplate> serviceTemplates) {
      this.serviceTemplates = serviceTemplates;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aHost()
          .withInfraId(infraId)
          .withHostName(hostName)
          .withOsType(osType)
          .withHostConnAttr(hostConnAttr)
          .withBastionConnAttr(bastionConnAttr)
          .withConfigTag(configTag)
          .withConfigFiles(configFiles)
          .withHostNames(hostNames)
          .withServiceTemplates(serviceTemplates)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build host.
     *
     * @return the host
     */
    public Host build() {
      Host host = new Host();
      host.setInfraId(infraId);
      host.setHostName(hostName);
      host.setOsType(osType);
      host.setHostConnAttr(hostConnAttr);
      host.setBastionConnAttr(bastionConnAttr);
      host.setConfigTag(configTag);
      host.setConfigFiles(configFiles);
      host.setHostNames(hostNames);
      host.setServiceTemplates(serviceTemplates);
      host.setUuid(uuid);
      host.setAppId(appId);
      host.setCreatedBy(createdBy);
      host.setCreatedAt(createdAt);
      host.setLastUpdatedBy(lastUpdatedBy);
      host.setLastUpdatedAt(lastUpdatedAt);
      host.setActive(active);
      return host;
    }
  }
}
