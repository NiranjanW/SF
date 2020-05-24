package producer

import java.io.File

import akka.actor.ActorSystem
import akka.kafka.ConsumerMessage.CommittableOffsetBatch
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ProducerSettings, _}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import producer.Settings.{consumerSettings, producerSettings}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, ByteArraySerializer, StringDeserializer, StringSerializer}

import scala.util.{Failure, Success}
import slick.basic.DatabasePublisher
import slick.jdbc.H2Profile.api._

object Settings {

  def consumerSettings(implicit system: ActorSystem) =
    ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers("localhost:9092")
      .withGroupId("CommitConsumerToFlowProducer")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

  def producerSettings(implicit system: ActorSystem) =
    ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
      .withBootstrapServers("localhost:9092")
}

object ProducerMain extends App {
  implicit val system = ActorSystem("CommitConsumerToFlowProducerMain")
  implicit val materializer = ActorMaterializer()

  val names = List("Hugo", "Paco", "Luis")

  // 1. simplest way to produce - plainSink
  Source(names)
    .map(_.toUpperCase)
    .map { elem => new ProducerRecord[Array[Byte], String]("topic1", elem)}
    .runWith(Producer.plainSink(producerSettings))

  // 2. consume and produce
  Consumer
    .committableSource(consumerSettings, Subscriptions.topics("topic1"))
    .map { msg =>
      println(s"topic1 -> topic2: $msg")
      ProducerMessage.Message(new ProducerRecord[Array[Byte], String](
        "topic2",
        msg.record.value
      ), msg.committableOffset)
    }
    .via(Producer.flow(producerSettings))
    .mapAsync(producerSettings.parallelism) { result =>
      result.message.passThrough.commitScaladsl()
    }
    .runWith(Sink.ignore)

  // 3. consume and produce with batch
  Consumer.committableSource(consumerSettings, Subscriptions.topics("topic1"))
    .map(msg =>
      ProducerMessage.Message(new ProducerRecord[Array[Byte], String]("topic2", msg.record.value),
        msg.committableOffset))
    .via(Producer.flow(producerSettings))
    .map(_.message.passThrough)
    .batch(max = 20, first => CommittableOffsetBatch.empty.updated(first)) { (batch, elem) =>
      batch.updated(elem)
    }
    .mapAsync(3)(_.commitScaladsl())
    .runWith(Sink.ignore)


  //Niran's code
  val db = Database.forConfig("mydb")

//  val db = new DB
//  Consumer.committableSource(consumerSettings,Subscriptions.topics("Niran1"))
//    .mapAsync(1){
//      msg => db.update(msg.record.value()).map ( _ => msg)
//    }
//    .mapAsync(1){ msg =>
//      msg.commitableOfffset.commitScaladsl()
//
//    }
//    .runWith(Sink.ignore)
//
//  //Batch commitable Source
//
//  Consumer.committableSource(consumerSettings,Subscriptions.topics("Niran1"))
//    .mapAsync(5)(processAsync)
//    .groupedWithin(100 , 5 seconds)
//    .map (group => group.foldLeft(CommittableOffsetBatch.empty){
//      (batch , elem) => batch.updated(elem.committableOffset)
//    })
//    .mapAsync(1)(_.commitScaladsl())
//    .runWith(Sink.ignore)
//
//  //External commit Source kind of Exactly Once delivery - not recomm
//  // dont use trans system for state
// db.loadOffset().foreach { fromOffset =>
//   val partition = 0
//   val subscription = Subscriptions.assignmentWithOffset(
//     new TopicPartition("Niran1",partition) -> fromOffset
//   )
//   Consumer.plainSource(consumerSettings ,subscription  )
//     .mapAsync(1)(db.save)
//     .runWith(Sink.ignore)
// }

//  Kafka as a Flow ( similar to sink) use via combinator
//  run will call materilaize values - control and Future

  /**
    * Get a recursive listing of all files underneath the given directory.
    * from stackoverflow.com/questions/2637643/how-do-i-list-all-files-in-a-subdirectory-in-scala
    */
  def getRecursiveListOfFiles(dir: File): Array[File] = {
    val these = dir.listFiles
    these ++ these.filter(_.isDirectory).flatMap(getRecursiveListOfFiles)
  }


  def getFileTree(f: File): Stream[File] =
    f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree)
    else Stream.empty)


  def readFiles ( file :File):Unit = {

    val fileHandler = Source

  }
}


