package blueeyes.persistence.mongo

import scala.collection.IterableView
import MongoQueryBuilder._
import MongoImplicits._
import akka.dispatch.Future
import akka.dispatch.Await
import akka.util.Timeout
import blueeyes.json.JsonAST._
import blueeyes.json.{JsonParser, JPath, Printer}
import scalaz.Scalaz._
import java.util.concurrent.CountDownLatch

import org.streum.configrity.Configuration
import org.streum.configrity.io.BlockFormat

object MongoDemo extends MongoImplicits{
  implicit def queryTimeout = Timeout(30000)


  private val jObject  = JObject(JField("address", JObject( JField("city", JString("A")) :: JField("street", JString("1")) :: JField("code", JInt(1)) :: Nil)) :: JField("location", JArray(List(JInt(40), JInt(40), JString("40")))) :: Nil)
  private val jObject1 = JObject(JField("address", JObject( JField("city", JString("B")) :: JField("street", JString("2")) :: JField("code", JInt(2)) :: Nil)) :: JField("location", JArray(List(JInt(40), JInt(40)))) :: Nil)
  private val jObject2 = JObject(JField("address", JObject( JField("city", JString("C")) :: JField("street", JString("3")) :: JField("code", JInt(1)) :: Nil)) :: Nil)
  private val jObject3 = JObject(JField("address", JObject( JField("city", JString("E")) :: JField("street", JString("4")) :: JField("code", JInt(1)) :: Nil)) :: Nil)
  private val jObject6 = JObject(JField("address", JString("ll")) :: Nil)
  private val countJObject = JObject(JField("count", JInt(1)) :: Nil)
  private val count1 = JObject(JField("value", JString("1")) :: Nil)

//  private val jobjectsWithArray = JsonParser.parse("""{ name: "Next promo", inprogress: false, priority:0,  tasks : [ "select product", "add inventory", "do placement"]}""").asInstanceOf[JObject] :: Nil

  // Doesn't appear that configuration is even required here...
  // Not the following lines was added when the conversion from Configgy to Configrity
  // was made, but never tested given that it appears the Configgy loading
  // was largely dead code as well... NDM
  //val config = Configuration.load("/etc/default/blueeyes.conf", BlockFormat)

  val collection = "my-collection"

  val realMongo = new MockMongo()

  val database  = realMongo.database( "mydb" )

  val short  = """{"adId":"livingSocialV3","adCode":"xxxxxxxxxxx","properties":{"width":"300","height":"250","advertiserId":"all","backupImageUrl":"http://static.socialmedia.com/ads/LivingSocial/CupCake/LivingSocial_Baseline_DC.jpg","clickthroughUrl":"http://www.livingsocial.com","channelId":"livingSocialChannel","campaignId":"livingSocialCampaign","groupId":"group1","clickTag":""}}"""
  val long   = """{"adId":"livingSocialV3","adCode":"xxxxxxxxxxx","properties":{"width":"300","height":"250","advertiserId":"all","backupImageUrl":"http://static.socialmedia.com/ads/LivingSocial/CupCake/LivingSocial_Baseline_DC.jpg","clickthroughUrl":"http://www.livingsocial.com","channelId":"livingSocialChannel","campaignId":"livingSocialCampaign","groupId":"group1","clickTag":""}}                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   """

  def main(args: Array[String]){
//    database(remove.from(collection))

//    database(dropIndexes.on(collection))
//    try{
//      database(ensureUniqueIndex("index").on(collection, "address.city", "address.street"))
//      database(verified(insert(jObject).into(collection)))
//      database(verified(insert(jObject).into(collection)))
//    }
//    finally database(remove.from(collection))

//    benchamrk()
//    demoSelectOne

    demoSelect
    
//    demoUpdate1

//    demoRemove

//    demoDistinct

//    demoGroup
//    demoGeoNear
  }

  private def demoGeoNear{
    println("------------demoGeoNear------------------")
    for {
      _ <- database(ensureUniqueIndex("location").on("location").in(collection).geospatial("location"))
      _ <- database(insert(jObject, jObject3).into(collection))
      _ <- printObjects(database(select().from(collection).where((JPath("location") within Circle((30, 30), 45)) && JPath("address.city") === "A")))
      _ <- printObjects(database(select().from(collection).where(JPath("location") near (45, 45, Some(40)))))
      _ <- database(remove.from(collection))
    } yield {
      println("------------demoGeoNear------------------")
    }
  }

  private def demoSelect{
    println("------------demoSelect------------------")
    for {
      _ <- database(insert(jObject, jObject1).into(collection))
      _ <- printObjects(database(select().from(collection).where("address.city1" doesNotExist)))
      _ <- database(remove.from(collection))
    } yield {
      println("------------demoSelect------------------")
    }
  }

  private def demoDistinct{
    println("------------demoDistinct------------------")
    for {
      _ <- database(insert(jObject, jObject1, jObject2, jObject3).into(collection))
      _ <- printObjects(database(select().from(collection)))
      _ <- database(remove.from(collection))
    } yield {
      println("------------demoDistinct------------------")
    }
  }

