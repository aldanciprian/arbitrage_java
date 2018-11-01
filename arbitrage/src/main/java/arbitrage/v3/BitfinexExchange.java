package arbitrage.v3;

import org.knowm.xchange.bitfinex.v1.service.BitfinexAccountService;

public class BitfinexExchange extends org.knowm.xchange.bitfinex.v2.BitfinexExchange {

    public BitfinexExchange() {
        super();
    }

    @Override
    protected void initServices() {
        super.initServices();

        super.accountService = new BitfinexAccountService(this);
    }

}
