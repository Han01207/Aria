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
package com.arialyy.aria.core.task

/**
 * @Author laoyuyu
 * @Description
 * @Date 1:19 PM 2023/1/28
 **/
interface ITaskInterceptor {

  suspend fun interceptor(chain: TaskChain): TaskResp

  interface IChain {
    fun getTask(): ITask
    suspend fun proceed(task: ITask): TaskResp
  }
}