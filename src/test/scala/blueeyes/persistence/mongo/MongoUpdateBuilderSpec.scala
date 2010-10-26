package blueeyes.persistence.mongo

import org.spex.Specification
import MongoImplicits._
import blueeyes.json.JPathImplicits._
import MongoUpdateOperators._
import MongoFilterOperators._
import blueeyes.json.JsonAST._

class MongoUpdateBuilderSpec extends Specification{
  "builds $inc operation" in {
    import MongoFilterImplicits._
    ("n" inc (1) toJValue) mustEqual (JObject(JField("$inc", JObject(JField("n", JInt(1)) :: Nil)) :: Nil))
  }
  "builds $set operation" in {
    import MongoFilterImplicits._
    ("n" set (1) toJValue) mustEqual (JObject(JField("$set", JObject(JField("n", JInt(1)) :: Nil)) :: Nil))
  }
  "builds $unset operation" in {
    import MongoFilterImplicits._
    (("n" unset) toJValue) mustEqual (JObject(JField("$unset", JObject(JField("n", JInt(1)) :: Nil)) :: Nil))
  }
  "builds popLast operation" in {
    import MongoFilterImplicits._
    (("n" popLast) toJValue) mustEqual (JObject(JField("$pop", JObject(JField("n", JInt(1)) :: Nil)) :: Nil))
  }
  "builds popFirst operation" in {
    import MongoFilterImplicits._
    (("n" popFirst) toJValue) mustEqual (JObject(JField("$pop", JObject(JField("n", JInt(-1)) :: Nil)) :: Nil))
  }

  "builds $push operation" in {
    import MongoFilterImplicits._
    ("n" push (1) toJValue) mustEqual (JObject(JField("$push", JObject(JField("n", JInt(1)) :: Nil)) :: Nil))
  }
  "builds $pull operation with default operator" in {
    import MongoFilterImplicits._
    ("n" pull (1) toJValue) mustEqual (JObject(JField("$pull", JObject(JField("n", JInt(1)) :: Nil)) :: Nil))
  }
  "builds $pull operation with custom operator" in {
    import MongoFilterImplicits._
    ("n" pull (1, $gt) toJValue) mustEqual (JObject(JField("$pull", JObject(JField("n", JObject(JField("$gt", JInt(1)) :: Nil)) :: Nil)) :: Nil))
  }
  "builds $pull operation with custom jValue" in {
    import MongoFilterImplicits._
    ("n" pull (JObject(JField("field", JInt(2)) :: Nil)) toJValue) mustEqual (JObject(JField("$pull", JObject(JField("n", JObject(JField("field", JInt(2)) :: Nil)) :: Nil)) :: Nil))
  }
  "builds $pullAll operation" in {
    import MongoFilterImplicits._
    ("n" pullAll (MongoPrimitiveString("foo"), MongoPrimitiveString("bar")) toJValue) mustEqual (JObject(JField("$pullAll", JObject(JField("n", JArray(JString("foo") :: JString("bar") :: Nil)) :: Nil)) :: Nil))
  }
  "builds $addToSet operation for one element" in {
    import MongoFilterImplicits._
    ("n" addToSet (MongoPrimitiveString("foo")) toJValue) mustEqual (JObject(JField("$addToSet", JObject(JField("n", JString("foo")) :: Nil)) :: Nil))
  }
  "builds $addToSet operation for several element" in {
    import MongoFilterImplicits._
    ("n" addToSet (MongoPrimitiveString("foo"), MongoPrimitiveString("bar")) toJValue) mustEqual (JObject(JField("$addToSet", JObject(JField("n", JObject(JField("$each", JArray(JString("foo") :: JString("bar") :: Nil)) :: Nil)) :: Nil)) :: Nil))
  }
}