package blueeyes.persistence.mongo

import org.specs2.mutable.Specification
import MongoQueryBuilder._
import org.mockito.Matchers._
import blueeyes.json.JsonAST._
import org.specs2.mock.Mockito

class RemoveQueryBehaviourSpec extends Specification with Mockito {

  "Call collection method" in{
    val collection  = mock[DatabaseCollection]

    collection.getLastError returns None

    val filter = Some("name" === "Joe")

    val query  = remove.from("collection").where("name" === "Joe")
    query(collection)

    there was one(collection).remove(filter)
  }
  "Call collection method with dummy JObject when filter is not specified" in{
    val collection  = mock[DatabaseCollection]

    collection.getLastError returns None

    val query = remove.from("collection")
    query(collection)

    there was one(collection).remove(None)
  }
}
