package software.wings.core.ssh.executors;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;

import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 2/8/16.
 */
public class SshSessionConfig {
  @NotEmpty private String appId;
  @NotEmpty private ExecutorType executorType;
  @NotEmpty private String executionId;
  private Integer sshConnectionTimeout = 5 * 60 * 1000; // 5 secs
  private Integer sshSessionTimeout = 10 * 60 * 1000; // 10 minutes
  private Integer retryInterval;
  @NotEmpty private String host;
  private Integer port = 22;
  private String userName;
  private String password;
  private String key;
  private String keyPassphrase;
  private String sudoAppName;
  private String sudoAppPassword;
  private SshSessionConfig bastionHostConfig;

  /**
   * Gets app id.
   *
   * @return the app id
   */
  public String getAppId() {
    return appId;
  }

  /**
   * Sets app id.
   *
   * @param appId the app id
   */
  public void setAppId(String appId) {
    this.appId = appId;
  }

  /**
   * Gets executor type.
   *
   * @return the executor type
   */
  public ExecutorType getExecutorType() {
    return executorType;
  }

  /**
   * Sets executor type.
   *
   * @param executorType the executor type
   */
  public void setExecutorType(ExecutorType executorType) {
    this.executorType = executorType;
  }

  /**
   * Gets execution id.
   *
   * @return the execution id
   */
  public String getExecutionId() {
    return executionId;
  }

  /**
   * Sets execution id.
   *
   * @param executionId the execution id
   */
  public void setExecutionId(String executionId) {
    this.executionId = executionId;
  }

  /**
   * Gets ssh connection timeout.
   *
   * @return the ssh connection timeout
   */
  public Integer getSshConnectionTimeout() {
    return sshConnectionTimeout;
  }

  /**
   * Sets ssh connection timeout.
   *
   * @param sshConnectionTimeout the ssh connection timeout
   */
  public void setSshConnectionTimeout(Integer sshConnectionTimeout) {
    this.sshConnectionTimeout = sshConnectionTimeout;
  }

  /**
   * Gets ssh session timeout.
   *
   * @return the ssh session timeout
   */
  public Integer getSshSessionTimeout() {
    return sshSessionTimeout;
  }

  /**
   * Sets ssh session timeout.
   *
   * @param sshSessionTimeout the ssh session timeout
   */
  public void setSshSessionTimeout(Integer sshSessionTimeout) {
    this.sshSessionTimeout = sshSessionTimeout;
  }

  /**
   * Gets retry interval.
   *
   * @return the retry interval
   */
  public Integer getRetryInterval() {
    return retryInterval;
  }

  /**
   * Sets retry interval.
   *
   * @param retryInterval the retry interval
   */
  public void setRetryInterval(Integer retryInterval) {
    this.retryInterval = retryInterval;
  }

  /**
   * Gets host.
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets host.
   *
   * @param host the host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets port.
   *
   * @return the port
   */
  public Integer getPort() {
    return port;
  }

  /**
   * Sets port.
   *
   * @param port the port
   */
  public void setPort(Integer port) {
    this.port = port;
  }

  /**
   * Gets user name.
   *
   * @return the user name
   */
  public String getUserName() {
    return userName;
  }

  /**
   * Sets user name.
   *
   * @param userName the user name
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * Gets password.
   *
   * @return the password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Sets password.
   *
   * @param password the password
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * Gets key.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets key.
   *
   * @param key the key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Gets key passphrase.
   *
   * @return the key passphrase
   */
  public String getKeyPassphrase() {
    return keyPassphrase;
  }

  /**
   * Sets key passphrase.
   *
   * @param keyPassphrase the key passphrase
   */
  public void setKeyPassphrase(String keyPassphrase) {
    this.keyPassphrase = keyPassphrase;
  }

  /**
   * Gets sudo app name.
   *
   * @return the sudo app name
   */
  public String getSudoAppName() {
    return sudoAppName;
  }

  /**
   * Sets sudo app name.
   *
   * @param sudoAppName the sudo app name
   */
  public void setSudoAppName(String sudoAppName) {
    this.sudoAppName = sudoAppName;
  }

