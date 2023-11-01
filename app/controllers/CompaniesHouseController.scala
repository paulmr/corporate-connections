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
  def getAppointmentsProxy(officerId: String) = Action.async {
    service.fetchAppointmentsProxy(officerId).map { res => Ok(res) }
  }

  def getAppointments(officerId: String) = Action.async {
    service.fetchAppointments(officerId).map { res =>
      Ok(res.items.map(_.appointed_to.company_number).asJson)
    }
  }

  def getOfficers(companyNumber: String) = Action.async {
    service.fetchOfficers(companyNumber).map(r => Ok(r.asJson))
  }

  def getConnections(officerId: String) = Action.async {
    service.fetchConnections(officerId).map { r =>
      Ok(r.items.flatMap(_._2).map(_.name).toSet.asJson)
    }
  }

}
