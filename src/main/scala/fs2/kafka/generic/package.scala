package fs2.kafka

import cats.effect._
import cats.implicits._
import shapeless._

package object generic {

  implicit def deserializeCNil[F[_]: Sync]: Deserializer[F, CNil] =
    Deserializer.failWith[F, CNil]("impossible")

  implicit final def deserializeCCons[F[_]: Sync, L, R <: Coproduct](
    implicit
    deserializeL: Deserializer[F, L],
    deserializeR: Deserializer[F, R]
  ): Deserializer[F, L :+: R] =
    deserializeL.map(Inl(_): L :+: R).orElse(deserializeR.map(Inr(_)))

  implicit def serializeCNil[F[_]: Sync]: Serializer[F, CNil] =
    Serializer.failWith[F, CNil]("impossible")

  implicit final def serializeCCons[F[_]: Sync, L, R <: Coproduct](
    implicit
    serializeL: Serializer[F, L],
    serializeR: Serializer[F, R]
  ): Serializer[F, L :+: R] =
    Serializer.instance[F, L :+: R] {
      case (topic, headers, a) =>
        a match {
          case Inl(l) => serializeL.serialize(topic, headers, l)
          case Inr(r) => serializeR.serialize(topic, headers, r)
        }
    }
}