  /**
   * Gets sudo app password.
   *
   * @return the sudo app password
   */
  public String getSudoAppPassword() {
    return sudoAppPassword;
  }

  /**
   * Sets sudo app password.
   *
   * @param sudoAppPassword the sudo app password
   */
  public void setSudoAppPassword(String sudoAppPassword) {
    this.sudoAppPassword = sudoAppPassword;
  }

  /**
   * Gets bastion host config.
   *
   * @return the bastion host config
   */
  public SshSessionConfig getBastionHostConfig() {
    return bastionHostConfig;
  }

  /**
   * Sets bastion host config.
   *
   * @param bastionHostConfig the bastion host config
   */
  public void setBastionHostConfig(SshSessionConfig bastionHostConfig) {
    this.bastionHostConfig = bastionHostConfig;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(appId, executorType, executionId, sshConnectionTimeout, sshSessionTimeout, retryInterval, host,
        port, userName, password, key, keyPassphrase, sudoAppName, sudoAppPassword, bastionHostConfig);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final SshSessionConfig other = (SshSessionConfig) obj;
    return Objects.equals(this.appId, other.appId) && Objects.equals(this.executorType, other.executorType)
        && Objects.equals(this.executionId, other.executionId)
        && Objects.equals(this.sshConnectionTimeout, other.sshConnectionTimeout)
        && Objects.equals(this.sshSessionTimeout, other.sshSessionTimeout)
        && Objects.equals(this.retryInterval, other.retryInterval) && Objects.equals(this.host, other.host)
        && Objects.equals(this.port, other.port) && Objects.equals(this.userName, other.userName)
        && Objects.equals(this.password, other.password) && Objects.equals(this.key, other.key)
        && Objects.equals(this.keyPassphrase, other.keyPassphrase)
        && Objects.equals(this.sudoAppName, other.sudoAppName)
        && Objects.equals(this.sudoAppPassword, other.sudoAppPassword)
        && Objects.equals(this.bastionHostConfig, other.bastionHostConfig);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("appId", appId)
        .add("executorType", executorType)
        .add("executionId", executionId)
        .add("sshConnectionTimeout", sshConnectionTimeout)
        .add("sshSessionTimeout", sshSessionTimeout)
        .add("retryInterval", retryInterval)
        .add("host", host)
        .add("port", port)
        .add("userName", userName)
        .add("password", password)
        .add("key", key)
        .add("keyPassphrase", keyPassphrase)
        .add("sudoAppName", sudoAppName)
        .add("sudoAppPassword", sudoAppPassword)
        .add("bastionHostConfig", bastionHostConfig)
        .toString();
  }

  /**
   * The type Ssh session config builder.
   */
  public static final class SshSessionConfigBuilder {
    private String appId;
    private ExecutorType executorType;
    private String executionId;
    private Integer sshConnectionTimeout = 5 * 60 * 1000; // 5 secs
    private Integer sshSessionTimeout = 10 * 60 * 1000; // 10 minutes
    private Integer retryInterval;
    private String host;
    private Integer port = 22;
    private String userName;
    private String password;
    private String key;
    private String keyPassphrase;
    private String sudoAppName;
    private String sudoAppPassword;
    private SshSessionConfig bastionHostConfig;

    private SshSessionConfigBuilder() {}

    /**
     * A ssh session config ssh session config builder.
     *
     * @return the ssh session config builder
     */
    public static SshSessionConfigBuilder aSshSessionConfig() {
      return new SshSessionConfigBuilder();
    }

