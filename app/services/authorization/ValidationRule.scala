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