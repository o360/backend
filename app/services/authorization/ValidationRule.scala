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

package services.authorization

/**
  * @param isSuccess true if validation successful
  * @param name      validation rule name. Used for error messages
  */
private[authorization] case class ValidationRule(isSuccess: Boolean, name: String)

private[authorization] object ValidationRule {

  /**
    * Validates rules and returns some error if validation is failed.
    *
    * @param rules validation rules
    * @return none in case of success, some error otherwise
    */
  def validate(rules: Seq[ValidationRule]): Option[String] = {
    val failed = rules.filter(!_.isSuccess)
    if (failed.isEmpty)
      None
    else
      Some(failed.map(_.name).mkString(", "))
  }

  /**
    * Creates new validation rule.
    *
    * @param name         validation rule name
    * @param precondition validation rule precondition
    * @param check        authorization result
    */
  def apply(name: String, precondition: Boolean)(check: => Boolean): ValidationRule =
    ValidationRule(
      isSuccess = !precondition || check,
      name = name
    )
}
