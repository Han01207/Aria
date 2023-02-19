/*
 * Copyright (C) 2016 AriaLyy(https://github.com/AriaLyy/Aria)
 *
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
 */
package com.arialyy.aria.core.listener;

import android.os.Handler;
import com.arialyy.aria.core.AriaConfig;
import com.arialyy.aria.core.DuaContext;
import com.arialyy.aria.core.inf.IEntity;
import com.arialyy.aria.core.inf.TaskSchedulerType;
import com.arialyy.aria.core.task.ITask;
import com.arialyy.aria.core.task.TaskCachePool;
import com.arialyy.aria.core.task.TaskState;
import com.arialyy.aria.exception.AriaException;
import com.arialyy.aria.util.ALog;
import com.arialyy.aria.util.ErrorHelp;
import timber.log.Timber;

public abstract class AbsEventListener implements IEventListener {
  static final int RUN_SAVE_INTERVAL = 5 * 1000;  //5s保存一次下载中的进度
  protected Handler outHandler;
  private long mLastLen;   //上一次发送长度
  private boolean isFirst = true;
  private final ITask mTask;
  long mLastSaveTime;
  private final long mUpdateInterval;

  /**
   * 处理任务取消
   */
  protected abstract void handleCancel();

  protected AbsEventListener(ITask task) {
    this.outHandler = DuaContext.INSTANCE.getServiceManager().getSchedulerHandler();
    mTask = task;
    mUpdateInterval = AriaConfig.getInstance().getCConfig().getUpdateInterval();
    mLastLen = task.getTaskState().getCurProgress();
    mLastSaveTime = System.currentTimeMillis();
  }

  protected ITask getTask() {
    return mTask;
  }

  @Override public void onPre() {
    saveData(IEntity.STATE_PRE, -1);
    sendInState2Target(ISchedulers.PRE);
  }

  @Override public void onStart(long startLocation) {
    saveData(IEntity.STATE_RUNNING, startLocation);
    sendInState2Target(ISchedulers.START);
  }

  @Override public void onResume(long resumeLocation) {
    saveData(IEntity.STATE_RUNNING, resumeLocation);
    sendInState2Target(ISchedulers.RESUME);
  }

  @Override public void onProgress(long currentLocation) {
    mTask.getTaskState().setCurProgress(currentLocation);
    long speed = currentLocation - mLastLen;
    if (isFirst) {
      speed = 0;
      isFirst = false;
    }
    handleSpeed(speed);
    sendInState2Target(ISchedulers.RUNNING);
    if (System.currentTimeMillis() - mLastSaveTime >= RUN_SAVE_INTERVAL) {
      saveData(IEntity.STATE_RUNNING, currentLocation);
      mLastSaveTime = System.currentTimeMillis();
    }

    mLastLen = currentLocation;
  }

  @Override public void onStop(long stopLocation) {
    saveData(mTask.getSchedulerType() == TaskSchedulerType.TYPE_STOP_AND_WAIT ? IEntity.STATE_WAIT
        : IEntity.STATE_STOP, stopLocation);
    handleSpeed(0);
    sendInState2Target(ISchedulers.STOP);
  }

  @Override public void onCancel() {
    saveData(IEntity.STATE_CANCEL, -1);
    handleSpeed(0);
    if (mTask.getSchedulerType() != TaskSchedulerType.TYPE_CANCEL_AND_NOT_NOTIFY) {
      Timber.d("remove task success");
      sendInState2Target(ISchedulers.CANCEL);
    }
  }

  @Override public void onFail(boolean needRetry, AriaException e) {
    TaskState ts = mTask.getTaskState();
    int taskFailNum = ts.getFailNum();
    ts.setFailNum(taskFailNum + 1);
    ts.setNeedRetry(needRetry);

    saveData(IEntity.STATE_FAIL, ts.getCurProgress());
    handleSpeed(0);
    sendInState2Target(ISchedulers.FAIL);
    if (e != null) {
      String error = ALog.getExceptionString(e);
      Timber.e(error);
      ErrorHelp.saveError(e.getMessage(), error);
    }
  }

  protected void handleSpeed(long speed) {
    if (mUpdateInterval != 1000) {
      speed = speed * 1000 / mUpdateInterval;
    }
    mTask.getTaskState().setSpeed(speed);
  }

  /**
   * 将任务状态发送给下载器
   *
   * @param state {@link ISchedulers#START}
   */
  protected void sendInState2Target(int state) {
    outHandler.obtainMessage(state, mTask).sendToTarget();
  }

  protected void saveData(int state, long location) {
    TaskState ts = mTask.getTaskState();
    ts.setState(state);
    ts.setCurProgress(location);

    if (state == IEntity.STATE_CANCEL) {
      handleCancel();
      TaskCachePool.INSTANCE.removeTask(getTask().getTaskId());
      return;
    }
    if (state == IEntity.STATE_COMPLETE || state == IEntity.STATE_STOP) {
      TaskCachePool.INSTANCE.removeTask(getTask().getTaskId());
    }

    TaskCachePool.INSTANCE.updateState(mTask.getTaskId(), state, location);
  }
}
