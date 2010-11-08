package blueeyes.persistence.mongo

import org.specs.Specification
import blueeyes.json.JPathImplicits._
import MongoFilterOperators._
import com.mongodb.MongoException
import MockMongoImplementation._
import MongoImplicits._
import blueeyes.json.{JsonParser, JPath}
import blueeyes.json.JsonAST.{JValue, JString, JField, JObject}

class MockDatabaseCollectionSpec extends Specification{
  private val jObject  = JObject(JField("address", JObject( JField("city", JString("A")) :: JField("street", JString("1")) ::  Nil)) :: Nil)
  private val jObject1 = JObject(JField("address", JObject( JField("city", JString("B")) :: JField("street", JString("2")) ::  Nil)) :: Nil)
  private val jObject2 = JObject(JField("address", JObject( JField("city", JString("B")) :: JField("street", JString("3")) ::  Nil)) :: Nil)
  private val jObject3 = JObject(JField("address", JObject( JField("city", JString("C")) :: JField("street", JString("4")) ::  Nil)) :: Nil)
  private val jobjects = jObject :: jObject1 :: jObject2 :: jObject3 :: Nil

  private val jobjectsWithArray = JsonParser.parse("""{ "foo" : [
      {
        "shape" : "square",
        "color" : "purple",
        "thick" : false
      },
      {
        "shape" : "circle",
        "color" : "red",
        "thick" : true
      }
] } """).asInstanceOf[JObject] :: JsonParser.parse("""
{ "foo" : [
      {
        "shape" : "square",
        "color" : "red",
        "thick" : true
      },
      {
        "shape" : "circle",
        "color" : "purple",
        "thick" : false
      }
] }""").asInstanceOf[JObject] :: Nil

  private val sort     = MongoSort("address.street", MongoSortOrderDescending)

