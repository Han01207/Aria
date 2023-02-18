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

import com.arialyy.aria.core.task.AbsThreadTaskAdapter
import com.arialyy.aria.core.task.ThreadConfig
import com.arialyy.aria.http.ConnectionHelp
import com.arialyy.aria.http.request.IRequest.Companion.getRequest
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels

/**
 * http download thread task, not support chunk
 * @Author laoyuyu
 * @Description
 * @Date 23:07 PM 2023/2/8
 **/
class HttpDTTaskAdapter(threadConfig: ThreadConfig) : AbsThreadTaskAdapter(threadConfig) {

  private fun getTaskOption(): HttpDTaskOption {
    return threadConfig.option as HttpDTaskOption
  }

  override fun handlerThreadTask() {
    val taskOption = getTaskOption()
    val option = taskOption.httpOption!!
    val conn = getRequest(option).getDConnection(taskOption.sourUrl!!, option)
    if (!taskOption.isSupportResume) {
      Timber.w("this task not support resume, url: %s", taskOption.sourUrl)
    } else {
      conn.setRequestProperty(
        "Range", String.format(
          "bytes=%s-%s", threadConfig.blockRecord.startLocation,
          threadConfig.blockRecord.endLocation - 1
        )
      )
    }

    conn.connect()
    BufferedInputStream(ConnectionHelp.convertInputStream(conn)).use {
      readBytes(it)
    }
  }

  private fun readBytes(ips: InputStream) {
    val fos = FileOutputStream(blockRecord.blockPath, true)
    val foc = fos.channel
    val fic = Channels.newChannel(ips)
    val bf = ByteBuffer.allocate(BUF_SIZE)
    fic.use {
      var len: Long
      while (fic.read(bf).also { len = it.toLong() } != -1) {
        if (isBreakTask) {
          break
        }
        if (mSpeedBandUtil != null) {
          mSpeedBandUtil.limitNextBytes(len.toInt())
        }
        if (rangeProgress + len >= blockRecord.endLocation) {
          len = (blockRecord.endLocation - rangeProgress)
          bf.flip()
          fos.write(bf.array(), 0, len.toInt())
          bf.compact()
          progress(len)
          break
        } else {
          bf.flip()
          foc.write(bf)
          bf.compact()
          progress(len)
        }
      }
      // 将数据刷出到磁盘
      foc.force(true)
      foc.close()
      fos.close()
    }
  }
}