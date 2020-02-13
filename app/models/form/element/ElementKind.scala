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

package models.form.element

/**
  * Kind of form element.
  *
  * @param needValues is list of values needed
  */
sealed abstract class ElementKind(val needValues: Boolean) {
  val isValuesNeeded = needValues
}

case object TextField extends ElementKind(false)
case object TextArea extends ElementKind(false)
case object Checkbox extends ElementKind(false)
case object CheckboxGroup extends ElementKind(true)
case object Radio extends ElementKind(true)
case object Select extends ElementKind(true)
case object LikeDislike extends ElementKind(true)
