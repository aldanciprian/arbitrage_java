package arbitrage;


import org.knowm.xchange.Exchange;

import  org.knowm.xchange.poloniex.PoloniexExchange;
import org.knowm.xchange.poloniex.service.PoloniexAccountService;
import org.knowm.xchange.poloniex.service.PoloniexMarketDataService;
import org.knowm.xchange.poloniex.service.PoloniexTradeService;


public class PoloniexExchangeV2 extends PoloniexExchange implements Exchange {

	  @Override
	  protected void initServices() {
	    this.marketDataService = new PoloniexMarketDataServiceV2(this);
	    this.accountService = new PoloniexAccountService(this);
	    this.tradeService =
	        new PoloniexTradeService(this, (PoloniexMarketDataService) marketDataService);
	  }
}
