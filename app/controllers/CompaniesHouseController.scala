package controllers

import play.api.mvc._
import services.CompaniesHouseService

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.circe.Circe

import io.circe.syntax._

class CompaniesHouseController(
  components: ControllerComponents,
  service: CompaniesHouseService
)(implicit ec: ExecutionContext) extends
    AbstractController(components)
    with Circe {

  def maybeResult[T](e: Future[Either[String, T]])(f: T => Result): Future[Result] = {
    e.map {
      case Right(res) => f(res)
      case Left(err) => InternalServerError(err)
    }
  }

  def getAppointmentsProxy(officerId: String) = Action.async {
    maybeResult(service.fetchAppointmentsProxy(officerId))(Ok(_))
  }

  def getAppointments(officerId: String) = Action.async {
    maybeResult(service.fetchAppointments(officerId)) { res =>
      Ok(res.items.map(_.appointed_to.company_number).asJson)
    }
  }

  def getOfficers(companyNumber: String) = Action.async {
    maybeResult(service.fetchOfficers(companyNumber))(r => Ok(r.asJson))
  }

  def getConnections(officerId: String) = Action.async {
    maybeResult(service.fetchConnections(officerId)) { r =>
      Ok(r.items.flatMap(_._2).map(_.name).toSet.asJson)
    }
  }

}
