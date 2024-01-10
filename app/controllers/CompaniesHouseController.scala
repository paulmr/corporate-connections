package controllers

import play.api.mvc._
import services.CompaniesHouseService

import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.circe.Circe

import io.circe.syntax._
import cats.data.EitherT

class CompaniesHouseController(
  components: ControllerComponents,
  service: CompaniesHouseService
)(implicit ec: ExecutionContext) extends
    AbstractController(components)
    with Circe {

  def maybeResult[T](e: EitherT[Future, String, T])(f: T => Result): Future[Result] = {
    // here we are converting each side of the Either into a "Result"
    // which can be returned from the HTTP Request. The error we
    // convert into a result using InternalServerError() and the non
    // error side, type T, we convert with the provided callback `f`.
    e.bimap(err => InternalServerError(err), f).merge
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
