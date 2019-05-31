package fs2.kafka

import cats.effect._
import io.confluent.kafka.serializers._

package object avro {
  import com.sksamuel.avro4s._

  def avroSerialiser[F[_], O](
    avroSerialiser: KafkaAvroSerializer
  )(
    implicit F: Sync[F],
    encoder: Encoder[O],
    schema: SchemaFor[O]
  ): Serializer[F, O] =
    Serializer.instance[F, O] { (topic, _, a) =>
      F.delay {
        avroSerialiser.serialize(topic, encoder.encode(a, schema.schema))
      }
    }

  def avroDeserialiser[F[_], O](
    avroDeserialiser: KafkaAvroDeserializer
  )(
    implicit F: Sync[F],
    decoder: Decoder[O],
    schema: SchemaFor[O]
  ): Deserializer[F, O] =
    Deserializer.instance[F, O] { (topic, _, bytes) =>
      F.delay {
        val record = avroDeserialiser.deserialize(topic, bytes, schema.schema)
        decoder.decode(record, schema.schema)
      }
    }
}