  private def demoGroup{
    println("------------demoGroup------------------")
    val objects = JsonParser.parse("""{"address":{ "city":"A", "code":2, "street":"1"  } }""").asInstanceOf[JObject] :: JsonParser.parse("""{"address":{ "city":"A", "code":5, "street":"3"  } }""").asInstanceOf[JObject] :: JsonParser.parse("""{"address":{ "city":"C", "street":"1"  } }""").asInstanceOf[JObject] :: JsonParser.parse("""{"address":{ "code":3, "street":"1"  } }""").asInstanceOf[JObject] :: Nil
    database(insert(objects :_*).into(collection))

    val initial = JsonParser.parse("""{ "csum": 10.0 }""").asInstanceOf[JObject]
    val result  = database(group(initial, "function(obj,prev) { prev.csum += obj.address.code; }", "address.city").from(collection))
    
    printObject(result.map[Option[JArray]](v => Some(v)))

    database(remove.from(collection))
    println("------------demoGroup------------------")
  }

  private def demoUpdate0{
//    database(insert(jobjectsWithArray: _*).into(collection))

    printObjects(database(select().from(collection)))

    database(updateMany(collection).set("foo" pull (("shape" === "square") && ("color" === "purple")).elemMatch("")))

    printObjects(database(select().from(collection)))

    database(remove.from(collection))
  }

  private def demoUpdate1{
    database(insert(jObject).into(collection))

    printObjects(database(select().from(collection)))

    database(update(collection).set(("count" inc (3))))
    database(update(collection).set(JPath(".address.street") set ("Odinzova")) .where("address.city" === "A"))

    printObjects(database(select().from(collection)))

    database(remove.from(collection))
  }

  private def demoUpdate{
    import MongoUpdateBuilder._
    println("------------demoUpdate------------------")
    for {
      _ <- insertObjects
      _ <- database(updateMany(collection).set("address" popFirst))
      _ <- printObjects(database(select().from(collection)))
      _ <- database(update(collection).set(("address.city" unset) |+| ("address.street" set ("Another Street"))).where("address.city" === "C"))
      _ <- printObjects(database(select().from(collection)))
      _ <- database(update(collection).set(jObject3).where("address.city" === "A"))
      _ <- printObjects(database(select().from(collection)))
      _ <- database(updateMany(collection).set("address.street" set ("New Street")))
      _ <- printObjects(database(select().from(collection)))
      _ <- database(remove.from(collection))
    } yield {
      println("------------demoUpdate------------------")
    }
  }

  private def demoSelectOne{
    println("------------demoSelectOne------------------")

    for {
      _ <- insertObjects
      _ <- printObject(database(selectOne().from(collection).sortBy("address.city" <<)))
      _ <- printObject(database(selectOne().from(collection).sortBy("address.city" >>)))
      _ <- printObject(database(selectOne().from(collection).where("address.city" === "B").sortBy("address.city" >>)))
      _ <- printObject(database(selectOne("address.city").from(collection).sortBy("address.city" >>)))
      _ <- printObject(database(selectOne("address.city").from(collection).sortBy("address.city" <<)))
      _ <- database(remove.from(collection))
    } yield {
      println("------------demoSelectOne------------------")
    }
  }

  def printObjects(future: Future[IterableView[JObject, Iterator[JObject]]]) = {
    future onSuccess { case objects =>
      println("------------------------------------------------")
      println(objects.map(v => Printer.pretty(Printer.render(v))).mkString("\n"))
      println("------------------------------------------------")
    }
  }

  private def printObject[T <: JValue](future: Future[Option[T]]) = {
    future onSuccess { case objects => 
      println("------------------------------------------------")
      println(objects.map(v => Printer.pretty(Printer.render(v))).mkString("\n"))
      println("------------------------------------------------")
    }
  }

  private def demoRemove{
    println("------------demoRemove------------------")

    for {
      _ <- insertObjects
      _ <- database(remove.from(collection).where("address.city" === "A"))
      _ <- database(remove.from(collection))
    } yield {
      println("------------demoRemove------------------")
    }
  }

  private def insertObjects = {
    database(insert(jObject, jObject).into(collection))
  }
}

//db.foo.insert({"address":{ "city":"A", "code":2, "street":"1"  } })
//db.foo.insert({"address":{ "city":"C", "street":"1"  } })
//db.foo.insert({"address":{ "code":33, "street":"5"  } })
//
//db.foo.group({key: { "address.city":1 }, cond: {}, reduce: function(a,b) { b.csum += a.address.code; return b;}, initial: { csum: 0 } })


//{ group: { key: { address.city: 1.0 }, cond: {}, initial: { csum: 12.0 }, $reduce: function (obj, prev) {prev.csum += obj.address.code;} } }
//{ group: { key: { address.city: 1.0 }, cond: {}, initial: { csum: 10 }, $reduce: "function(obj,prev) { prev.csum += obj.address.code;}" } }