  "store jobjects" in{
    val collection = newCollection

    collection.insert(jobjects)
    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(jobjects)
  }
  "does not store jobject when unique index exists and objects are the same" in{
    val collection = newCollection

    collection.ensureIndex("index", JPath("address.city") :: JPath("address.street") :: Nil, true)
    collection.insert(jObject :: jObject :: Nil) must throwA[MongoException]

    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(Nil)
  }
  "does not store jobject when unique index exists and the same object exists" in{
    val collection = newCollection

    collection.ensureIndex("index", JPath("address.city") :: Nil, true)
    collection.insert(jObject1 :: Nil)
    collection.insert(jObject2 :: Nil) must throwA[MongoException]

    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(jObject1 :: Nil)
  }
  "store jobject when unique index exists and objects are different" in{
    val collection = newCollection

    collection.ensureIndex("index", JPath("address.city") :: JPath("address.street") :: Nil, true)
    collection.insert(jObject :: jObject1 :: Nil)
    collection.insert(jObject2 :: jObject3 :: Nil)

    collection.select(MongoSelection(Nil), None, Some(sort), None, None) mustEqual(jobjects.reverse)
  }
  "update jobject field" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jObject1 :: Nil)
    collection.update(None, "address.street" set ("3"), false, false)

    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(jObject2 :: Nil)
  }
  "update not existing jobject field" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jObject1 :: Nil)
    collection.update(None, "name" set ("foo"), false, false)

    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(jObject1.merge(JObject(JField("name", JString("foo")) :: Nil)) :: Nil)
  }
  "update jobject fields" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jObject :: Nil)
    collection.update(None, ("address.city" set ("B")) & ("address.street" set ("3")), false, false)

    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(jObject2 :: Nil)
  }
  "update all objects" in{
    val collection = newCollection

    collection.insert(jObject :: jObject1 :: Nil)
    collection.update(None, jObject2, false, true)

    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(jObject2 :: jObject2:: Nil)
  }
  "update object by filter" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jObject :: jObject1 :: Nil)
    collection.update(Some(MongoFieldFilter("address.city", $eq,"A")), jObject2, false, true)

    collection.select(MongoSelection(Nil), None, Some(sort), None, None) mustEqual(jObject2 :: jObject1  :: Nil)
  }
  "update only one object when multi is false" in{
    val collection = newCollection

    collection.insert(jObject :: jObject1 :: Nil)
    collection.update(None, MongoUpdateObject(jObject2), false, false)

    val result = collection.select(MongoSelection(Nil), None, None, None, None)
    result.contains(jObject2) must be (true)
    (if (result.contains(jObject)) !result.contains(jObject1) else result.contains(jObject1)) must be (true)
  }
  "removes all jobjects when filter is not specified" in{
    val collection = newCollection

    collection.insert(jobjects)
    collection.remove(None) mustEqual(4)
    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(Nil)
  }
  "count all jobjects when filter is not specified" in{
    val collection = newCollection

    collection.insert(jobjects)
    collection.count(None) mustEqual(4)
  }
  "count jobjects which match filter" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jobjects)
    collection.count(Some(MongoFieldFilter("address.city", $eq,"A"))) mustEqual(1)
  }
  "removes jobjects which match filter" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jobjects)
    collection.remove(Some(MongoFieldFilter("address.city", $eq,"A"))) mustEqual(1)
    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(jObject1 :: jObject2 :: jObject3 :: Nil)
  }
  "select all jobjects when filter is not specified" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jobjects)
    collection.select(MongoSelection(Nil), None, None, None, None) mustEqual(jobjects)
  }
  "select jobjects by filter" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jobjects)
    collection.select(MongoSelection(Nil), Some(MongoFieldFilter("address.city", $eq,"A")), None, None, None) mustEqual(jObject :: Nil)
  }
  "select jobjects with array when array element field filter is specified" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jobjectsWithArray)
    collection.select(MongoSelection(Nil), Some(MongoFieldFilter("foo.shape", $eq,"square") && MongoFieldFilter("foo.color", $eq,"purple")), None, None, None) mustEqual(jobjectsWithArray)
  }
  "select jobjects with array when array element filter is specified" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jobjectsWithArray)
    val filterObject = JsonParser.parse(""" {"shape" : "square", "color" : "purple","thick" : false} """).asInstanceOf[JObject]
    collection.select(MongoSelection(Nil), Some(MongoFieldFilter("foo", $eq, filterObject)), None, None, None) mustEqual(List(jobjectsWithArray.head).toStream)
  }
  "select jobjects with array when elemMatch filter is specified" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jobjectsWithArray)
    collection.select(MongoSelection(Nil), Some(MongoElementsMatchFilter("foo", (MongoFieldFilter("shape", $eq,"square") && MongoFieldFilter("color", $eq,"red")))), None, None, None) mustEqual(List(jobjectsWithArray.tail.head).toStream)
  }

  "does not select jobjects with array when wrong array element filter is specified" in{
    import MongoFilterImplicits._
    val collection = newCollection

    collection.insert(jobjectsWithArray)
    val filterObject = JsonParser.parse(""" {"shape" : "square", "color" : "purple"} """).asInstanceOf[JObject]
    collection.select(MongoSelection(Nil), Some(MongoFieldFilter("foo", $eq, filterObject)), None, None, None) mustEqual(Nil.toStream)
  }
  "select ordered objects" in{
    import MongoFilterImplicits._
    val collection  = newCollection

    collection.insert(jobjects)
    collection.select(MongoSelection(Nil), None, Some(sort), None, None) mustEqual(jobjects.reverse)
  }
  "skip objects" in{
    import MongoFilterImplicits._
    val collection  = newCollection

    collection.insert(jobjects)
    collection.select(MongoSelection(Nil), None, Some(sort), Some(2), None) mustEqual(jObject1 :: jObject:: Nil)
  }
  "limit objects" in{
    import MongoFilterImplicits._
    val collection  = newCollection

    collection.insert(jobjects)
    collection.select(MongoSelection(Nil), None, Some(sort), Some(2), Some(1)) mustEqual(jObject1:: Nil)
  }

  "select specified objects fields" in{
    import MongoFilterImplicits._
    val collection  = newCollection

    collection.insert(jObject :: jObject1 :: Nil)
    val fields  = JObject(JField("address", JObject(JField("city", JString("A")) :: Nil)) :: Nil)
    val fields1 = JObject(JField("address", JObject(JField("city", JString("B")) :: Nil)) :: Nil)
    collection.select(MongoSelection(JPath("address.city") :: Nil), None, Some(sort), None, None) mustEqual(fields1 :: fields :: Nil)
  }
  "select specified objects fields when some fields are missing" in{
    import MongoFilterImplicits._
    val collection  = newCollection

    collection.insert(jObject :: jObject1 :: Nil)
    val fields  = JObject(JField("address", JObject(JField("city", JString("A")) :: Nil)) :: Nil)
    val fields1 = JObject(JField("address", JObject(JField("city", JString("B")) :: Nil)) :: Nil)
    collection.select(MongoSelection(JPath("address.city") :: JPath("address.phone") :: Nil), None, Some(sort), None, None) mustEqual(fields1 :: fields :: Nil)
  }
  "select nothing when wwong selection is specified" in{
    import MongoFilterImplicits._
    val collection  = newCollection

    collection.insert(jObject :: jObject1 :: Nil)
    val fields  = JObject(JField("address", JObject(JField("city", JString("A")) :: Nil)) :: Nil)
    val fields1 = JObject(JField("address", JObject(JField("city", JString("B")) :: Nil)) :: Nil)
    collection.select(MongoSelection(JPath("address.town") :: Nil), None, Some(sort), None, None) mustEqual(Nil)
  }

  private def newCollection = new MockDatabaseCollection()
}