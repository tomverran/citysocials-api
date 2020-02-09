package io.tvc.convivial.profile

import cats.syntax.flatMap._
import io.tvc.convivial.storage.TestPostgres
import io.tvc.convivial.users.{UserStorage, user, userId}
import org.scalacheck.Prop
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.claimant.Claim

class ProfileStorageTest extends AnyWordSpec with Matchers with Checkers with TestPostgres {

  "Profile Storage" should {

    "Fail if the user doesn't exist" inDb { db =>
      val profiles = ProfileStorage.postgres(db)
      Prop.forAll(userId, complete) { case (id, profile) =>
        profiles.put(id, profile).attempt.map(r => Claim(r.isLeft)).unsafeRunSync()
      }
    }

    "Read back what is written" inDb { db =>
      val users = UserStorage.postgres(db)
      val profiles = ProfileStorage.postgres(db)
      Prop.forAll(user, complete) { case (user, profile) =>
        (
          for {
            id <- users.upsert(user)
            stored <- profiles.put(id, profile) >> profiles.get(id)
          } yield Claim(stored == profile)
        ).unsafeRunSync()
      }
    }
  }
}
