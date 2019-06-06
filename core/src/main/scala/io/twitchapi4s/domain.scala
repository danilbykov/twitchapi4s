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

sealed abstract class TwitchApiException extends Exception
case class TwitchResponseError(response: FailureResponse) extends TwitchApiException
case class UnauthorizedException(message: Option[String]) extends TwitchApiException
case class TwitchParseException(message: String, rawJson: String) extends TwitchApiException
case class RateLimitExceeded(reset: Long) extends TwitchApiException
case class TwitchConnectionException(e: Throwable) extends TwitchApiException
