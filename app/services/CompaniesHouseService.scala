package services

import play.api.libs.ws.{WSAuthScheme, WSClient}
import scala.concurrent.{ExecutionContext, Future}
import io.circe.Json
import io.circe.parser.decode
import scala.util.{Success, Failure}
import io.circe.Decoder

class CompaniesHouseService(wsClient: WSClient)(implicit ec: ExecutionContext) {

  lazy val apiKey = {
    val res = System.getenv("API_KEY")
    assert(res != null)
    res
  }

  private def fetch(url: String): Future[String] = {
    wsClient
      .url(url)
      .withAuth(apiKey, "", WSAuthScheme.BASIC)
      .get()
      .map(response => response.body)
  }

  def fetchJson(url: String): Future[Json] = fetch(url).map { result =>
    decode[Json](result).toOption.get
  }

  def decodeJson[T : Decoder](url: String): Future[T] = fetchJson(url).map(_.as[T].toOption.get)

  def fetchAppointmentsProxy(id: String): Future[Json] =
    fetchJson(s"https://api.company-information.service.gov.uk/officers/$id/appointments")

  def fetchAppointments(id: String): Future[models.OfficerAppointmentResponse] =
    decodeJson[models.OfficerAppointmentResponse](s"https://api.company-information.service.gov.uk/officers/$id/appointments")

  def fetchOfficers(companyId: String): Future[models.CompanyOfficersResponse] =
    decodeJson[models.CompanyOfficersResponse](s"https://api.company-information.service.gov.uk/company/$companyId/officers")

  def fetchConnections(id: String): Future[models.ConnectionsResponse] =
    for {
      appts <- fetchAppointments(id)
      companies = appts.items.map(_.appointed_to.company_number)
      connections <- Future.sequence(companies.map(c => fetchOfficers(c).map(c -> _.items)))
    } yield models.ConnectionsResponse(connections.toMap)
}
