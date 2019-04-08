package models.dao

import java.time.LocalDateTime

import io.scalaland.chimney.dsl._
import javax.inject.{Inject, Singleton}
import models.ListWithTotal
import models.event.Event
import models.notification._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import scalaz.std.option._
import slick.jdbc.JdbcProfile
import utils.TimestampConverter
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.{ExecutionContext, Future}

/**
  * Component for notification datatypes.
  */
trait NotificationComponent extends EnumColumnMapper { self: HasDatabaseConfigProvider[JdbcProfile] =>

  implicit lazy val eventKindMappedType = mappedEnumSeq[NotificationKind](PreBegin, Begin, PreEnd, End)

  implicit lazy val eventRecipient = mappedEnumSeq[NotificationRecipient](Respondent, Auditor)
}

/**
  * Component for event and event_notification tables.
  */
trait EventComponent extends NotificationComponent with DateColumnMapper {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  /**
    * Event DB model.
    */
  case class DbEvent(
    id: Long,
    description: Option[String],
    start: LocalDateTime,
    end: LocalDateTime,
    isPreparing: Boolean
  ) {
    def toModel(notifications: Seq[Event.NotificationTime]) =
      this
        .into[Event]
        .withFieldConst(_.notifications, notifications)
        .withFieldConst(_.userInfo, none[Event.UserInfo])
        .transform
  }

  object DbEvent {
    def fromModel(event: Event) = event.transformInto[DbEvent]
  }

  class EventTable(tag: Tag) extends Table[DbEvent](tag, "event") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def description = column[Option[String]]("description")
    def start = column[LocalDateTime]("start_time")
    def end = column[LocalDateTime]("end_time")
    def isPreparing = column[Boolean]("is_preparing")

    def * = (id, description, start, end, isPreparing) <> ((DbEvent.apply _).tupled, DbEvent.unapply)
  }

  val Events = TableQuery[EventTable]

  /**
    * Event notification DB model.
    */
  case class DbEventNotification(
    id: Long,
    eventId: Long,
    time: LocalDateTime,
    kind: NotificationKind,
    recipient: NotificationRecipient
  )

  object DbEventNotification {
    def fromModel(eventId: Long, notification: Event.NotificationTime) =
      notification
        .into[DbEventNotification]
        .withFieldConst(_.id, 0L)
        .withFieldConst(_.eventId, eventId)
        .transform
  }

  class EventNotificationTable(tag: Tag) extends Table[DbEventNotification](tag, "event_notification") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def eventId = column[Long]("event_id")
    def time = column[LocalDateTime]("time")
    def kind = column[NotificationKind]("kind")
    def recipient = column[NotificationRecipient]("recipient_kind")

    def * = (id, eventId, time, kind, recipient) <> ((DbEventNotification.apply _).tupled, DbEventNotification.unapply)
  }

  val EventNotifications = TableQuery[EventNotificationTable]

  def statusFilter(event: EventTable, status: Event.Status) = {
    val currentTime = TimestampConverter.now
    status match {
      case Event.Status.NotStarted => event.start > currentTime || event.isPreparing
      case Event.Status.InProgress => event.start <= currentTime && event.end > currentTime && !event.isPreparing
      case Event.Status.Completed => event.end <= currentTime
    }
  }
}

/**
  * Event DAO.
  */
