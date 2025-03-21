/*-
 * ========================LICENSE_START=================================
 * camel-processors
 * %%
 * Copyright (C) 2021 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package de.fhg.aisec.ids.camel.processors

import de.fhg.aisec.ids.camel.processors.Constants.IDSCP2_HEADER
import de.fraunhofer.iais.eis.ContractOfferMessage
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

/**
 * This Processor handles a ContractOfferMessage and creates a ContractAgreementMessage.
 */
@Suppress("unused")
class ContractOfferProcessor : Processor {

    override fun process(exchange: Exchange) {
        if (LOG.isDebugEnabled) {
            LOG.debug("[IN] ${this::class.java.simpleName}")
        }

        val contractOfferMessage = exchange.message.getHeader(
            IDSCP2_HEADER,
            ContractOfferMessage::class.java
        )

        ContractHandler.handleContractOffer(exchange, contractOfferMessage.id, LOG)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ContractOfferProcessor::class.java)
    }
}
