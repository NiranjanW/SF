import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Source
import com.typesafe.config.ConfigFactory
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.internals.ProduceRequestResult
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future

object  KafkaProducer extends  App{
  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()

//  val bootstrapServers = "localhost:9092"
//  val kafkaTopic = "akka_streams_topic"
//  val partition = 0
//  val subscription = Subscriptions.assignment(new TopicPartition(kafkaTopic, partition))


  val config = ConfigFactory.load.getConfig("akka.kafka.producer")

  val producerSettings =
    ProducerSettings(config, new StringSerializer , new StringSerializer)
      .withBootstrapServers("localhost:9092")

  val producerSink:Future[Done] =
    Source (1 to 10)
    .map(_.toString)
    .map(value => new ProducerRecord[String,String]( "Niran" , value))
    .runWith(Producer.plainSink(producerSettings))


}
