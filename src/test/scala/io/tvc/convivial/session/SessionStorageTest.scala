package io.tvc.convivial.session

import cats.effect.IO
import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.storage.TestRedis
import io.tvc.convivial.twitter.TwitterId
import io.tvc.convivial.users.User
import org.scalatest.{Matchers, WordSpec}

class SessionStorageTest extends WordSpec with Matchers {

  "Session storage" should {

    "Read back what it has written okay" in {
      (
        for {
          redis <- TestRedis[IO]
          user = User("a", TwitterId("b"))
          storage = SessionStorage.redis(redis)
          _ <- storage.put(SessionId("foo"), user)
          got <- storage.get(SessionId("foo"))
        } yield got shouldBe Some(user)
      ).unsafeRunSync()
    }
  }
}
