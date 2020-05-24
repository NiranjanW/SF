name := "SF"

version := "0.1"

scalaVersion := "2.12.7"

libraryDependencies  ++=Seq( "com.typesafe.akka" %% "akka-stream-kafka" % "1.0-RC1",
"com.typesafe.slick" %% "slick" % "3.3.2",
"org.slf4j" % "slf4j-nop" % "1.6.4",
"com.typesafe.slick" %% "slick-hikaricp" % "3.3.2",
"org.postgresql" % "postgresql" % "9.4-1206-jdbc42"
)