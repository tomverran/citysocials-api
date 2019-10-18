package io.tvc.convivial.session

import cats.effect.IO
import io.tvc.convivial.session.IdCreator.Config
import org.scalatest.{Matchers, WordSpec}

class IdCreatorTest extends WordSpec with Matchers {

  "IdCreator" should {

    "Be able to verify ids it has produced" in {
      IdCreator[IO](Config("test")).use { c =>
        for {
          first <- c.create
          verified <- c.verify(first.value)
        } yield first shouldBe verified
      }.unsafeRunSync()
    }

    "Not verify mismatched ids & macs" in {
      IdCreator[IO](Config("test")).use { c =>
        for {
          first <- c.create
          second <- c.create
          firstId = first.value.split('.').head
          secondMac = second.value.split('.').last
          verified <- c.verify(s"$firstId.$secondMac").attempt
        } yield verified should matchPattern { case Left(_) => }
      }.unsafeRunSync()
    }
  }
}
