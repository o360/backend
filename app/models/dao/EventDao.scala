package models.dao

import java.sql.Timestamp
import javax.inject.{Inject, Singleton}

import models.ListWithTotal
import models.event.{Event, EventJob}
import models.notification.Notification
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile
import utils.TimestampConverter
import utils.implicits.FutureLifting._
import utils.listmeta.ListMeta

import scala.concurrent.Future

/**
  * Component for notification datatypes.
  */
trait NotificationComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  implicit val eventKindMappedType = MappedColumnType.base[Notification.Kind, Byte](
    {
      case Notification.Kind.PreBegin => 0
      case Notification.Kind.Begin => 1
      case Notification.Kind.PreEnd => 2
      case Notification.Kind.End => 3
    }, {
      case 0 => Notification.Kind.PreBegin
      case 1 => Notification.Kind.Begin
      case 2 => Notification.Kind.PreEnd
      case 3 => Notification.Kind.End
    }
  )

  implicit val eventRecipient = MappedColumnType.base[Notification.Recipient, Byte](
    {
      case Notification.Recipient.Respondent => 0
      case Notification.Recipient.Auditor => 1
    }, {
      case 0 => Notification.Recipient.Respondent
      case 1 => Notification.Recipient.Auditor
    }
  )
}

/**
  * Component for event and event_notification tables.
  */
trait EventComponent extends NotificationComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import driver.api._

  /**
    * Event DB model.
    */
  case class DbEvent(
    id: Long,
    description: Option[String],
    start: Timestamp,
    end: Timestamp,
    canRevote: Boolean
  )

  object DbEvent {
    def fromModel(event: Event) = DbEvent(
      event.id,
      event.description,
      event.start,
      event.end,
      event.canRevote
    )
  }

  class EventTable(tag: Tag) extends Table[DbEvent](tag, "event") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def description = column[Option[String]]("description")
    def start = column[Timestamp]("start_time")
    def end = column[Timestamp]("end_time")
    def canRevote = column[Boolean]("can_revote")

    def * = (id, description, start, end, canRevote) <> ((DbEvent.apply _).tupled, DbEvent.unapply)
  }

  val Events = TableQuery[EventTable]

  /**
    * Event notification DB model.
    */
  case class DbEventNotification(
    id: Long,
    eventId: Long,
    time: Timestamp,
    kind: Notification.Kind,
    recipient: Notification.Recipient
  )

  object DbEventNotification {
    def fromModel(eventId: Long, notification: Event.NotificationTime) = DbEventNotification(
      0,
      eventId,
      notification.time,
      notification.kind,
      notification.recipient
    )
  }

  class EventNotificationTable(tag: Tag) extends Table[DbEventNotification](tag, "event_notification") {

    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def eventId = column[Long]("event_id")
    def time = column[Timestamp]("time")
    def kind = column[Notification.Kind]("kind")
    def recipient = column[Notification.Recipient]("recipient_kind")

    def * = (id, eventId, time, kind, recipient) <> ((DbEventNotification.apply _).tupled, DbEventNotification.unapply)
  }

  val EventNotifications = TableQuery[EventNotificationTable]
}


/**
  * Event DAO.
  */
@Singleton
class EventDao @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider
) extends HasDatabaseConfigProvider[JdbcProfile]
  with EventComponent
  with EventProjectComponent
  with ProjectRelationComponent
  with DaoHelper {

  import driver.api._

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
    }.map(id => event.copy(id = id))
  }

  /**
    * Returns event list filtered by given criteria.
    */
  def getList(
    optId: Option[Long] = None,
    optStatus: Option[Event.Status] = None,
    optProjectId: Option[Long] = None,
    optFormId: Option[Long] = None,
    optGroupFromIds: Option[Seq[Long]] = None
  )(implicit meta: ListMeta = ListMeta.default): Future[ListWithTotal[Event]] = {

    def statusFilter(event: EventTable) = optStatus.map { status =>
      val currentTime = TimestampConverter.now
      status match {
        case Event.Status.NotStarted => event.start > currentTime
        case Event.Status.InProgress => event.start <= currentTime && event.end > currentTime
        case Event.Status.Completed => event.end <= currentTime
      }
    }

    def projectFilter(event: EventTable) = optProjectId.map { projectId =>
      EventProjects
        .filter(x => x.eventId === event.id && x.projectId === projectId)
        .exists
    }

    def formFilter(event: EventTable) = optFormId.map { formId =>
      EventProjects.join(Relations).on(_.projectId === _.projectId)
        .filter { case (eventProject, relation) =>
          eventProject.eventId === event.id && relation.formId === formId
        }
        .exists
    }

    def groupFromFilter(event: EventTable) = optGroupFromIds.map { groupFromIds =>
      EventProjects.join(Relations).on(_.projectId === _.projectId)
        .filter { case (eventProject, relation) =>
          eventProject.eventId === event.id && relation.groupFromId.inSet(groupFromIds)
      }
      .exists
    }

    val baseQuery = Events
      .applyFilter { event =>
        Seq(
          optId.map(event.id === _),
          statusFilter(event),
          projectFilter(event),
          formFilter(event),
          groupFromFilter(event)
        )
      }

    val countQuery = baseQuery.length

    def sortMapping(event: EventTable): PartialFunction[Symbol, Rep[_]] = {
      case 'id => event.id
      case 'start => event.start
      case 'end => event.end
      case 'description => event.description
      case 'canRevote => event.canRevote
    }

    val resultQuery = baseQuery
      .applySorting(meta.sorting)(sortMapping)
      .applyPagination(meta.pagination)
      .joinLeft(EventNotifications).on(_.id === _.eventId)
      .applySorting(meta.sorting) { case (event, _) => sortMapping(event) } // sort one page (order not preserved after join)

    for {
      count <- db.run(countQuery.result)
      flatResult <- if (count > 0) db.run(resultQuery.result) else Nil.toFuture
    } yield {
      val data = flatResult
        .groupByWithOrder { case (event, _) => event }
        .map { case (event, notificationsWithEvent) =>
          val notifications = notificationsWithEvent
            .collect { case (_, Some(n)) => Event.NotificationTime(n.time, n.kind, n.recipient) }
          Event(event.id, event.description, event.start, event.end, event.canRevote, notifications)
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
    }.map(_ => event)
  }

  /**
    * Deletes event from DB.
    *
    * @param eventId ID of event.
    */
  def delete(eventId: Long): Future[Int] = db.run {
    Events.filter(_.id === eventId).delete
  }
}
