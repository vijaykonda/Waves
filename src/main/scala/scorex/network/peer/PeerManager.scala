package scorex.network.peer

import java.net.{InetAddress, InetSocketAddress}

import akka.actor.{Actor, ActorRef, Props}
import akka.event.LoggingReceive
import com.wavesplatform.Version
import com.wavesplatform.settings.{Constants, NetworkSettings}
import scorex.network.NetworkController.SendToNetwork
import scorex.network._
import scorex.network.message.MessageHandler.RawNetworkData
import scorex.network.message.{Message, MessageSpec}
import scorex.utils.ScorexLogging

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Random, Try}


object PeerManager {

  case class AddPeer(address: InetSocketAddress)

  case class Connected(socketAddress: InetSocketAddress, handlerRef: ActorRef,
                       ownSocketAddress: Option[InetSocketAddress], inbound: Boolean)

  case class Handshaked(address: InetSocketAddress, handshake: Handshake)

  case class Disconnected(remote: InetSocketAddress)

  case class AddToBlacklist(address: InetSocketAddress)

  case class GetRandomPeersToBroadcast(howMany: Int)

  case class ConnectedPeers(peers: Set[ConnectedPeer])

  case class PeerConnection(handlerRef: ActorRef, handshake: Option[Handshake], inbound: Boolean)

  case class BlackListUpdated(host: String)

  case class RegisterBlacklistListener(listener: ActorRef)

  case class UnregisterBlacklistListener(listener: ActorRef)

  case class ExistedBlacklist(hosts: Seq[String])

  case class Suspect(address: InetSocketAddress)

  case object CheckPeers

  case object GetAllPeers

  case object GetBlacklistedPeers

  case object GetConnectedPeers

  case object GetConnectedPeersTyped

  case object GetConnections

  private case object MarkConnectedPeersVisited

  case object BlacklistResendRequired

  case object CloseAllConnections

  case object CloseAllConnectionsComplete

}