    /**
     * With app id ssh session config builder.
     *
     * @param appId the app id
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With executor type ssh session config builder.
     *
     * @param executorType the executor type
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withExecutorType(ExecutorType executorType) {
      this.executorType = executorType;
      return this;
    }

    /**
     * With execution id ssh session config builder.
     *
     * @param executionId the execution id
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withExecutionId(String executionId) {
      this.executionId = executionId;
      return this;
    }

    /**
     * With ssh connection timeout ssh session config builder.
     *
     * @param sshConnectionTimeout the ssh connection timeout
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withSshConnectionTimeout(Integer sshConnectionTimeout) {
      this.sshConnectionTimeout = sshConnectionTimeout;
      return this;
    }

    /**
     * With ssh session timeout ssh session config builder.
     *
     * @param sshSessionTimeout the ssh session timeout
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withSshSessionTimeout(Integer sshSessionTimeout) {
      this.sshSessionTimeout = sshSessionTimeout;
      return this;
    }

    /**
     * With retry interval ssh session config builder.
     *
     * @param retryInterval the retry interval
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withRetryInterval(Integer retryInterval) {
      this.retryInterval = retryInterval;
      return this;
    }

    /**
     * With host ssh session config builder.
     *
     * @param host the host
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withHost(String host) {
      this.host = host;
      return this;
    }

    /**
     * With port ssh session config builder.
     *
     * @param port the port
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withPort(Integer port) {
      this.port = port;
      return this;
    }

    /**
     * With user name ssh session config builder.
     *
     * @param userName the user name
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withUserName(String userName) {
      this.userName = userName;
      return this;
    }

    /**
     * With password ssh session config builder.
     *
     * @param password the password
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withPassword(String password) {
      this.password = password;
      return this;
    }

    /**
     * With key ssh session config builder.
     *
     * @param key the key
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withKey(String key) {
      this.key = key;
      return this;
    }

    /**
     * With key passphrase ssh session config builder.
     *
     * @param keyPassphrase the key passphrase
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withKeyPassphrase(String keyPassphrase) {
      this.keyPassphrase = keyPassphrase;
      return this;
    }

    /**
     * With sudo app name ssh session config builder.
     *
     * @param sudoAppName the sudo app name
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withSudoAppName(String sudoAppName) {
      this.sudoAppName = sudoAppName;
      return this;
    }

    /**
     * With sudo app password ssh session config builder.
     *
     * @param sudoAppPassword the sudo app password
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withSudoAppPassword(String sudoAppPassword) {
      this.sudoAppPassword = sudoAppPassword;
      return this;
    }

    /**
     * With bastion host config ssh session config builder.
     *
     * @param bastionHostConfig the bastion host config
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder withBastionHostConfig(SshSessionConfig bastionHostConfig) {
      this.bastionHostConfig = bastionHostConfig;
      return this;
    }

    /**
     * But ssh session config builder.
     *
     * @return the ssh session config builder
     */
    public SshSessionConfigBuilder but() {
      return aSshSessionConfig()
          .withAppId(appId)
          .withExecutorType(executorType)
          .withExecutionId(executionId)
          .withSshConnectionTimeout(sshConnectionTimeout)
          .withSshSessionTimeout(sshSessionTimeout)
          .withRetryInterval(retryInterval)
          .withHost(host)
          .withPort(port)
          .withUserName(userName)
          .withPassword(password)
          .withKey(key)
          .withKeyPassphrase(keyPassphrase)
          .withSudoAppName(sudoAppName)
          .withSudoAppPassword(sudoAppPassword)
          .withBastionHostConfig(bastionHostConfig);
    }

    /**
     * Build ssh session config.
     *
     * @return the ssh session config
     */
    public SshSessionConfig build() {
      SshSessionConfig sshSessionConfig = new SshSessionConfig();
      sshSessionConfig.setAppId(appId);
      sshSessionConfig.setExecutorType(executorType);
      sshSessionConfig.setExecutionId(executionId);
      sshSessionConfig.setSshConnectionTimeout(sshConnectionTimeout);
      sshSessionConfig.setSshSessionTimeout(sshSessionTimeout);
      sshSessionConfig.setRetryInterval(retryInterval);
      sshSessionConfig.setHost(host);
      sshSessionConfig.setPort(port);
      sshSessionConfig.setUserName(userName);
      sshSessionConfig.setPassword(password);
      sshSessionConfig.setKey(key);
      sshSessionConfig.setKeyPassphrase(keyPassphrase);
      sshSessionConfig.setSudoAppName(sudoAppName);
      sshSessionConfig.setSudoAppPassword(sudoAppPassword);
      sshSessionConfig.setBastionHostConfig(bastionHostConfig);
      return sshSessionConfig;
    }
  }
}
