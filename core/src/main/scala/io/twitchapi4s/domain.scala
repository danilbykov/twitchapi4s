package io.twitchapi4s

sealed trait TwitchEnv

case class ClientTwitchEnv(
  clientId: String
) extends TwitchEnv

case class ResponseHolder[D](
  data: D
)
case class Pagination(cursor: String)
case class ResponseHolderPage[D](
  data: D,
  total: Int,
  pagination: Pagination
)
case class FailureResponse(
  error: Option[String],
  status: Option[Int],
  message: Option[String]
)

sealed abstract class TwitchApiException extends Exception
case class TwitchResponseError(response: FailureResponse) extends TwitchApiException
case class TwitchParseException(message: String, rawJson: String) extends TwitchApiException
case class TwitchConnectionException(e: Throwable) extends TwitchApiException
