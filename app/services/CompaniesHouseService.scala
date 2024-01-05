package services

import play.api.libs.ws.{WSAuthScheme, WSClient}
import scala.concurrent.{ExecutionContext, Future}
import io.circe.Json
import io.circe.parser.decode
import scala.util.{Success, Failure}
import io.circe.Decoder
import cats.data.EitherT
import cats.Traverse

object CompaniesHouseService {
  type CompaniesHouseResponse[T] = EitherT[Future, String, T]
}

import CompaniesHouseService._

class CompaniesHouseService(wsClient: WSClient)(implicit ec: ExecutionContext) {

  lazy val apiKey = {
    val res = System.getenv("API_KEY")
    assert(res != null)
    res
  }

  private def fetch(url: String): CompaniesHouseResponse[String] = {
    EitherT(
      wsClient
        .url(url)
        .withAuth(apiKey, "", WSAuthScheme.BASIC)
        .get()
        .map { response =>
          if(response.status == 200) Right(response.body)
          else Left(response.statusText)
        }
    )
  }

  def fetchJson(url: String): CompaniesHouseResponse[Json] =
    for {
      result <- fetch(url)
      js <- EitherT.fromEither[Future](decode[Json](result).left.map(_.getMessage()))
    } yield js

  def decodeJson[T : Decoder](url: String): CompaniesHouseResponse[T] =
    for {
      js <- fetchJson(url)
      res <- EitherT.fromEither[Future](js.as[T].left.map(_.message))
    } yield res

  def fetchAppointmentsProxy(id: String): CompaniesHouseResponse[Json] =
    fetchJson(s"https://api.company-information.service.gov.uk/officers/$id/appointments")

  def fetchAppointments(id: String): CompaniesHouseResponse[models.OfficerAppointmentResponse] =
    decodeJson[models.OfficerAppointmentResponse](s"https://api.company-information.service.gov.uk/officers/$id/appointments")

  def fetchOfficers(companyId: String): CompaniesHouseResponse[models.CompanyOfficersResponse] =
    decodeJson[models.CompanyOfficersResponse](s"https://api.company-information.service.gov.uk/company/$companyId/officers")

  def fetchConnections(id: String): CompaniesHouseResponse[models.ConnectionsResponse] =
    for {
      appts <- fetchAppointments(id)
      companies = appts.items.map(_.appointed_to.company_number)
      officers <- Traverse[List].sequence(companies.map(c => fetchOfficers(c).map(c -> _)))
    } yield models.ConnectionsResponse(items = officers.map { case (k, v) => k -> v.items }.toMap)

}
