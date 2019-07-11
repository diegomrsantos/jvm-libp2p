/*
 * Copyright 2019 BLK Technologies Limited (web3labs.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.libp2p.core.wip

import io.libp2p.core.mplex.MplexFlags
import io.libp2p.core.mplex.MplexFrame
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter

class MplexDataHandler : ChannelInboundHandlerAdapter() {

    var initiator = false

    val chatStreamId = 100L
    var flagChatOpened = false

    override fun channelActive(ctx: ChannelHandlerContext) {
        // TODO: now that mplex is active, we should not accept any data off the raw connection.
        // Once mplex is set up, the connection should no longer be writable or readable directly.
        // You can only read or write from/to streams
    }

    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        msg as MplexFrame

        if (msg.flag == MplexFlags.NewStream) {
            initiator = false
            val streamName = String(msg.data)
            println("Have new stream['$streamName']: $msg")

//            val frame = MplexFrame.createMessage(initiator, msg.streamId, "/multistream/1.0.0\n", "/chat/1.0.0\n")
//            ctx!!.writeAndFlush(frame)

            // Send /multistream/1.0.0
            // /secio/1.0.0
            // /yamux/1.0.0 then it goes to the respective mstream handler!
//

//            continue here, set up test to parse this!
//            Then we need to send our equivalent of this outside a message!
//            val frame = MplexFrame(newStreamTag + 1, msg.streamId, "sam".toByteArray())
//            ctx!!.writeAndFlush(frame)
        } else if (msg.flag == MplexFlags.MessageInitiator || msg.flag == MplexFlags.MessageReceiver) {
            // Expect:/multistream/1.0.0\r/ipfs/id/1.0.0
            if (msg.dataString.contains("/ipfs/id")) {
//                val parser = IdentifyOuterClass.Identify.parser()
//                val x = parser.parseFrom(msg.data)
                var frame =
//                    MplexFrame.createMessage(initiator, msg.streamId, "na\n")
                    MplexFrame.createMessage(
                        initiator,
                        msg.streamId,
                        "/multistream/1.0.0",
                        "/ipfs/id/1.0.0",
                        "na"
                    )
                ctx!!.writeAndFlush(frame)

            } else if (msg.dataString.contains("/chat/1.0.0")) {
                println("GOt chat!!!")
                ctx!!.writeAndFlush(
                    MplexFrame.createMessage(
                        initiator,
                        chatStreamId,
                        "Hello there!"
                    )
                )
            } else if (msg.dataString.contains("/multistream/1.0.0")) {
                ctx!!.writeAndFlush(
                    MplexFrame.createMessage(
                        initiator,
                        chatStreamId,
                        "/multistream/1.0.0",
                        "/chat/1.0.0"
                    )
                )
            } else {
                println("UNSUPPORTED")
            }
            // Part 2.
//            frame = MplexFrame.createMessage(initiator, msg.streamId, "/multistream/1.0.0", "/chat/1.0.0")
//            ctx!!.writeAndFlush(frame)
        } else if (msg.flag == MplexFlags.MessageReceiver) {
            if (msg.streamId == chatStreamId) {
                flagChatOpened = true
                ctx!!.writeAndFlush(
                    MplexFrame.createMessage(
                        initiator,
                        chatStreamId,
                        "/multistream/1.0.0",
                        "/chat/1.0.0"
                    )
                )
            } else {
                throw RuntimeException("UNEXPECTED")
            }
        } else if (msg.flag == MplexFlags.ResetReceiver || msg.flag == MplexFlags.ResetInitiator) {
            println("Stream has been reset, now try to send across a chat.")
            initiator = true

            var frame = MplexFrame.createNewStream(chatStreamId)
            ctx!!.writeAndFlush(frame)
//            ctx!!.disconnect().sync()
        } else {

            println("*** WARN: Unsupported stream: $msg")
        }

        super.channelRead(ctx, msg)
    }
}