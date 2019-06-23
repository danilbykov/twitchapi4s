package io.twitchapi4s

sealed trait TwitchEnv
case class ClientIdTwitchEnv(clientId: String) extends TwitchEnv
case class TokenTwitchEnv(token: String) extends TwitchEnv

sealed trait RecoverableTwitchEnv {
  def clientId: String
  def clientSecret: String
}
case class ClientIdAndSecretTwitchEnv(
  clientId: String,
  clientSecret: String,
  token: String
) extends RecoverableTwitchEnv
case class WithoutTokenEnv(
  clientId: String,
  clientSecret: String
) extends RecoverableTwitchEnv

case class ResponseHolder[D](
  data: D
)
case class Pagination(cursor: Option[String])
case class ResponseHolderPage[D](
  data: D,
  //total: Int,
  pagination: Pagination
)
case class FailureResponse(
  error: Option[String],
  status: Option[Int],
  message: Option[String]
)

case class Token(
  accessToken: String,
  expiresIn: Int,
  tokenType: String
)

sealed abstract class TwitchApiException(message: String, throwable: Throwable)
  extends Exception(message, throwable) {
  def this(message: String) = this(message, null)
}
case class TwitchResponseError(response: FailureResponse)
  extends TwitchApiException(response.toString)
case class UnauthorizedException(message: Option[String])
  extends TwitchApiException(message.getOrElse(""))
case class TwitchParseException(message: String, rawJson: String)
  extends TwitchApiException(s"$message while parsing $rawJson")
case class RateLimitExceeded(reset: Long)
  extends TwitchApiException(s"Rate limit exceeded. Reset is at $reset")
case class TwitchConnectionException(e: Throwable)
  extends TwitchApiException("Connection problem", e)
