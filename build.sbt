name := "prism"

version := "1.0-SNAPSHOT"

resolvers += "Guardian Github Snapshots" at "http://guardian.github.com/maven/repo-releases"

libraryDependencies ++= Seq(
    "com.google.code.findbugs" % "jsr305" % "2.0.0",
    "org.apache.jclouds" % "jclouds-all" % "1.7.0",
    "com.amazonaws" % "aws-java-sdk" % "1.7.7",
    "com.gu" %% "management-play" % "6.0" exclude("javassist", "javassist"), // http://code.google.com/p/reflections/issues/detail?id=140
    "com.gu" %% "configuration" % "3.9",
    "com.typesafe.akka" %% "akka-agent" % "2.1.2",
    filters
)

scalacOptions ++= Seq("-feature")

play.Project.playScalaSettings

com.gu.deploy.MagentaArtifact.magentaArtifactSettings
