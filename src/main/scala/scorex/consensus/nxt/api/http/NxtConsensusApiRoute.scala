package scorex.consensus.nxt.api.http

import javax.ws.rs.Path

import akka.http.scaladsl.server.Route
import com.wavesplatform.settings.RestAPISettings
import com.wavesplatform.state2.reader.StateReader
import io.swagger.annotations._
import play.api.libs.json.Json
import scorex.account.Account
import scorex.api.http.{ApiRoute, CommonApiFunctions, InvalidAddress}
import scorex.consensus.nxt.WavesConsensusModule
import scorex.crypto.encode.Base58
import scorex.transaction.{History, TransactionModule}

@Path("/consensus")
@Api(value = "/consensus")
case class NxtConsensusApiRoute(
    settings: RestAPISettings,
    consensusModule: WavesConsensusModule,
    state: StateReader,
    history: History,
    transactionModule: TransactionModule) extends ApiRoute with CommonApiFunctions {

  override val route: Route =
    pathPrefix("consensus") {
      algo ~ basetarget ~ baseTargetId ~ generationSignature ~ generationSignatureId ~ generatingBalance
    }

  @Path("/generatingbalance/{address}")
  @ApiOperation(value = "Generating balance", notes = "Account's generating balance(the same as balance atm)", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "address", value = "Address", required = true, dataType = "string", paramType = "path")
  ))
  def generatingBalance: Route = (path("generatingbalance" / Segment) & get) { address =>
    Account.fromString(address) match {
      case Left(_) => complete(InvalidAddress)
      case Right(account) =>
        complete(Json.obj(
          "address" -> account.address,
          "balance" -> consensusModule.generatingBalance(account, state.height)(transactionModule)))
    }
  }

  @Path("/generationsignature/{blockId}")
  @ApiOperation(value = "Generation signature", notes = "Generation signature of a block with specified id", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "blockId", value = "Block id ", required = true, dataType = "string", paramType = "path")
  ))
  def generationSignatureId: Route = (path("generationsignature" / Segment) & get) { encodedSignature =>
    withBlock(history, encodedSignature) { block =>
      complete(Json.obj("generationSignature" -> Base58.encode(block.consensusDataField.value.generationSignature)))
    }
  }

  @Path("/generationsignature")
  @ApiOperation(value = "Generation signature last", notes = "Generation signature of a last block", httpMethod = "GET")
  def generationSignature: Route = (path("generationsignature") & get) {
    complete(Json.obj("generationSignature" -> Base58.encode(history.lastBlock.consensusDataField.value.generationSignature)))
  }

  @Path("/basetarget/{blockId}")
  @ApiOperation(value = "Base target", notes = "base target of a block with specified id", httpMethod = "GET")
  @ApiImplicitParams(Array(
    new ApiImplicitParam(name = "blockId", value = "Block id ", required = true, dataType = "string", paramType = "path")
  ))
  def baseTargetId: Route = (path("basetarget" / Segment) & get) { encodedSignature =>
    withBlock(history, encodedSignature) { block =>
      complete(Json.obj("baseTarget" -> block.consensusDataField.value.baseTarget))
    }
  }

  @Path("/basetarget")
  @ApiOperation(value = "Base target last", notes = "Base target of a last block", httpMethod = "GET")
  def basetarget: Route = (path("basetarget") & get) {
    complete(Json.obj("baseTarget" -> history.lastBlock.consensusDataField.value.baseTarget))
  }

  @Path("/algo")
  @ApiOperation(value = "Consensus algo", notes = "Shows which consensus algo being using", httpMethod = "GET")
  def algo: Route = (path("algo") & get) {
    complete(Json.obj("consensusAlgo" -> "proof-of-stake (PoS)"))
  }
}
