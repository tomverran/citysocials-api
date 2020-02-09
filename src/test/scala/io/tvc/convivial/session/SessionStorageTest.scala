package io.tvc.convivial.session

import cats.effect.IO
import io.tvc.convivial.session.IdCreator.SessionId
import io.tvc.convivial.storage.TestRedis
import io.tvc.convivial.users.User
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SessionStorageTest extends AnyWordSpec with Matchers {

  "Session storage" should {

    "Read back what it has written okay" in {
      (
        for {
          redis <- TestRedis[IO]
          user = User.Id(1234)
          storage = SessionStorage.redis(redis)
          _ <- storage.put(SessionId("foo"), user)
          got <- storage.get(SessionId("foo"))
        } yield got shouldBe Some(user)
      ).unsafeRunSync()
    }
  }
}
