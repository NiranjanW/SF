import java.io.File

import akka.stream.ActorMaterializer
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import akka.stream.scaladsl.Source
//import scala.collection.map
import scala.annotation.tailrec
import scala.collection.JavaConverters._


def getFileTree(f: File): Stream[File] =
  f #:: (if (f.isDirectory) f.listFiles().toStream.flatMap(getFileTree)
  else Stream.empty)

object Demo {

  def main(args: Array[String]): Unit = {
    val resourcesPath = getClass.getResource("/order.lco_01.json")
    println(resourcesPath.getPath)

  }
}

val resourcesPath = getClass.getResource("/application.conf")
println(resourcesPath.getPath)

val path = getClass.getResource("/resources")
val folder = new File(path.getPath)
if (folder.exists && folder.isDirectory)
  folder.listFiles
    .toList
    .foreach(file => println(file.getName))

val fileStream =  getClass.getResourceAsStream("/order.lco_01.json")
val lines = scala.io.Source.fromInputStream(fileStream).getLines
lines.foreach(line => println(line))

//print(getFileTree("C:\Scala\project\SF"))

def using[A,B <: {def close():Unit} ](closeable: B) (f: B => A): A =
  try { f(closeable) } finally { closeable.close() }


val source = Source(1 to 3)
val sink = Sink.foreach[Int](println)

val invert = Flow[Int].map(elem => elem * -1)
val doubler = Flow[Int].map(elem => elem * 2)

// join them all together
val runnable = source via invert via doubler to sink

// run them

implicit val system = ActorSystem("QuickStart")
implicit val materializer = ActorMaterializer()
runnable.run()

object quickSort extends App {

  val r = scala.util.Random
  val rndArray =  (for (i <- 1 to 100) yield r.nextInt(1000)).toArray
  println(rndArray)

  val sortedArray = qSort(rndArray)

  println (sortedArray)
  def qSort ( xs : Array[Int]) : Array [Int] = {

    if (xs.length <= 1) xs
    val pivot = xs(xs.length / 2)
    Array.concat(
      qSort(xs filter (pivot >)),
      xs filter (pivot ==),
      qSort(xs filter (pivot <))

    )
  }

  def mSort( xs : List[Int]) : List [Int] = xs match {

    case Nil => Nil
    case xs ::Nil => List(xs)
    case _ =>
       val (left, right ) = xs splitAt (xs.length) / 2
         merge (mSort(left), mSort(right))


  }

  val mList = rndArray.toList

  def merge (xs1:List[Int] , ys1:List[Int] ) :List[Int]= (xs1 , ys1) match{
    case (Nil, ys) => ys
    case (xs , Nil) => xs
    case( x::xs,y::ys) =>
        if(x<y) x::merge(xs , ys1)
        else y::merge(xs1 ,ys)
  }

  @tailrec
  def merge(seq1: List[Int], seq2: List[Int], accumulator: List[Int] = List()):List[Int] = (seq1, seq2) match {
    case (Nil, _) => accumulator ++ seq2
    case (_, Nil) => accumulator ++ seq1
    case (x::xs, y::ys) =>
      if(x<y) merge(xs,seq2, accumulator :+ x)
      else merge(seq1,ys, accumulator :+ y)
  }


}
