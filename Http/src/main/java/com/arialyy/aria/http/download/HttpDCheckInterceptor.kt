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

import android.net.Uri
import com.arialyy.aria.core.DuaContext
import com.arialyy.aria.core.task.ITaskInterceptor
import com.arialyy.aria.core.task.TaskCachePool
import com.arialyy.aria.core.task.TaskChain
import com.arialyy.aria.core.task.TaskResp
import com.arialyy.aria.http.HttpTaskOption
import com.arialyy.aria.orm.entity.DEntity

/**
 * @Author laoyuyu
 * @Description
 * @Date 8:31 PM 2023/3/6
 **/
class HttpDCheckInterceptor : ITaskInterceptor {

  /**
   * find DEntity, if that not exist, create and save it
   */
  private suspend fun findDEntityByUrl(option: HttpTaskOption): DEntity {
    val dao = DuaContext.getServiceManager().getDbService().getDuaDb().getDEntityDao()
    val de = dao.queryDEntityByUrl(option.sourUrl ?: "")
    if (de != null) {
      return de
    }
    // Set the file save path only after getting the file name
    val newDe = DEntity(
      sourceUrl = option.sourUrl!!,
      savePath = Uri.EMPTY,
    )
    dao.insert(newDe)
    return newDe
  }

  override suspend fun interceptor(chain: TaskChain): TaskResp {
    val option = chain.getTask().getTaskOption(HttpTaskOption::class.java)
    val dEntity = findDEntityByUrl(option)
    chain.getTask().taskState.setEntity(dEntity)
    TaskCachePool.putEntity(chain.getTask().taskId, dEntity)
    return chain.proceed(chain.getTask())
  }
}