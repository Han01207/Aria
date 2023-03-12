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

import com.arialyy.aria.core.inf.IBlockManager

/**
 * @Author laoyuyu
 * @Description
 * @Date 11:35 2023/3/12
 **/
internal class SubTaskDelegate(val adapter: HttpDTaskAdapter) : ITaskAdapterDelegate {
  private lateinit var blockManager: IBlockManager

  override fun isRunning(): Boolean {
    TODO("Not yet implemented")
  }

  override fun cancel() {
    TODO("Not yet implemented")
  }

  override fun stop() {
    TODO("Not yet implemented")
  }

  override fun start() {
    TODO("Not yet implemented")
  }

  override fun setBlockManager(blockManager: IBlockManager) {
    this.blockManager = blockManager
  }

  override fun getBlockManager(): IBlockManager {
    return blockManager
  }
}