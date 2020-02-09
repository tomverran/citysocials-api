package io.tvc.convivial.users

import io.tvc.convivial.storage.TestPostgres
import io.tvc.convivial.twitter.TwitterId
import org.scalacheck.Prop
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.Checkers
import org.typelevel.claimant.Claim

class UserStorageTest extends AnyWordSpec with Matchers with Checkers with TestPostgres {

  "User Storage" should {
    "Idempotently upsert users by twitter ID" inDb { db =>
      val store = UserStorage.postgres(db)
      Prop.forAll(user) { user =>
        (
          for {
            idempotent1 <- store.upsert(user)
            idempotent2 <- store.upsert(user)
            newTwitterId = TwitterId(user.twitterId.value + "_2")
            differentId <- store.upsert(user.copy(twitterId = newTwitterId))
          } yield Claim(idempotent1 == idempotent2 && idempotent1 != differentId)
        ).unsafeRunSync()
      }
    }
  }
}
