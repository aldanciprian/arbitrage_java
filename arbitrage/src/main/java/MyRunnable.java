package main.java;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.marketdata.MarketDataService;



public class MyRunnable implements Runnable {

	public String exchange_name = null;
	public MyRunnable(Object parameter) {
		// store parameter for later user
		exchange_name = ( String ) parameter;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		MarketDataService marketDataService = App.exchanges.get(exchange_name).getMarketDataService();

		Map<CurrencyPair,Ticker> exchange_tickers = new HashMap<CurrencyPair,Ticker>();
		for ( CurrencyPair cp : App.pairPerExchange.get(exchange_name) )
		{
			Ticker  ticker = null;
			try {
				ticker = marketDataService.getTicker(cp);
				exchange_tickers.put(cp, ticker);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}				
			if ( ticker != null)
			{
				System.out.println(exchange_name+"   "+ticker);
			}
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		App.all_tickers.put(exchange_name, exchange_tickers);
	}

}
