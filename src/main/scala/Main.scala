import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.server.Directives
import akka.stream.ActorMaterializer
import spray.json._

import scala.concurrent.ExecutionContextExecutor
import scala.io.StdIn

final case class PingResponse(value: String)

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
    implicit val pingFormat: RootJsonFormat[PingResponse] = jsonFormat1(PingResponse)
}

object SampleServer extends Directives with JsonSupport {
    def main(args: Array[String]) {

        implicit val system: ActorSystem = ActorSystem("my-system")
        implicit val materializer: ActorMaterializer = ActorMaterializer()
        // needed for the future flatMap/onComplete in the end
        implicit val executionContext: ExecutionContextExecutor = system.dispatcher

        val route =
            path("ping") {
                get {
                    complete {
                        PingResponse("Pong")
                    }
                }
            }

        val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

        println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
        StdIn.readLine() // let it run until user presses return
        bindingFuture
            .flatMap(_.unbind()) // trigger unbinding from the port
            .onComplete(_ => system.terminate()) // and shutdown when done
    }
}
