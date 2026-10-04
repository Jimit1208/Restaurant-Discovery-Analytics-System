import com.sun.net.httpserver.{HttpExchange, HttpServer}

import org.mongodb.scala.Document
import org.bson.{Document => BsonDocument}

import java.io.{InputStream, OutputStream}
import java.net.{InetSocketAddress, URLDecoder}
import java.nio.charset.StandardCharsets
import scala.util.Try

object WebServer {

  private val repository = new RestaurantRepository()
  private val analytics = new RestaurantAnalytics()

  def start(port: Int): Unit = {

    repository.createIndexes()

    val server =
      HttpServer.create(
        new InetSocketAddress(port),
        0
      )

    server.createContext("/", handleRequest)

    server.setExecutor(null)

    println()
    println("==============================================")
    println(" RESTAURANT DISCOVERY & ANALYTICS WEB SERVER")
    println("==============================================")
    println(s"Running at: http://localhost:$port")
    println("Press Ctrl+C to stop.")
    println()

    server.start()
  }

  private def handleRequest(
      exchange: HttpExchange
  ): Unit = {

    try {

      addCorsHeaders(exchange)

      val path =
        exchange.getRequestURI.getPath

      val method =
        exchange.getRequestMethod.toUpperCase

      if (path.startsWith("/api/")) {

        handleApi(
          exchange,
          method,
          path
        )

      } else {

        serveStatic(
          exchange,
          path
        )
      }

    } catch {

      case exception: Exception =>

        sendJson(
          exchange,
          500,
          Document(
            "success" -> false,
            "error" -> Option(exception.getMessage)
              .getOrElse("Internal server error")
          ).toJson()
        )
    }
  }

  // ==========================================
  // API ROUTER
  // ==========================================

  private def handleApi(
      exchange: HttpExchange,
      method: String,
      path: String
  ): Unit = {

    path match {

      // -------------------------------
      // RESTAURANTS
      // -------------------------------

      case "/api/restaurants" if method == "GET" =>

        val params =
          queryParameters(exchange)

        val results =
          repository.search(
            params.get("name"),
            params.get("cuisine"),
            params.get("borough"),
            params.get("zipcode")
          )

        sendJsonArray(
          exchange,
          200,
          results
        )

      case "/api/restaurants" if method == "POST" =>

        val body =
            readBody(exchange)

        val document =
            BsonDocument.parse(body)

        val success =
            repository.addRestaurant(
            document.getString("name"),
            document.getString("borough"),
            document.getString("cuisine"),
            document.getString("restaurant_id"),
            document.getString("zipcode")
            )

        if (success) {

            sendJson(
            exchange,
            201,
            """{"success":true,"message":"Restaurant created successfully"}"""
            )

        } else {

            sendJson(
            exchange,
            400,
            """{"success":false,"message":"Could not create restaurant"}"""
            )
        }

      // -------------------------------
      // UPDATE
      // -------------------------------

    case path if
    path.startsWith("/api/restaurants/") &&
    method == "PUT" =>

        val id =
            decodePathId(path)

        val body =
            readBody(exchange)

        val document =
            BsonDocument.parse(body)

        val newCuisine =
            document.getString("cuisine")

        val success =
            repository.updateCuisine(
            id,
            newCuisine
            )

        if (success) {

            sendJson(
            exchange,
            200,
            """{"success":true,"message":"Restaurant updated successfully"}"""
            )

        } else {

            sendJson(
            exchange,
            404,
            """{"success":false,"message":"Restaurant not found"}"""
            )
        }

      // -------------------------------
      // DELETE
      // -------------------------------

      case path if
          path.startsWith("/api/restaurants/") &&
          method == "DELETE" =>

        val id =
          decodePathId(path)

        val success =
          repository.deleteRestaurant(id)

        if (success) {

          sendJson(
            exchange,
            200,
            """{"success":true,"message":"Restaurant deleted successfully"}"""
          )

        } else {

          sendJson(
            exchange,
            404,
            """{"success":false,"message":"Restaurant not found"}"""
          )
        }

      // -------------------------------
      // ANALYTICS
      // -------------------------------

      case "/api/analytics/cuisine" =>

        sendJsonArray(
          exchange,
          200,
          analytics.restaurantsByCuisine()
        )

      case "/api/analytics/borough" =>

        sendJsonArray(
          exchange,
          200,
          analytics.restaurantsByBorough()
        )

      case "/api/analytics/grades" =>

        sendJsonArray(
          exchange,
          200,
          analytics.gradeDistribution()
        )

      case "/api/analytics/top-rated" =>

        sendJsonArray(
          exchange,
          200,
          analytics.topRatedRestaurants()
        )

      case "/api/analytics/average-score" =>

        sendJsonArray(
          exchange,
          200,
          analytics.averageScoreByCuisine()
        )

      // -------------------------------
      // INDEXES
      // -------------------------------

      case "/api/indexes" =>

        sendJsonArray(
          exchange,
          200,
          repository.getIndexes()
        )

      // -------------------------------
      // DATABASE STATS
      // -------------------------------

      case "/api/stats" =>

        val count =
          repository.countRestaurants()

        sendJson(
          exchange,
          200,
          Document(
            "count" -> count
          ).toJson()
        )

      // -------------------------------
      // HEALTH CHECK
      // -------------------------------

      case "/api/health" =>

        sendJson(
          exchange,
          200,
          """{"status":"online","database":"MongoDB Atlas","dataset":"sample_restaurants"}"""
        )

      case _ =>

        sendJson(
          exchange,
          404,
          """{"success":false,"message":"API endpoint not found"}"""
        )
    }
  }

