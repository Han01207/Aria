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
package com.arialyy.dua.group

import android.net.Uri
import com.arialyy.aria.core.command.StartCmd
import com.arialyy.aria.core.task.ITaskInterceptor
import com.arialyy.aria.core.task.TaskCachePool
import com.arialyy.aria.http.HttpBaseStartController
import com.arialyy.aria.http.HttpOption
import com.arialyy.aria.http.HttpUtil
import com.arialyy.aria.util.FileUtils
import timber.log.Timber

/**
 * @Author laoyuyu
 * @Description
 * @Date 7:47 PM 2023/3/6
 **/
class HttpDGStartController(target: Any, val savePath: Uri) : HttpBaseStartController(target) {

  private val optionAdapter = HttpDGOptionAdapter()

  init {
    httpTaskOption.taskOptionAdapter = optionAdapter
    httpTaskOption.savePathDir = savePath
  }

  override fun setTaskInterceptor(taskInterceptor: ITaskInterceptor): HttpDGStartController {
    return super.setTaskInterceptor(taskInterceptor) as HttpDGStartController
  }

  override fun setThreadNum(threadNum: Int): HttpDGStartController {
    return super.setThreadNum(threadNum) as HttpDGStartController
  }

  override fun setHttpOption(httpOption: HttpOption): HttpDGStartController {
    return super.setHttpOption(httpOption) as HttpDGStartController
  }

  /**
   * Number of subtasks executed simultaneously
   * @param num max 16
   */
  fun setSubTaskNum(num: Int): HttpDGStartController {
    if (num < 1) {
      Timber.e("Quantity less than 1")
      return this
    }
    if (num > 16) {
      Timber.e("Quantity greater than 16")
      return this
    }
    optionAdapter.subTaskNum = num
    return this
  }

  /**
   * add sub task download uri
   */
  fun addSubUriResource(subUrlList: List<String>): HttpDGStartController {
    optionAdapter.subUrlList.addAll(subUrlList)
    return this
  }

  /**
   * map sub task name, [subTaskNameList].size must be consistent [addSubUriResource].size
   */
  fun addSubTaskName(subTaskNameList: List<String>): HttpDGStartController {
    optionAdapter.subNameList.addAll(subTaskNameList)
    return this
  }

  private fun getTask(createNewTask: Boolean = true): HttpDGroupTask {
    if (HttpUtil.checkHttpDParams(httpTaskOption)) {
      throw IllegalArgumentException("invalid params")
    }
    val temp = TaskCachePool.getTaskByKey(savePath.toString())
    if (temp != null) {
      return temp as HttpDGroupTask
    }
    val task = HttpDGroupTask(httpTaskOption)
    task.adapter = HttpDGroupAdapter()
    TaskCachePool.putTask(task)
    return task
  }

  /**
   * Start task
   * @return taskId
   */
  fun start(): Int {
    if (!FileUtils.uriEffective(savePath)) {
      Timber.e("invalid savePath: $savePath")
      return -1
    }

    val task = getTask()
    val resp = StartCmd(task).executeCmd()
    return if (resp.isInterrupt()) -1 else task.taskId
  }
}