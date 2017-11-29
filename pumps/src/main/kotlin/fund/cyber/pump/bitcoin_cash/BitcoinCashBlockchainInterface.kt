package fund.cyber.pump.bitcoin_cash

import fund.cyber.node.common.Chain.BITCOIN_CASH
import fund.cyber.node.model.JsonRpcBitcoinBlock
import fund.cyber.pump.BlockchainInterface
import fund.cyber.pump.bitcoin.BitcoinBlockBundle
import fund.cyber.pump.bitcoin.BitcoinJsonRpcClient
import io.reactivex.Emitter
import io.reactivex.Flowable
import io.reactivex.functions.BiFunction
import org.slf4j.LoggerFactory
import java.util.concurrent.Callable

private val log = LoggerFactory.getLogger(BitcoinCashBlockchainInterface::class.java)!!


class BitcoinCashBlockchainInterface : BlockchainInterface<BitcoinBlockBundle> {

    override val chain = BITCOIN_CASH

    override fun subscribeBlocks(startBlockNumber: Long) =
            Flowable.generate<JsonRpcBitcoinBlock, Long>(Callable { startBlockNumber }, downloadNextBlockFunction())
                    .map(BitcoinCashPumpContext.jsonRpcToDaoBitcoinEntitiesConverter::convertToBundle)!!
}


fun downloadNextBlockFunction(client: BitcoinJsonRpcClient = BitcoinCashPumpContext.bitcoinJsonRpcClient) =
        BiFunction { blockNumber: Long, subscriber: Emitter<JsonRpcBitcoinBlock> ->
            try {
                log.debug("Pulling block $blockNumber")
                val blockHash = client.getBlockHash(blockNumber)

                if (blockHash != null) {
                    val block = client.getBlockByHash(blockHash)!!

                    //special case first coinbase transaction
                    val transactions = if (blockNumber != 0L) client.getTxes(block.tx) else emptyList()
                    subscriber.onNext(block.copy(rawtx = transactions))
                    return@BiFunction blockNumber + 1
                }
            } catch (e: Exception) {
                log.error("error during download block $blockNumber", e)
            }
            return@BiFunction blockNumber

        }