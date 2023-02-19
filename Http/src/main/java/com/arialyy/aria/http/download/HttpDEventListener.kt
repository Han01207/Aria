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
package com.arialyy.aria.http.download

import com.arialyy.aria.core.DuaContext
import com.arialyy.aria.core.inf.IEntity
import com.arialyy.aria.core.listener.AbsEventListener
import com.arialyy.aria.core.listener.ISchedulers
import com.arialyy.aria.core.task.DownloadTask
import com.arialyy.aria.core.task.TaskCachePool
import com.arialyy.aria.exception.AriaException
import com.arialyy.aria.orm.entity.DEntity
import com.arialyy.aria.util.BlockUtil
import com.arialyy.aria.util.FileUri
import com.arialyy.aria.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

class HttpDEventListener(task: DownloadTask) : AbsEventListener(task) {

  /**
   * merge block file, if merge fail call [onFail]
   */
  override fun onComplete() {
    val b = BlockUtil.mergeFile(task.taskState.taskRecord)

    if (!b) {
      Timber.e("merge block fail")
      onFail(false, AriaException("merge block fail"))
      return
    }
    handleSpeed(0)
    sendInState2Target(ISchedulers.COMPLETE)
    saveData(IEntity.STATE_COMPLETE, task.taskState.fileSize)
  }

  /**
   * 1.remove all block
   * 2.remove record
   */
  override fun handleCancel() {
    val file = FileUri.getPathByUri(task.taskState.taskRecord.filePath)?.let { File(it) }
    file?.parentFile?.let {
      FileUtils.deleteDir(it)
    }

    DuaContext.duaScope.launch(Dispatchers.IO) {
      val entity = TaskCachePool.getEntity(taskId = task.taskId)
      val db = DuaContext.getServiceManager().getDbService().getDuaDb()
      db.getRecordDao().deleteTaskRecord(task.taskState.taskRecord)
      db.getDEntityDao().delete(entity as DEntity)
    }
  }

}