package conf

import java.net.InetAddress

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.{DescribeTagsRequest => EC2DescribeTagsRequest, DescribeInstancesRequest, Filter}
import com.amazonaws.util.EC2MetadataUtils
import com.gu.management.Loggable
import scala.collection.JavaConverters._
import scala.util.Try

object AWS extends Loggable {

  // This is to detect if we are running in AWS or on GC2. The 169.254.169.254
  // thing works on both but this DNS entry seems peculiar to AWS.
  lazy val isAWS = Try(InetAddress.getByName("instance-data")).isSuccess
  def awsOption[T](f: => T): Option[T] = if (isAWS) Option(f) else None

  lazy val connectionRegion = instance.region.getOrElse(Region.getRegion(Regions.EU_WEST_1))

  lazy val EC2Client = connectionRegion.createClient(classOf[AmazonEC2Client], null, null)

  type Tag = (String, String)

  object instance {
    lazy val id:Option[String] = awsOption(EC2MetadataUtils.getInstanceId)
    lazy val region:Option[Region] = awsOption(Regions.getCurrentRegion)
    lazy val allTags:Map[String,String] =
      id.toSeq.flatMap { id =>
        val tagsResult = AWS.EC2Client.describeTags(
          new EC2DescribeTagsRequest().withFilters(
            new Filter("resource-type").withValues("instance"),
            new Filter("resource-id").withValues(id)
          )
        )
        tagsResult.getTags.asScala.map{td => td.getKey -> td.getValue }
      }.toMap
    lazy val customTags = allTags.filterKeys(!_.startsWith("aws:"))
    lazy val identity = (customTags.get("Stack"), customTags.get("App"), customTags.get("Stage")) match {
      case (Some(stack), Some(app), Some(stage)) => Some(Identity(stack, app, stage))
      case _ => None
    }
  }

  object instanceLookup {

    def addressesFromTags(tags: List[Tag]): List[String] = {

      logger.info(s"Looking up instances with tags: $tags")
      val tagsAsFilters = tags.map{
        case(name, value) => new Filter("tag:" + name).withValues(value)
      }.asJavaCollection

      val describeInstancesResult = EC2Client.describeInstances(new DescribeInstancesRequest().withFilters(tagsAsFilters))

      val reservation = describeInstancesResult.getReservations.asScala.toList
      val instances = reservation.flatMap(r => r.getInstances.asScala)
      val addresses = instances.flatMap(i => Option(i.getPrivateIpAddress))
      logger.info(s"Instances with tags $tags: $addresses")
      addresses
    }
  }

}
