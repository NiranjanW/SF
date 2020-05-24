package consumer



import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.concurrent.Future
import consumer.Settings.{consumerSettings, producerSettings}

object Settings {
  def consumerSettings(implicit system: ActorSystem) =
    ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("CommittableSourceConsumer")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def producerSettings(implicit system: ActorSystem) =
    ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")
}

object CommittableSource extends App {

  type KafkaMessage = CommittableMessage[Array[Byte], String]

  implicit val system = ActorSystem("CommittableSourceConsumerMain")
  implicit val materializer = ActorMaterializer()

  implicit val ec = system.dispatcher

  // explicit commit
  Consumer
    .committableSource(consumerSettings, Subscriptions.topics("topic1"))
    .mapAsync(1) { msg =>
      BusinessController.handleMessage(msg.record.value)
        .flatMap(response => msg.committableOffset.commitScaladsl())
        .recoverWith { case e => msg.committableOffset.commitScaladsl() }
    }
    .runWith(Sink.ignore)

}

object BusinessController {

  type Service[A, B] = A => Future[B]

  val handleMessage: Service[String, String] =
    (message) => Future.successful(message.toUpperCase)

} 