/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import java.io.{File, IOException}
import java.nio.file.{Files, Path, Paths}
import javax.inject.Inject

import org.apache.commons.io.FilenameUtils
import utils.errors.{ApplicationError, BadRequestError}
import utils.{Config, RandomGenerator}

import scalaz.Scalaz.ToEitherOps
import scalaz.\/

/**
  * File service.
  */
class FileService @Inject() (config: Config) {

  /**
    * Uploads file to local filesystem, validate extensions.
    */
  def upload(filename: String, from: Path, validExtensions: Set[String]): ApplicationError \/ String = {
    val ext = FilenameUtils.getExtension(filename)

    val isValidExtension =
      if (!validExtensions.contains(ext)) BadRequestError.File.Extension(ext, validExtensions).left
      else ().right

    for {
      _ <- isValidExtension

      newFilename = RandomGenerator.generateFilename + "." + ext
      _ <- try {
        val target = Paths.get(config.userFilesPath + newFilename)
        Files.createDirectories(target.getParent)
        Files.move(from, target)
        ().right
      } catch {
        case _: IOException => BadRequestError.File.Unexpected.left
      }
    } yield newFilename
  }

  /**
    * Returns file by name if exists.
    */
  def get(name: String): Option[File] = {
    val file = new File(config.userFilesPath + cleanupFilename(name))
    if (file.exists && !file.isDirectory) Some(file)
    else None
  }

  /**
    * Deletes file.
    */
  def delete(name: String): Unit = {
    val file = new File(config.userFilesPath + cleanupFilename(name))
    if (file.exists) file.delete()
  }

  private val cleanupFilename = new File(_: String).getName
}
