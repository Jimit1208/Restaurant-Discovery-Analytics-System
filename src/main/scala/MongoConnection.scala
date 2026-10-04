import org.mongodb.scala.MongoClient
import org.mongodb.scala.MongoDatabase

object MongoConnection {

  private val uri: String = sys.env.getOrElse(
    "MONGODB_URI",
    throw new RuntimeException(
      "MONGODB_URI environment variable is not set."
    )
  )

  private val client: MongoClient = MongoClient(uri)

  val database: MongoDatabase =
    client.getDatabase("sample_restaurants")

  val restaurants =
    database.getCollection("restaurants")

  def close(): Unit = {
    client.close()
  }
}