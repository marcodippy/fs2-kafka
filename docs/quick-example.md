---
id: quick-example
title: Quick Example
---

Following is an example showing how to:

- use `consumerStream` in order to stream records from Kafka,
- use `producerStream` to produce newly created records to Kafka,
- use `commitBatchWithinF` to commit consumed offsets in batches.

```scala mdoc
import cats.Id
import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.functor._
import fs2.Stream
import fs2.kafka._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

import scala.concurrent.duration._

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    def processRecord(record: ConsumerRecord[String, String]): IO[(String, String)] =
      IO.pure(record.key -> record.value)

    val stream =
      for {
        consumer <- consumerStream[IO].using {
          ConsumerSettings(
            keyDeserializer = new StringDeserializer,
            valueDeserializer = new StringDeserializer
          )
          .withAutoOffsetReset(AutoOffsetReset.Earliest)
          .withBootstrapServers("localhost")
          .withGroupId("group")
        }
        producer <- producerStream[IO].using {
          ProducerSettings(
            keySerializer = new StringSerializer,
            valueSerializer = new StringSerializer
          )
          .withBootstrapServers("localhost")
        }
        _ <- Stream.eval(consumer.subscribe(NonEmptyList.one("topic")))
        _ <- consumer.stream
          .mapAsync(25)(message =>
            processRecord(message.record)
              .map {
                case (key, value) =>
                  val record = new ProducerRecord("topic", key, value)
                  ProducerMessage.single[Id].of(record, message.committableOffset)
              })
            .evalMap(producer.produceBatched)
            .map(_.map(_.passthrough))
            .to(commitBatchWithinF(500, 15.seconds))
      } yield ()

    stream.compile.drain.as(ExitCode.Success)
  }
}
```