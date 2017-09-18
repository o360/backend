package utils.errors

import models.NamedEntity

/**
  * Conflict error.
  */
abstract class ConflictError(
  code: String,
  message: String,
  logMessage: Option[String] = None,
  related: Option[Map[String, Seq[NamedEntity]]] = None
) extends ApplicationError(code, message, logMessage) {
  def getRelated = related
}

object ConflictError {

  /**
    * Returns map with conflicts. Map element is entity name -> pairs of id-name.
    */
  def getConflictedEntitiesMap(pairs: (String, Seq[NamedEntity])*): Option[Map[String, Seq[NamedEntity]]] = {
    val nonEmptyPairs = pairs.filter(_._2.nonEmpty)

    if (nonEmptyPairs.isEmpty) None
    else Some(nonEmptyPairs.toMap)
  }

  case class General(
    entityName: Option[String] = None,
    conflicted: Option[Map[String, Seq[NamedEntity]]] = None,
    logMessage: Option[String] = None,
    errorText: Option[String] = None
  ) extends ConflictError(
      s"CONFLICT-${entityName.map(_.toUpperCase + "-").getOrElse("")}GENERAL",
      errorText.getOrElse("Integrity violation"),
      logMessage,
      conflicted
    )

  object Group {
    case class ParentId(id: Long) extends ConflictError("CONFLICT-GROUP-1", s"Group id:$id can't be parent to itself")

    case class CircularReference(id: Long, parentId: Long)
      extends ConflictError("CONFLICT-GROUP-2", s"Can't set parentId:$parentId in group id:$id. Circular reference")

    case object RelationExists extends ConflictError("CONFLICT-GROUP-5", "Can't delete, relation exists")

    case object DuplicateName extends ConflictError("CONFLICT-GROUP-6", "Duplicate name")
  }
  object User {
    case object Unapproved extends ConflictError("CONFLICT-USER-2", "Can't add unapproved user to group")
  }

  object Form {
    case class ElementValuesMissed(element: String)
      extends ConflictError("CONFLICT-FORM-1", s"Missed values list in [$element]")

    case object ActiveEventExists extends ConflictError("CONFLICT-FORM-3", "Can't update, active events exists")

    case class FormKind(action: String) extends ConflictError("CONFLICT-FORM-5", s"Can't $action.")

    case object MissedValuesInLikeDislike
      extends ConflictError("CONFLICT-FORM-6", "Wrong values for likedislike element")
  }

  object Project {
    case object ActiveEventExists extends ConflictError("CONFLICT-PROJECT-2", "Can't update, active events exists")

    case object EventExists extends ConflictError("CONFLICT-PROJECT-3", "Can't delete, events exists")

    case object DuplicateName extends ConflictError("CONFLICT-PROJECT-4", "Duplicate name")
  }

  object Template {
    case object ProjectExists extends ConflictError("CONFLICT-TEMPLATE-1", "Can't delete, project exists")

    case object RelationExists extends ConflictError("CONFLICT-RELATION-2", "Can't delete, relation exists")
  }

  object Assessment {
    case object WrongParameters
      extends ConflictError("CONFLICT-ASSESSMENT-1", "Can't find relation matched user and form ids")

    case object CantRevote extends ConflictError("CONFLICT-ASSESSMENT-2", "Revoting is forbidden")

    case object CantSkip extends ConflictError("CONFLICT-ASSESSMENT-3", "Skipping is forbidden")

    case object InactiveEvent extends ConflictError("CONFLICT-ASSESSMENT-4", "Event is not in progress")
  }

  object Invite {
    case class UserAlreadyRegistered(email: String)
      extends ConflictError("CONFLICT-INVITE-1", s"User $email already registered")

    case object UserAlreadyApproved extends ConflictError("CONFLICT-INVITE-2", s"User already approved")

    case object CodeAlreadyUsed extends ConflictError("CONFLICT-INVITE-3", s"Code already used")
  }

  object Competence {
    case object CompetenceIdNotExists extends ConflictError("CONFLICT-COMPETENCE-1", "Competence not exists")

    case object DuplicateElementCompetence
      extends ConflictError("CONFLICT-COMPETENCE-2", "Duplicate element competence")
  }
}
