package services

import play.api.libs.ws.{WSAuthScheme, WSClient}
import scala.concurrent.{ExecutionContext, Future}
import io.circe.Json
import io.circe.parser.decode
import scala.util.{Success, Failure}
import io.circe.Decoder

object CompaniesHouseService {
  type CompaniesHouseResponse[T] = Future[Either[String, T]]
}

import CompaniesHouseService._

class CompaniesHouseService(wsClient: WSClient)(implicit ec: ExecutionContext) {

  lazy val apiKey = {
    val res = System.getenv("API_KEY")
    assert(res != null)
    res
  }

  private def fetch(url: String): CompaniesHouseResponse[String] = {
    wsClient
      .url(url)
      .withAuth(apiKey, "", WSAuthScheme.BASIC)
      .get()
      .map { response =>
        if(response.status == 200) Right(response.body)
        else Left(response.statusText)
      }
  }

  def fetchJson(url: String): CompaniesHouseResponse[Json] = fetch(url).map { resultE =>
    resultE.flatMap(result => decode[Json](result).left.map(_.getMessage()))
  }

  def decodeJson[T : Decoder](url: String): CompaniesHouseResponse[T] = fetchJson(url).map(_.flatMap(_.as[T].left.map(_.message)))

  def fetchAppointmentsProxy(id: String): CompaniesHouseResponse[Json] =
    fetchJson(s"https://api.company-information.service.gov.uk/officers/$id/appointments")

  def fetchAppointments(id: String): CompaniesHouseResponse[models.OfficerAppointmentResponse] =
    decodeJson[models.OfficerAppointmentResponse](s"https://api.company-information.service.gov.uk/officers/$id/appointments")

  def fetchOfficers(companyId: String): CompaniesHouseResponse[models.CompanyOfficersResponse] =
    decodeJson[models.CompanyOfficersResponse](s"https://api.company-information.service.gov.uk/company/$companyId/officers")

  def fetchConnections(id: String): CompaniesHouseResponse[models.ConnectionsResponse] = {
    fetchAppointments(id).flatMap {
      case Right(appts) =>
        val companies = appts.items.map(_.appointed_to.company_number)
        val officersF = Future.sequence(companies.map(c => fetchOfficers(c).map(c -> _)))
        officersF.map { officers =>
          // was there an error?
          officers.collectFirst { case (_, Left(err)) => err }.toLeft {
            models.ConnectionsResponse(items = officers.collect { case (c, Right(s)) => c -> s.items }.toMap)
          }
        }
        //   .map { officers =>
        //   officers.collectFirst { case Left(err) => err }.toLeft {
        //     officers.collect { case Right(officers) => officers }
        //   }
        // }
      case Left(err) => Future.successful(Left[String, models.ConnectionsResponse](err))
    }
  }

    // for {
    //   appts <- fetchAppointments(id)
    //   companies =
    //   connections <- Future.sequence(companies.map(c => fetchOfficers(c).map(c -> _.items)))
    // } yield models.ConnectionsResponse(connections.toMap)
}