  // ==========================================
  // STATIC FILE SERVER
  // ==========================================

  private def serveStatic(
      exchange: HttpExchange,
      path: String
  ): Unit = {

    val requestedPath =
      if (path == "/" || path.isEmpty)
        "/index.html"
      else
        path

    val resourcePath =
      "/static" + requestedPath

    val stream =
      Option(
        getClass.getResourceAsStream(resourcePath)
      )

    stream match {

      case Some(input) =>

        val bytes =
          readBytes(input)

        val contentType =
          getContentType(requestedPath)

        exchange
          .getResponseHeaders
          .set(
            "Content-Type",
            contentType
          )

        exchange.sendResponseHeaders(
          200,
          bytes.length
        )

        val output =
          exchange.getResponseBody

        output.write(bytes)
        output.close()

      case None =>

        sendJson(
          exchange,
          404,
          """{"error":"Page not found"}"""
        )
    }
  }

  // ==========================================
  // QUERY PARAMETERS
  // ==========================================

  private def queryParameters(
      exchange: HttpExchange
  ): Map[String, String] = {

    val query =
      Option(
        exchange.getRequestURI.getRawQuery
      ).getOrElse("")

    if (query.isEmpty) {

      Map.empty

    } else {

      query
        .split("&")
        .flatMap { parameter =>

          parameter.split("=", 2) match {

            case Array(key, value) =>

              Some(
                URLDecoder.decode(
                  key,
                  "UTF-8"
                ) ->
                  URLDecoder.decode(
                    value,
                    "UTF-8"
                  )
              )

            case _ =>
              None
          }
        }
        .toMap
    }
  }

  // ==========================================
  // REQUEST BODY
  // ==========================================

  private def readBody(
      exchange: HttpExchange
  ): String = {

    val input =
      exchange.getRequestBody

    val bytes =
      readBytes(input)

    new String(
      bytes,
      StandardCharsets.UTF_8
    )
  }

  private def readBytes(
      input: InputStream
  ): Array[Byte] = {

    try {

      val buffer =
        new java.io.ByteArrayOutputStream()

      val data =
        new Array[Byte](4096)

      var count =
        input.read(data)

      while (count != -1) {

        buffer.write(
          data,
          0,
          count
        )

        count =
          input.read(data)
      }

      buffer.toByteArray

    } finally {

      input.close()
    }
  }

  // ==========================================
  // JSON RESPONSE
  // ==========================================

  private def sendJson(
      exchange: HttpExchange,
      status: Int,
      body: String
  ): Unit = {

    val bytes =
      body.getBytes(
        StandardCharsets.UTF_8
      )

    exchange
      .getResponseHeaders
      .set(
        "Content-Type",
        "application/json; charset=UTF-8"
      )

    exchange.sendResponseHeaders(
      status,
      bytes.length
    )

    val output =
      exchange.getResponseBody

    output.write(bytes)
    output.close()
  }

  private def sendJsonArray(
      exchange: HttpExchange,
      status: Int,
      documents: Seq[Document]
  ): Unit = {

    val json =
      documents
        .map(_.toJson())
        .mkString(
          "[",
          ",",
          "]"
        )

    sendJson(
      exchange,
      status,
      json
    )
  }

  // ==========================================
  // CORS
  // ==========================================

  private def addCorsHeaders(
      exchange: HttpExchange
  ): Unit = {

    exchange
      .getResponseHeaders
      .set(
        "Access-Control-Allow-Origin",
        "*"
      )

    exchange
      .getResponseHeaders
      .set(
        "Access-Control-Allow-Methods",
        "GET, POST, PUT, DELETE, OPTIONS"
      )

    exchange
      .getResponseHeaders
      .set(
        "Access-Control-Allow-Headers",
        "Content-Type"
      )
  }

  // ==========================================
  // HELPERS
  // ==========================================

  private def decodePathId(
      path: String
  ): String = {

    URLDecoder.decode(
      path
        .stripPrefix("/api/restaurants/")
        .trim,
      "UTF-8"
    )
  }

  private def getContentType(
      path: String
  ): String = {

    if (path.endsWith(".html"))
      "text/html; charset=UTF-8"
    else if (path.endsWith(".css"))
      "text/css; charset=UTF-8"
    else if (path.endsWith(".js"))
      "application/javascript; charset=UTF-8"
    else if (path.endsWith(".svg"))
      "image/svg+xml"
    else
      "text/plain; charset=UTF-8"
  }
}