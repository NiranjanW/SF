package consumer

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
//import com.scalaio.kafka.consumer.Settings.consumerSettings
//import consumer.Settings.{consumerSettings, producerSettings}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.control.NonFatal
import consumer.SourceSettings.{consumerSettings}

object SourceSettings {

  def consumerSettings(implicit system: ActorSystem) =
    ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("PlainSourceConsumer")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
      .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")


}

object PlainSourceConsumerMain extends App {

  lazy val logger = LoggerFactory.getLogger(getClass)

  implicit val system = ActorSystem("PlainSourceConsumerMain")

  implicit val ec = system.dispatcher

  val decider: Supervision.Decider = {
    case _: IllegalStateException => Supervision.Resume
    case _ => Supervision.Stop
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withSupervisionStrategy(decider)
  )

  // plainSource, don't do auto commit by default!!!, see consumerSettings
  Consumer
    .plainSource(consumerSettings, Subscriptions.topics("topic1"))
    .mapAsync(1)(record => SourceBusinessController.handleMessage(record.value()))
    .runWith(Sink.ignore)

  // Handles failures
  val (control, streamFuture) = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("topic1"))
    .mapAsync(1)(record => SourceBusinessController.handleMessage(record.value()))
    .toMat(Sink.ignore)(Keep.both)
    .run()

  streamFuture.onFailure {
    case NonFatal(e) => logger.error("error streaming", e)
  }

  val shutdownFuture = control.shutdown()

}

object SourceBusinessController {
  type Service[A, B] = A => Future[B]

  val handleMessage: Service[String, String] =
    (message) => Future.successful(message.toUpperCase)

}

