package io.twitchapi4s.zio.client

import io.twitchapi4s.{client => baseclient}

import Endpoint._
trait UsersEndpoint extends baseclient.UsersEndpoint[Effect]
