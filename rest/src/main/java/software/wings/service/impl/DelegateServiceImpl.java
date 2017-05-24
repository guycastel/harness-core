package software.wings.service.impl;

import static com.google.common.collect.Iterables.isEmpty;
import static freemarker.template.Configuration.VERSION_2_3_23;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

import com.github.zafarkhaja.semver.Version;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IQueue;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.apache.commons.compress.archivers.zip.AsiExtraField;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.fluent.Request;
import org.atmosphere.cpr.BroadcasterFactory;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.ErrorCode;
import software.wings.beans.Event.Type;
import software.wings.common.UUIDGenerator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.DelegateService;
import software.wings.utils.CacheHelper;
import software.wings.waitnotify.NotifyResponseData;
import software.wings.waitnotify.WaitNotifyEngine;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.cache.Caching;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 11/28/16.
 */
@Singleton
public class DelegateServiceImpl implements DelegateService {
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);
  public static final int SYNC_CALL_TIMEOUT_INTERVAL = 30000;

  static {
    cfg.setTemplateLoader(new ClassTemplateLoader(DelegateServiceImpl.class, "/delegatetemplates"));
  }

  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private ExecutorService executorService;
  @Inject private AccountService accountService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private EventEmitter eventEmitter;
  @Inject private HazelcastInstance hazelcastInstance;
  @Inject private BroadcasterFactory broadcasterFactory;

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return wingsPersistence.query(Delegate.class, pageRequest);
  }

  @Override
  public Delegate get(String accountId, String delegateId) {
    return wingsPersistence.get(
        Delegate.class, aPageRequest().addFilter("accountId", EQ, accountId).addFilter(ID_KEY, EQ, delegateId).build());
  }

  @Override
  public Delegate update(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, "status", delegate.getStatus());
    setUnset(updateOperations, "lastHeartBeat", delegate.getLastHeartBeat());
    setUnset(updateOperations, "connected", delegate.isConnected());
    setUnset(updateOperations, "supportedTaskTypes", delegate.getSupportedTaskTypes());

    logger.debug("Updating delegate : {}", delegate.getUuid());
    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .field("accountId")
                                .equal(delegate.getAccountId())
                                .field(ID_KEY)
                                .equal(delegate.getUuid()),
        updateOperations);

    // Touch currently executing tasks.
    if (delegate.getCurrentlyExecutingDelegateTasks() != null
        && !isEmpty(delegate.getCurrentlyExecutingDelegateTasks())) {
      logger.debug("Updating tasks");

      Query<DelegateTask> delegateTaskQuery =
          wingsPersistence.createQuery(DelegateTask.class)
              .field("accountId")
              .equal(delegate.getAccountId())
              .field("delegateId")
              .equal(delegate.getUuid())
              .field("status")
              .equal(DelegateTask.Status.STARTED)
              .field("lastUpdatedAt")
              .lessThan(System.currentTimeMillis())
              .field(ID_KEY)
              .in(delegate.getCurrentlyExecutingDelegateTasks().stream().map(DelegateTask::getUuid).collect(toList()));
      wingsPersistence.update(delegateTaskQuery, wingsPersistence.createUpdateOperations(DelegateTask.class));
    }

    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
    Delegate updatedDelegate = get(delegate.getAccountId(), delegate.getUuid());
    return updatedDelegate;
  }

  @Override
  public Delegate checkForUpgrade(String accountId, String delegateId, String version, String managerHost)
      throws IOException, TemplateException {
    Delegate delegate = get(accountId, delegateId);
    logger.debug("Checking delegate for upgrade: {}", delegate.getUuid());

    String latestVersion = null;
    try {
      latestVersion = substringBefore(Request.Get(mainConfiguration.getDelegateMetadataUrl())
                                          .connectTimeout(1000)
                                          .socketTimeout(1000)
                                          .execute()
                                          .returnContent()
                                          .asString(),
          " ");
    } catch (IOException e) {
      logger.error("Unable to fetch delegate version information ", e);
      latestVersion = "0.0.0";
    }
    boolean doUpgrade = Version.valueOf(version).lessThan(Version.valueOf(latestVersion));

    delegate.setDoUpgrade(doUpgrade);
    if (doUpgrade) {
      logger.debug("Upgrading delegate to version: {}", latestVersion);
      delegate.setVersion(latestVersion);
      try (StringWriter stringWriter = new StringWriter()) {
        cfg.getTemplate("upgrade.sh.ftl")
            .process(ImmutableMap.of("delegateMetadataUrl", mainConfiguration.getDelegateMetadataUrl(), "accountId",
                         accountId, "managerHostAndPort", managerHost),
                stringWriter);
        delegate.setUpgradeScript(stringWriter.toString());
      }
    }
    return delegate;
  }

  @Override
  public Delegate add(Delegate delegate) {
    logger.debug("Adding delegate: {}", delegate.getUuid());
    delegate.setAppId(GLOBAL_APP_ID);
    Delegate savedDelegate = wingsPersistence.saveAndGet(Delegate.class, delegate);
    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
    return delegate;
  }

  @Override
  public void delete(String accountId, String delegateId) {
    logger.debug("Deleting delegate: {}", delegateId);
    wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class)
                                .field("accountId")
                                .equal(accountId)
                                .field(ID_KEY)
                                .equal(delegateId));
  }

  @Override
  public Delegate register(Delegate delegate) {
    logger.info("registering delegate: " + delegate);
    Delegate existingDelegate = wingsPersistence.get(Delegate.class,
        aPageRequest()
            .addFilter("ip", EQ, delegate.getIp())
            .addFilter("hostName", EQ, delegate.getHostName())
            .addFilter("accountId", EQ, delegate.getAccountId())
            .build());

    if (existingDelegate == null) {
      logger.debug("No existing delegate, adding: {}", delegate.getUuid());
      return add(delegate);
    } else {
      logger.debug("Delegate exists, updating: {}", delegate.getUuid());
      delegate.setUuid(existingDelegate.getUuid());
      delegate.setStatus(existingDelegate.getStatus() == Status.DISABLED ? Status.DISABLED : delegate.getStatus());
      return update(delegate);
    }
  }

  @Override
  public String queueTask(DelegateTask task) {
    logger.debug("Queueing task: {}", task.getUuid());
    wingsPersistence.save(task);
    broadcasterFactory.lookup("/stream/delegate/" + task.getAccountId(), true).broadcast(task);
    return task.getUuid();
  }

  @Override
  public <T extends NotifyResponseData> T executeTask(DelegateTask task) throws InterruptedException {
    String taskId = UUIDGenerator.getUuid();
    task.setQueueName(taskId);
    task.setUuid(taskId);
    IQueue<T> topic = hazelcastInstance.getQueue(taskId);
    CacheHelper.getCache("delegateSyncCache", String.class, DelegateTask.class).put(taskId, task);
    broadcasterFactory.lookup("/stream/delegate/" + task.getAccountId(), true).broadcast(task);
    logger.info("Broadcast new task: {}", taskId);
    T responseData = topic.poll(SYNC_CALL_TIMEOUT_INTERVAL, TimeUnit.MILLISECONDS);
    if (responseData == null) {
      throw new WingsException(ErrorCode.REQUEST_TIMEOUT, "name", "Harness Bot");
    }
    return responseData;
  }

  @Override
  public PageResponse<DelegateTask> getDelegateTasks(String accountId, String delegateId) {
    return wingsPersistence.query(DelegateTask.class, aPageRequest().addFilter("accountId", EQ, accountId).build());
  }

  @Override
  public DelegateTask acquireDelegateTask(String accountId, String delegateId, String taskId) {
    logger.debug("Acquiring delegate task {} for delegate {}", taskId, delegateId);
    DelegateTask delegateTask = CacheHelper.getCache("delegateSyncCache", String.class, DelegateTask.class).get(taskId);
    if (delegateTask == null) {
      // Async
      logger.debug("Delegate task from cache is null for task {}", taskId);
      Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                      .field("accountId")
                                      .equal(accountId)
                                      .field("status")
                                      .equal(DelegateTask.Status.QUEUED)
                                      .field("delegateId")
                                      .doesNotExist()
                                      .field(ID_KEY)
                                      .equal(taskId);
      UpdateOperations<DelegateTask> updateOperations =
          wingsPersistence.createUpdateOperations(DelegateTask.class).set("delegateId", delegateId);
      delegateTask = wingsPersistence.getDatastore().findAndModify(query, updateOperations);
    } else {
      // Sync
      logger.debug("Delegate task from cache: {}", delegateTask.getUuid());
      if (isBlank(delegateTask.getDelegateId())) {
        logger.debug("Assigning task {} to delegate {}", taskId, delegateId);
        delegateTask.setDelegateId(delegateId);
        Caching.getCache("delegateSyncCache", String.class, DelegateTask.class).put(taskId, delegateTask);
      } else {
        logger.debug("Task {} is already assigned to delegate {}", taskId, delegateTask.getDelegateId());
        delegateTask = null;
      }
    }
    return delegateTask;
  }

  @Override
  public DelegateTask startDelegateTask(String accountId, String delegateId, String taskId) {
    logger.debug("Starting task {} with delegate {}", taskId, delegateId);
    DelegateTask delegateTask = CacheHelper.getCache("delegateSyncCache", String.class, DelegateTask.class).get(taskId);
    if (delegateTask == null) {
      logger.debug("Delegate task from cache is null for task {}", taskId);
      Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                      .field("accountId")
                                      .equal(accountId)
                                      .field("status")
                                      .equal(DelegateTask.Status.QUEUED)
                                      .field("delegateId")
                                      .equal(delegateId)
                                      .field(ID_KEY)
                                      .equal(taskId);
      UpdateOperations<DelegateTask> updateOperations =
          wingsPersistence.createUpdateOperations(DelegateTask.class).set("status", DelegateTask.Status.STARTED);
      delegateTask = wingsPersistence.getDatastore().findAndModify(query, updateOperations);
    } else {
      logger.debug("Delegate task from cache: {}", delegateTask.getUuid());
      Caching.getCache("delegateSyncCache", String.class, DelegateTask.class).remove(taskId);
    }
    return delegateTask;
  }

  @Override
  public void processDelegateResponse(DelegateTaskResponse response) {
    if (isNotBlank(response.getTask().getWaitId())) {
      DelegateTask delegateTask = wingsPersistence.get(DelegateTask.class,
          aPageRequest()
              .addFilter("accountId", EQ, response.getAccountId())
              .addFilter(ID_KEY, EQ, response.getTask().getUuid())
              .build());
      String waitId = delegateTask.getWaitId();
      waitNotifyEngine.notify(waitId, response.getResponse());
      wingsPersistence.delete(wingsPersistence.createQuery(DelegateTask.class)
                                  .field("accountId")
                                  .equal(response.getAccountId())
                                  .field(ID_KEY)
                                  .equal(delegateTask.getUuid()));
    } else {
      String topicName = response.getTask().getQueueName();
      // do the haze
      IQueue<NotifyResponseData> topic = hazelcastInstance.getQueue(topicName);
      topic.offer(response.getResponse());
    }
  }

  @Override
  public File download(String managerHost, String accountId) throws IOException, TemplateException {
    File delegateFile = File.createTempFile("wings-bot", ".zip");
    File run = File.createTempFile("run", ".sh");
    File stop = File.createTempFile("stop", ".sh");
    File readme = File.createTempFile("README", ".txt");

    ZipArchiveOutputStream out = new ZipArchiveOutputStream(delegateFile);
    out.putArchiveEntry(new ZipArchiveEntry("wings-bot/"));
    out.closeArchiveEntry();
    Account account = accountService.get(accountId);

    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(run))) {
      cfg.getTemplate("run.sh.ftl")
          .process(ImmutableMap.of("delegateMetadataUrl", mainConfiguration.getDelegateMetadataUrl(), "accountId",
                       accountId, "accountSecret", account.getAccountKey(), "managerHostAndPort", managerHost),
              fileWriter);
    }
    run = new File(run.getAbsolutePath());
    ZipArchiveEntry runZipArchiveEntry = new ZipArchiveEntry(run, "wings-bot/run.sh");
    runZipArchiveEntry.setUnixMode(0755 << 16L);
    AsiExtraField permissions = new AsiExtraField();
    permissions.setMode(0755);
    runZipArchiveEntry.addExtraField(permissions);
    out.putArchiveEntry(runZipArchiveEntry);
    try (FileInputStream fis = new FileInputStream(run)) {
      IOUtils.copy(fis, out);
    }
    out.closeArchiveEntry();

    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(stop))) {
      cfg.getTemplate("stop.sh.ftl").process(null, fileWriter);
    }
    stop = new File(stop.getAbsolutePath());
    ZipArchiveEntry stopZipArchiveEntry = new ZipArchiveEntry(stop, "wings-bot/stop.sh");
    stopZipArchiveEntry.setUnixMode(0755 << 16L);
    permissions = new AsiExtraField();
    permissions.setMode(0755);
    stopZipArchiveEntry.addExtraField(permissions);
    out.putArchiveEntry(stopZipArchiveEntry);
    try (FileInputStream fis = new FileInputStream(stop)) {
      IOUtils.copy(fis, out);
    }
    out.closeArchiveEntry();

    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(readme))) {
      cfg.getTemplate("readme.txt.ftl").process(null, fileWriter);
    }
    readme = new File(readme.getAbsolutePath());
    ZipArchiveEntry readmeZipArchiveEntry = new ZipArchiveEntry(readme, "wings-bot/README.txt");
    out.putArchiveEntry(readmeZipArchiveEntry);
    try (FileInputStream fis = new FileInputStream(readme)) {
      IOUtils.copy(fis, out);
    }
    out.closeArchiveEntry();

    out.close();
    return delegateFile;
  }

  @Override
  public boolean filter(String delegateId, DelegateTask task) {
    boolean qualifies = false;
    Delegate delegate = wingsPersistence.get(Delegate.class,
        aPageRequest()
            .addFilter("accountId", IN, task.getAccountId(), GLOBAL_ACCOUNT_ID)
            .addFilter(ID_KEY, EQ, delegateId)
            .addFilter("status", EQ, Status.ENABLED)
            .addFilter("supportedTaskTypes", EQ, task.getTaskType())
            .build());

    if (delegate != null) {
      qualifies = true;
    }

    return qualifies;
  }

  @Override
  public boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent) {
    return wingsPersistence.get(DelegateTask.class,
               aPageRequest()
                   .addFilter(ID_KEY, EQ, taskAbortEvent.getDelegateTaskId())
                   .addFilter("delegateId", EQ, delegateId)
                   .build())
        != null;
  }

  @Override
  public void abortTask(String accountId, String delegateTaskId) {
    logger.debug("Aborting delegate task {}", delegateTaskId);
    DelegateTask updatedTask =
        wingsPersistence.getDatastore().findAndModify(wingsPersistence.createQuery(DelegateTask.class)
                                                          .field(ID_KEY)
                                                          .equal(delegateTaskId)
                                                          .field("accountId")
                                                          .equal(accountId)
                                                          .field("status")
                                                          .equal(DelegateTask.Status.QUEUED),
            wingsPersistence.createUpdateOperations(DelegateTask.class)
                .set("status", DelegateTask.Status.ABORTED)
                .unset("delegateId"));

    if (updatedTask == null) {
      logger.debug("Updated task null");
      broadcasterFactory.lookup("/stream/delegate/" + accountId, true)
          .broadcast(aDelegateTaskAbortEvent().withAccountId(accountId).withDelegateTaskId(delegateTaskId).build());
    }
  }
}