@Singleton
class EventDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  implicit val ec: ExecutionContext
) extends HasDatabaseConfigProvider[JdbcProfile]
  with EventComponent
  with EventProjectComponent
  with ProjectRelationComponent
  with ActiveProjectComponent
  with AnswerComponent
  with DaoHelper {

  import profile.api._

  /**
    * Creates event with notifications.
    *
    * @param event event model
    * @return created event with ID
    */
  def create(event: Event): Future[Event] = {
    db.run {
        (for {
          eventId <- Events.returning(Events.map(_.id)) += DbEvent.fromModel(event)
          _ <- DBIO.seq(EventNotifications ++= event.notifications.map(x => DbEventNotification.fromModel(eventId, x)))
        } yield eventId).transactionally
      }
      .map(id => event.copy(id = id))
  }

  /**
    * Returns event list filtered by given criteria.
    */
  def getList(
    optId: Option[Long] = None,
    optStatus: Option[Event.Status] = None,
    optProjectId: Option[Long] = None,
    optFormId: Option[Long] = None,
    optGroupFromIds: Option[Seq[Long]] = None,
    optUserId: Option[Long] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Event]] = {

    def projectFilter(event: EventTable) = optProjectId.map { projectId =>
      EventProjects
        .filter(x => x.eventId === event.id && x.projectId === projectId)
        .exists
    }

    def formFilter(event: EventTable) = optFormId.map { formId =>
      EventProjects
        .join(Relations)
        .on(_.projectId === _.projectId)
        .filter {
          case (eventProject, relation) =>
            eventProject.eventId === event.id && relation.formId === formId
        }
        .exists
    }

    def groupFromFilter(event: EventTable) = optGroupFromIds.map { groupFromIds =>
      EventProjects
        .join(Relations)
        .on(_.projectId === _.projectId)
        .filter {
          case (eventProject, relation) =>
            eventProject.eventId === event.id && relation.groupFromId.inSet(groupFromIds)
        }
        .exists
    }

    def userFilter(event: EventTable) = optUserId.map { userId =>
      Answers
        .join(ActiveProjects)
        .on(_.activeProjectId === _.id)
        .filter {
          case (answer, activeProject) =>
            activeProject.eventId === event.id && answer.userFromId === userId
        }
        .exists
    }

    val baseQuery = Events
      .applyFilter { event =>
        Seq(
          optId.map(event.id === _),
          optStatus.map(statusFilter(event, _)),
          projectFilter(event),
          formFilter(event),
          groupFromFilter(event),
          userFilter(event)
        )
      }

    val countQuery = baseQuery.length

    def sortMapping(event: EventTable): PartialFunction[Symbol, Rep[_]] = {
      case 'id => event.id
      case 'start => event.start
      case 'end => event.end
      case 'description => event.description
    }

    val resultQuery = baseQuery
      .applySorting(meta.sorting)(sortMapping)
      .applyPagination(meta.pagination)
      .joinLeft(EventNotifications)
      .on(_.id === _.eventId)
      .applySorting(meta.sorting) { case (event, _) => sortMapping(event) } // sort one page (order not preserved after join)

    for {
      count <- db.run(countQuery.result)
      flatResult <- if (count > 0) db.run(resultQuery.result) else Nil.toFuture
    } yield {
      val data = flatResult
        .groupByWithOrder { case (event, _) => event }
        .map {
          case (event, notificationsWithEvent) =>
            val notifications = notificationsWithEvent
              .collect { case (_, Some(n)) => Event.NotificationTime(n.time, n.kind, n.recipient) }
            event.toModel(notifications)
        }

      ListWithTotal(count, data)
    }
  }

  /**
    * Returns event by ID.
    */
  def findById(id: Long): Future[Option[Event]] = {
    getList(optId = Some(id)).map(_.data.headOption)
  }

  /**
    * Updates event with notifications.
    *
    * @param event event model
    * @return updated event
    */
  def update(event: Event): Future[Event] = {
    db.run {
        (for {
          _ <- Events.filter(_.id === event.id).update(DbEvent.fromModel(event))
          _ <- EventNotifications.filter(_.eventId === event.id).delete
          _ <- DBIO.seq(EventNotifications ++= event.notifications.map(DbEventNotification.fromModel(event.id, _)))
        } yield ()).transactionally
      }
      .map(_ => event)
  }

  /**
    * Deletes event from DB.
    *
    * @param eventId ID of event.
    */
  def delete(eventId: Long): Future[Int] = db.run {
    Events.filter(_.id === eventId).delete
  }

  /**
    * Sets isPreparing flag.
    */
  def setIsPreparing(eventId: Long, isPreparing: Boolean): Future[Unit] = {
    db.run(Events.filter(_.id === eventId).map(_.isPreparing).update(isPreparing)).map(_ => ())
  }
}
