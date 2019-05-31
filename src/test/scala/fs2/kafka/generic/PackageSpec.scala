package fs2.kafka.generic

import cats.effect._
import fs2.kafka._
import org.scalatest._

class PackageSpec extends FunSuite with Assertions with Matchers {
  test("Coproduct roundtrip") {
    import shapeless._

    type TestCoproduct = Int :+: String :+: CNil

    val deserializer = Deserializer[IO, TestCoproduct]
    val serializer = Serializer[IO, TestCoproduct]

    val stringVal = Coproduct[TestCoproduct]("aString")
    val intVal = Coproduct[TestCoproduct](1)

    deserializer
      .deserialize(
        "topic",
        Headers.empty,
        serializer.serialize("topic", Headers.empty, stringVal).unsafeRunSync()
      )
      .unsafeRunSync shouldBe stringVal

    deserializer
      .deserialize(
        "topic",
        Headers.empty,
        serializer.serialize("topic", Headers.empty, intVal).unsafeRunSync()
      )
      .unsafeRunSync shouldBe intVal

  }
}
