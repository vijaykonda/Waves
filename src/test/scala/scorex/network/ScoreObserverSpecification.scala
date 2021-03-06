package scorex.network

import akka.actor.{ActorRef, Props}
import akka.pattern.ask
import akka.testkit.TestProbe
import com.typesafe.config.ConfigFactory
import com.wavesplatform.settings.WavesSettings
import scorex.ActorTestingCommons
import scorex.network.ScoreObserver.{CurrentScore, GetScore, UpdateScore}
import scorex.transaction.History.BlockchainScore

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.{implicitConversions, postfixOps}

class ScoreObserverSpecification extends ActorTestingCommons {

  val testCoordinator = TestProbe("Coordinator")

  val localConfig = ConfigFactory.parseString(
    """
      |waves {
      |  synchronization {
      |    score-ttl: 1s
      |  }
      |}
    """.stripMargin).withFallback(baseTestConfig).resolve()

  val wavesSettings = WavesSettings.fromConfig(localConfig)

  protected override val actorRef = system.actorOf(Props(new ScoreObserver(networkControllerMock, testCoordinator.ref, wavesSettings.synchronizationSettings)))

  testSafely {
    "no-score case" in {
      scores shouldBe empty
    }

    "consider incoming updates" - {

      def peerMock(port: Int): ConnectedPeer = stub[ConnectedPeer]

      def expectScores(values: (ConnectedPeer, BlockchainScore)*): CurrentScore =
        testCoordinator.expectMsg(CurrentScore(values.toSeq))

      def updateScore(value: (ConnectedPeer, BlockchainScore)): Unit = actorRef ! UpdateScore(value._1, value._2)

      val peer = stub[ConnectedPeer]

      updateScore(peer -> BigInt(100))
      expectScores(peer -> BigInt(100))

      val peer3 = stub[ConnectedPeer]
      updateScore(peer3 -> BigInt(300))
      expectScores(peer -> BigInt(100), peer3 -> BigInt(300))

      "no update in case of lesser score" in {
        val lesserScore = BigInt(200)

        updateScore(peer -> lesserScore)
        updateScore(stub[ConnectedPeer] -> lesserScore)
        updateScore(stub[ConnectedPeer] -> lesserScore)

        expectNoMsg(testDuration)
      }

      "clean old scores on timeout" in {
        def sleepHalfTTL(): Unit = Thread sleep wavesSettings.synchronizationSettings.scoreTTL.toMillis / 2 + 100

        sleepHalfTTL()

        scores should be(Seq(peer -> BigInt(100), peer3 -> BigInt(300)))

        val tinyScore = BigInt(10)

        val peer5 = stub[ConnectedPeer]

        updateScore(peer5 -> tinyScore)
        sleepHalfTTL()

        scores shouldEqual Seq(peer5 -> tinyScore)

        sleepHalfTTL()
        scores shouldBe empty

        val secondRoundScore = BigInt(1)

        updateScore(peer -> secondRoundScore)
        expectScores(peer -> secondRoundScore)
      }
    }
  }

  private def scores = Await.result((actorRef ? GetScore).mapTo[CurrentScore], testDuration).scores
}
