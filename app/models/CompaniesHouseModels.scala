package models

import io.circe._, io.circe.generic.semiauto._

case class AppointedTo(company_name: String, company_number: String, company_status: String)
case class Appointment(officer_role: String, appointed_to: AppointedTo)
case class OfficerAppointmentResponse(items: List[Appointment])

object AppointedTo {
  implicit val appointedToDecoder = deriveDecoder[AppointedTo]
}

object Appointment {
  implicit val appointmentDecoder = deriveDecoder[Appointment]
}

object OfficerAppointmentResponse {
  implicit val officerAppointmentResponseDecoder: Decoder[OfficerAppointmentResponse] =
    deriveDecoder[OfficerAppointmentResponse]
}

case class Officer(name: String, officer_role: String)
case class CompanyOfficersResponse(items: List[Officer])

object Officer {
  implicit val officerDecoder = deriveDecoder[Officer]
  implicit val officerEncoder = deriveEncoder[Officer]
}

object CompanyOfficersResponse {
  implicit val companyOfficersResponseDecoder = deriveDecoder[CompanyOfficersResponse]
  implicit val companyOfficersResponseEncoder = deriveEncoder[CompanyOfficersResponse]
}

case class ConnectionsResponse(items: Map[String, List[Officer]])

object ConnectionsResponse {
  implicit val connectionsResponseEncoder = deriveEncoder[ConnectionsResponse]
}
